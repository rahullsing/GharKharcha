package com.gharkharcha.ui.analytics

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gharkharcha.data.dao.AttendanceDao
import com.gharkharcha.data.dao.MaterialDao
import com.gharkharcha.data.entity.LabourerEntity
import com.gharkharcha.data.entity.ProjectEntity
import com.gharkharcha.data.entity.WorkEntity
import com.gharkharcha.data.repo.LabourerRepository
import com.gharkharcha.data.repo.LedgerRepository
import com.gharkharcha.data.repo.ProjectRepository
import com.gharkharcha.data.repo.WorkRepository
import com.gharkharcha.domain.model.LabourSummary
import com.gharkharcha.domain.model.SpendTotals
import com.gharkharcha.ui.common.LoadState
import com.gharkharcha.ui.common.retryableLoad
import com.gharkharcha.ui.navigation.HouseBreakdownRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.YearMonth
import javax.inject.Inject

data class WorkBreakdownItem(
    val work: WorkEntity,
    val totals: SpendTotals,
)

data class CategoryBreakdown(
    val materialPaise: Long,
    val labourPaise: Long,
    val grandTotalPaise: Long,
    val unpaidShopsPaise: Long,
    val budgetPaise: Long,
)

data class MonthBreakdownItem(
    val month: YearMonth,
    val materialPaise: Long,
    val labourPaise: Long,
    val totalPaise: Long,
)

data class LabourerBreakdownItem(
    val labourer: LabourerEntity,
    val summary: LabourSummary,
)

data class VendorBreakdownItem(
    val name: String,
    val purchaseCount: Int,
    val totalAmountPaise: Long,
    val unpaidAmountPaise: Long,
    val vendorId: Long? = null,
)

data class BreakdownUi(
    val project: ProjectEntity,
    val works: List<WorkBreakdownItem>,
    val category: CategoryBreakdown,
    val months: List<MonthBreakdownItem>,
    val labourers: List<LabourerBreakdownItem>,
    val vendors: List<VendorBreakdownItem>,
)

private data class WorksAndCategoryData(
    val project: ProjectEntity,
    val category: CategoryBreakdown,
    val workItems: List<WorkBreakdownItem>,
    val vendorGroups: List<VendorBreakdownItem>,
)

@HiltViewModel
class HouseBreakdownViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    projectRepository: ProjectRepository,
    workRepository: WorkRepository,
    materialDao: MaterialDao,
    attendanceDao: AttendanceDao,
    ledgerRepository: LedgerRepository,
    labourerRepository: LabourerRepository,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<HouseBreakdownRoute>()
    val projectId: Long = route.projectId

    private val retryTrigger = MutableStateFlow(0)
    fun retry() = retryTrigger.update { it + 1 }

    val state: StateFlow<LoadState<BreakdownUi>> = retryableLoad(retryTrigger) {
        val worksFlow = workRepository.observeWithSpend(projectId)
        val projectFlow = projectRepository.observe(projectId)
        val materialsFlow = materialDao.observeByProject(projectId)
        val matDailyFlow = materialDao.observeAllDailySpend(projectId)
        val attDailyFlow = attendanceDao.observeAllDailySpend(projectId)
        val summariesFlow = ledgerRepository.observeSummaries(projectId)
        val labourersFlow = labourerRepository.observeAll()

        val worksAndCategoryFlow: Flow<WorksAndCategoryData?> = combine(
            projectFlow,
            worksFlow,
            materialsFlow,
        ) { project, works, materials ->
            if (project == null) return@combine null

            val visible = works
            val totals = visible.map { it.totals }.fold(SpendTotals.ZERO, SpendTotals::plus)
            val unpaidShops = materials.filter { !it.isPaid }.sumOf { it.totalPaise }

            val category = CategoryBreakdown(
                materialPaise = totals.materialPaise,
                labourPaise = totals.labourPaise,
                grandTotalPaise = totals.grandTotalPaise,
                unpaidShopsPaise = unpaidShops,
                budgetPaise = totals.budgetPaise,
            )
            val workItems = visible.map { WorkBreakdownItem(it.work, it.totals) }
                .sortedByDescending { it.totals.grandTotalPaise }

            val vendorGroups = materials
                .groupBy { it.vendorId to it.shopName.ifBlank { "Shop" } }
                .map { (key, list) ->
                    val (vendorId, shopName) = key
                    VendorBreakdownItem(
                        name = shopName,
                        purchaseCount = list.size,
                        totalAmountPaise = list.sumOf { it.totalPaise },
                        unpaidAmountPaise = list.filter { !it.isPaid }.sumOf { it.totalPaise },
                        vendorId = vendorId,
                    )
                }
                .sortedByDescending { it.totalAmountPaise }

            WorksAndCategoryData(
                project = project,
                category = category,
                workItems = workItems,
                vendorGroups = vendorGroups,
            )
        }

        val monthlyFlow: Flow<List<MonthBreakdownItem>> = combine(matDailyFlow, attDailyFlow) { matDaily, attDaily ->
            val monthMap = mutableMapOf<YearMonth, Pair<Long, Long>>()
            for (row in matDaily) {
                val ym = YearMonth.from(row.date)
                val cur = monthMap.getOrDefault(ym, 0L to 0L)
                monthMap[ym] = (cur.first + row.totalPaise) to cur.second
            }
            for (row in attDaily) {
                val ym = YearMonth.from(row.date)
                val cur = monthMap.getOrDefault(ym, 0L to 0L)
                monthMap[ym] = cur.first to (cur.second + row.totalPaise)
            }
            monthMap.map { (ym, spend) ->
                MonthBreakdownItem(
                    month = ym,
                    materialPaise = spend.first,
                    labourPaise = spend.second,
                    totalPaise = spend.first + spend.second,
                )
            }.sortedByDescending { it.month }
        }

        val labourersBreakdownFlow: Flow<List<LabourerBreakdownItem>> = combine(summariesFlow, labourersFlow) { summaries, labourers ->
            val labourerMap = labourers.associateBy { it.id }
            summaries.mapNotNull { (id, summary) ->
                val labourer = labourerMap[id] ?: return@mapNotNull null
                LabourerBreakdownItem(labourer = labourer, summary = summary)
            }.sortedByDescending { it.summary.earnedPaise }
        }

        combine(worksAndCategoryFlow, monthlyFlow, labourersBreakdownFlow) { worksCat, months, labourers ->
            if (worksCat == null) {
                error("Project not found")
            }
            BreakdownUi(
                project = worksCat.project,
                works = worksCat.workItems,
                category = worksCat.category,
                months = months,
                labourers = labourers,
                vendors = worksCat.vendorGroups,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoadState.Loading)
}
