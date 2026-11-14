package com.gharkharcha.ui.reports

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gharkharcha.data.entity.LabourerEntity
import com.gharkharcha.data.entity.ProjectEntity
import com.gharkharcha.data.entity.WorkEntity
import com.gharkharcha.data.repo.GeneratedReport
import com.gharkharcha.data.repo.LabourerRepository
import com.gharkharcha.data.repo.ReportRepository
import com.gharkharcha.data.repo.WorkRepository
import com.gharkharcha.data.reports.DateFilter
import com.gharkharcha.data.reports.DateRangePreset
import com.gharkharcha.data.reports.ReportFormat
import com.gharkharcha.data.reports.ReportType
import com.gharkharcha.domain.usecase.CurrentProjectUseCase
import com.gharkharcha.ui.navigation.ReportsRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class ReportsUiState(
    val reportType: ReportType = ReportType.HOUSE_SUMMARY,
    val reportFormat: ReportFormat = ReportFormat.PDF,
    val datePreset: DateRangePreset = DateRangePreset.ALL_TIME,
    val customStartDate: LocalDate? = null,
    val customEndDate: LocalDate? = null,
    val selectedWorkId: Long = 0,
    val selectedLabourerId: Long = 0,
    val includePhotos: Boolean = false,
    val currentProject: ProjectEntity? = null,
    val works: List<WorkEntity> = emptyList(),
    val labourers: List<LabourerEntity> = emptyList(),
    val isGenerating: Boolean = false,
)

sealed interface ReportsEvent {
    data class ReportGenerated(val report: GeneratedReport) : ReportsEvent
    data class Error(val message: String) : ReportsEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReportsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val reports: ReportRepository,
    currentProjectUseCase: CurrentProjectUseCase,
    worksRepo: WorkRepository,
    labourersRepo: LabourerRepository,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<ReportsRoute>()

    private val initialType = when (route.initialType) {
        "WORK" -> ReportType.WORK_REPORT
        "LABOURER" -> ReportType.LABOURER_STATEMENT
        else -> ReportType.HOUSE_SUMMARY
    }

    private val _uiState = MutableStateFlow(
        ReportsUiState(
            reportType = initialType,
            selectedWorkId = route.initialWorkId,
            selectedLabourerId = route.initialLabourerId,
        ),
    )

    private val _events = Channel<ReportsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val currentProjectFlow = currentProjectUseCase.state.map { it.current }

    private val projectWithWorksFlow = currentProjectFlow.flatMapLatest { project ->
        if (project != null) {
            worksRepo.observeByProject(project.id).map { works -> Pair(project, works) }
        } else {
            flowOf(Pair(null, emptyList()))
        }
    }

    val state: StateFlow<ReportsUiState> = combine(
        _uiState,
        projectWithWorksFlow,
        labourersRepo.observeActive(),
    ) { currentUi, (project, works), activeLabourers ->
        val defaultWorkId = if (currentUi.selectedWorkId != 0L && works.any { it.id == currentUi.selectedWorkId }) {
            currentUi.selectedWorkId
        } else {
            works.firstOrNull()?.id ?: 0L
        }
        val defaultLabourerId = if (currentUi.selectedLabourerId != 0L && activeLabourers.any { it.id == currentUi.selectedLabourerId }) {
            currentUi.selectedLabourerId
        } else {
            activeLabourers.firstOrNull()?.id ?: 0L
        }

        currentUi.copy(
            currentProject = project,
            works = works,
            labourers = activeLabourers,
            selectedWorkId = defaultWorkId,
            selectedLabourerId = defaultLabourerId,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), _uiState.value)

    fun setReportType(type: ReportType) {
        _uiState.update { it.copy(reportType = type) }
    }

    fun setReportFormat(format: ReportFormat) {
        _uiState.update { it.copy(reportFormat = format) }
    }

    fun setDatePreset(preset: DateRangePreset) {
        _uiState.update { it.copy(datePreset = preset) }
    }

    fun setCustomStartDate(date: LocalDate?) {
        _uiState.update { it.copy(customStartDate = date) }
    }

    fun setCustomEndDate(date: LocalDate?) {
        _uiState.update { it.copy(customEndDate = date) }
    }

    fun setSelectedWorkId(id: Long) {
        _uiState.update { it.copy(selectedWorkId = id) }
    }

    fun setSelectedLabourerId(id: Long) {
        _uiState.update { it.copy(selectedLabourerId = id) }
    }

    fun setIncludePhotos(include: Boolean) {
        _uiState.update { it.copy(includePhotos = include) }
    }

    fun generateReport() {
        val current = state.value
        if (current.isGenerating) return

        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true) }
            try {
                val dateFilter = DateFilter(
                    preset = current.datePreset,
                    startDate = current.customStartDate,
                    endDate = current.customEndDate,
                )
                val generated = reports.exportReport(
                    reportType = current.reportType,
                    format = current.reportFormat,
                    projectId = current.currentProject?.id ?: 1L,
                    workId = current.selectedWorkId,
                    labourerId = current.selectedLabourerId,
                    dateFilter = dateFilter,
                    includePhotos = current.includePhotos,
                )
                _events.send(ReportsEvent.ReportGenerated(generated))
            } catch (e: Exception) {
                _events.send(ReportsEvent.Error(e.message ?: "Failed to generate report"))
            } finally {
                _uiState.update { it.copy(isGenerating = false) }
            }
        }
    }
}
