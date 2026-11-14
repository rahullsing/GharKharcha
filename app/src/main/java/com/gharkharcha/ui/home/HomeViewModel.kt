package com.gharkharcha.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gharkharcha.data.dao.AttendanceActivityRow
import com.gharkharcha.data.dao.AttendanceDao
import com.gharkharcha.data.dao.MaterialDao
import com.gharkharcha.data.dao.PaymentDao
import com.gharkharcha.data.entity.MaterialEntity
import com.gharkharcha.data.entity.PaymentEntity
import com.gharkharcha.data.entity.ProjectEntity
import com.gharkharcha.data.entity.WorkEntity
import com.gharkharcha.data.repo.LabourerRepository
import com.gharkharcha.data.repo.LedgerRepository
import com.gharkharcha.data.repo.WorkRepository
import com.gharkharcha.domain.model.SpendTotals
import com.gharkharcha.domain.model.WorkWithSpend
import com.gharkharcha.domain.usecase.CurrentProjectUseCase
import com.gharkharcha.ui.common.LoadState
import com.gharkharcha.ui.common.retryableLoad
import com.gharkharcha.util.UndoDeleteManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import javax.inject.Inject

sealed interface BudgetAlert {
    val work: WorkEntity
    val totals: SpendTotals

    data class Exceeded(override val work: WorkEntity, override val totals: SpendTotals) : BudgetAlert
    data class NearLimit(override val work: WorkEntity, override val totals: SpendTotals) : BudgetAlert
}

sealed interface RecentActivityItem {
    val id: String
    val date: LocalDate
    val amountPaise: Long

    data class Material(
        val material: MaterialEntity,
        val workName: String,
    ) : RecentActivityItem {
        override val id: String get() = "m${material.id}"
        override val date: LocalDate get() = material.purchaseDate
        override val amountPaise: Long get() = material.totalPaise
    }

    data class Attendance(
        val entry: AttendanceActivityRow,
    ) : RecentActivityItem {
        override val id: String get() = "a${entry.attendance.id}"
        override val date: LocalDate get() = entry.attendance.date
        override val amountPaise: Long get() = entry.attendance.totalPayPaise
    }

    data class Payment(
        val payment: PaymentEntity,
        val labourerName: String,
        val workName: String?,
    ) : RecentActivityItem {
        override val id: String get() = "p${payment.id}"
        override val date: LocalDate get() = payment.date
        override val amountPaise: Long get() = payment.amountPaise
    }
}

