package com.gharkharcha.ui.labour

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gharkharcha.data.entity.LabourerEntity
import com.gharkharcha.data.entity.Skill
import com.gharkharcha.data.repo.LabourerRepository
import com.gharkharcha.data.entity.OwnerType
import com.gharkharcha.data.repo.AttachmentRepository
import com.gharkharcha.data.repo.LedgerRepository
import com.gharkharcha.data.repo.WorkRepository
import com.gharkharcha.domain.model.LabourSummary
import com.gharkharcha.domain.usecase.CurrentProjectUseCase
import com.gharkharcha.ui.common.LoadState
import com.gharkharcha.ui.common.retryableLoad
import com.gharkharcha.util.UndoDeleteManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** Filter chip value: everyone, one skill, or inactive labourers only. */
sealed interface LabourerFilter {
    data object All : LabourerFilter
    data object Inactive : LabourerFilter
    data class BySkill(val skill: Skill) : LabourerFilter
}

data class LabourerRow(val labourer: LabourerEntity, val summary: LabourSummary?, val photoPath: String? = null)

data class LabourersUi(
    /** False when there is no house yet (attendance needs a house). */
    val hasProject: Boolean,
    val hasWorks: Boolean,
    val totalCount: Int,
    val rows: List<LabourerRow>,
    val skills: List<Skill>,
    val hasInactive: Boolean,
    val totalDuePaise: Long,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LabourersViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    labourers: LabourerRepository,
    ledger: LedgerRepository,
    currentProject: CurrentProjectUseCase,
    works: WorkRepository,
    undo: UndoDeleteManager,
    attachments: AttachmentRepository,
) : ViewModel() {
    private val retry = MutableStateFlow(0)

    val query: StateFlow<String> = savedStateHandle.getStateFlow(KEY_QUERY, "")

    /** Stored as a string so it survives process death: "ALL", "INACTIVE" or a skill name. */
    private val filterKey = savedStateHandle.getStateFlow(KEY_FILTER, "ALL")
    val filter: StateFlow<LabourerFilter> = filterKey.map(::decode)
        .stateIn(viewModelScope, SharingStarted.Eagerly, LabourerFilter.All)

    private val hasWorks = currentProject.state.flatMapLatest { state ->
        val p = state.current ?: return@flatMapLatest flowOf(null)
        works.observeByProject(p.id).map { it.isNotEmpty() }
    }

    val state: StateFlow<LoadState<LabourersUi>> = retryableLoad(retry) {
        combine(
            combine(labourers.observeAll(), undo.pending, attachments.observeFirstPhotos(OwnerType.LABOURER)) { list, pending, photos ->
                list.filter { UndoDeleteManager.labourerKey(it.id) !in pending } to photos
            },
            ledger.observeSummaries(),
            hasWorks,
            query,
            filter,
        ) { (all, photos), summaries, worksFlag, q, f ->
            val rows = all.filter { matches(it, q, f) }.map { LabourerRow(it, summaries[it.id], photos[it.id]) }
            LabourersUi(
                hasProject = worksFlag != null,
                hasWorks = worksFlag == true,
                totalCount = all.size,
                rows = rows,
                skills = all.filter { it.isActive }.map { it.skill }.distinct().sortedBy { it.ordinal },
                hasInactive = all.any { !it.isActive },
                totalDuePaise = all.sumOf { (summaries[it.id]?.pendingPaise ?: 0).coerceAtLeast(0) },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoadState.Loading)

    fun retry() = retry.update { it + 1 }

    fun onQuery(value: String) {
        savedStateHandle[KEY_QUERY] = value
    }

    fun onFilter(value: LabourerFilter) {
        savedStateHandle[KEY_FILTER] = when (value) {
            LabourerFilter.All -> "ALL"
            LabourerFilter.Inactive -> "INACTIVE"
            is LabourerFilter.BySkill -> value.skill.name
        }
    }

    private fun decode(key: String): LabourerFilter = when (key) {
        "ALL" -> LabourerFilter.All
        "INACTIVE" -> LabourerFilter.Inactive
        else -> runCatching { LabourerFilter.BySkill(Skill.valueOf(key)) }.getOrDefault(LabourerFilter.All)
    }

    private fun matches(l: LabourerEntity, q: String, f: LabourerFilter): Boolean {
        val byFilter = when (f) {
            LabourerFilter.All -> true
            LabourerFilter.Inactive -> !l.isActive
            is LabourerFilter.BySkill -> l.isActive && l.skill == f.skill
        }
        val text = q.trim()
        val digits = text.filter(Char::isDigit)
        val byQuery = text.isEmpty() ||
            l.name.contains(text, ignoreCase = true) ||
            l.customSkill.contains(text, ignoreCase = true) ||
            (digits.isNotEmpty() && l.phone.filter(Char::isDigit).contains(digits))
        return byFilter && byQuery
    }

    private companion object {
        const val KEY_QUERY = "query"
        const val KEY_FILTER = "filter"
    }
}