data class HomeUi(
    val project: ProjectEntity?,
    val totals: SpendTotals = SpendTotals.ZERO,
    /** Works sorted by spend, highest first. */
    val works: List<WorkWithSpend> = emptyList(),
    val todayLabourCostPaise: Long = 0L,
    val pendingLabourDuesPaise: Long = 0L,
    val donutSlices: List<DonutSlice> = emptyList(),
    val dailySpendBars: List<DailySpendBar> = emptyList(),
    val budgetAlerts: List<BudgetAlert> = emptyList(),
    val recentActivity: List<RecentActivityItem> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    currentProject: CurrentProjectUseCase,
    private val works: WorkRepository,
    private val undo: UndoDeleteManager,
    private val attendanceDao: AttendanceDao,
    private val materialDao: MaterialDao,
    private val paymentDao: PaymentDao,
    private val ledgerRepo: LedgerRepository,
    private val labourerRepo: LabourerRepository,
) : ViewModel() {
    private val retry = MutableStateFlow(0)

    val state: StateFlow<LoadState<HomeUi>> = retryableLoad(retry) {
        currentProject.state.flatMapLatest { projects ->
            val project = projects.current ?: return@flatMapLatest flowOf(HomeUi(project = null))
            val today = LocalDate.now()
            val thirtyDaysAgo = today.minusDays(29)

            combine(
                works.observeWithSpend(project.id),
                dailySpendBarsFlow(project.id, thirtyDaysAgo, today),
                recentActivityFlow(project.id),
                metricsFlow(project.id, today),
                undo.pending,
            ) { list, dailyBars, activity, metrics, pending ->
                val visible = list.filter { UndoDeleteManager.workKey(it.work.id) !in pending }
                val sum = visible.fold(SpendTotals.ZERO) { acc, w -> acc + w.totals.copy(budgetPaise = 0) }
                val totalSpent = sum.grandTotalPaise

                // Donut chart slices
                val spendWorks = visible.filter { it.totals.grandTotalPaise > 0 }
                val slices = spendWorks.mapIndexed { index, w ->
                    val color = ChartPalette[index % ChartPalette.size]
                    val pct = if (totalSpent > 0) w.totals.grandTotalPaise.toFloat() / totalSpent.toFloat() else 0f
                    DonutSlice(
                        id = w.work.id,
                        name = w.work.name,
                        amountPaise = w.totals.grandTotalPaise,
                        percentage = pct,
                        color = color,
                    )
                }

                // Budget alerts: >= 100% exceeded, >= 80% near limit
                val alerts = visible.mapNotNull { w ->
                    val b = w.work.budgetPaise
                    if (b <= 0L) return@mapNotNull null
                    val spent = w.totals.grandTotalPaise
                    when {
                        spent >= b -> BudgetAlert.Exceeded(w.work, w.totals)
                        spent >= (b * 4 / 5) -> BudgetAlert.NearLimit(w.work, w.totals)
                        else -> null
                    }
                }

                HomeUi(
                    project = project,
                    totals = sum.copy(budgetPaise = project.budgetPaise),
                    works = visible.sortedByDescending { it.totals.grandTotalPaise },
                    todayLabourCostPaise = metrics.first,
                    pendingLabourDuesPaise = metrics.second,
                    donutSlices = slices,
                    dailySpendBars = dailyBars,
                    budgetAlerts = alerts,
                    recentActivity = activity,
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoadState.Loading)

    private fun dailySpendBarsFlow(projectId: Long, since: LocalDate, today: LocalDate): Flow<List<DailySpendBar>> =
        combine(
            materialDao.observeDailySpend(projectId, since),
            attendanceDao.observeDailySpend(projectId, since),
        ) { matSpend, attSpend ->
            val matMap = matSpend.associate { it.date to it.totalPaise }
            val attMap = attSpend.associate { it.date to it.totalPaise }
            (0L..29L).map { offset ->
                val date = since.plusDays(offset)
                val total = (matMap[date] ?: 0L) + (attMap[date] ?: 0L)
                DailySpendBar(date = date, totalPaise = total, isToday = date == today)
            }
        }

    private fun recentActivityFlow(projectId: Long): Flow<List<RecentActivityItem>> =
        combine(
            materialDao.observeRecentByProject(projectId, 8),
            attendanceDao.observeRecentForProject(projectId, 8),
            paymentDao.observeRecentForProject(projectId, 8),
            labourerRepo.observeAll(),
            works.observeByProject(projectId),
        ) { materials, attendance, payments, labourers, workList ->
            val workNames = workList.associate { it.id to it.name }
            val labourerNames = labourers.associate { it.id to it.name }
            val mItems = materials.map { RecentActivityItem.Material(it, workNames[it.workId].orEmpty()) }
            val aItems = attendance.map { RecentActivityItem.Attendance(it) }
            val pItems = payments.map {
                RecentActivityItem.Payment(
                    payment = it,
                    labourerName = labourerNames[it.labourerId].orEmpty(),
                    workName = it.workId?.let(workNames::get),
                )
            }
            (mItems + aItems + pItems).sortedByDescending { it.date }.take(8)
        }

    private fun metricsFlow(projectId: Long, today: LocalDate): Flow<Pair<Long, Long>> =
        combine(
            attendanceDao.observeDayCost(projectId, today),
            ledgerRepo.observeSummaries(projectId = projectId),
        ) { todayCost, summaries ->
            val pendingDues = summaries.values.sumOf { it.pendingPaise.coerceAtLeast(0) }
            todayCost to pendingDues
        }

    fun retry() = retry.update { it + 1 }
}
