package com.gharkharcha.ui.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gharkharcha.R
import com.gharkharcha.data.dao.ProjectSummaryRow
import com.gharkharcha.data.entity.ProjectEntity
import com.gharkharcha.data.repo.ProjectRepository
import com.gharkharcha.domain.usecase.CurrentProjectUseCase
import com.gharkharcha.ui.common.LoadState
import com.gharkharcha.ui.common.UiText
import com.gharkharcha.ui.common.retryableLoad
import com.gharkharcha.util.UndoDeleteManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProjectsUi(
    val projects: List<ProjectSummaryRow>,
    val currentId: Long?,
)

@HiltViewModel
class ProjectsViewModel @Inject constructor(
    private val projects: ProjectRepository,
    private val currentProject: CurrentProjectUseCase,
    private val undo: UndoDeleteManager,
) : ViewModel() {
    private val retry = MutableStateFlow(0)

    val state: StateFlow<LoadState<ProjectsUi>> = retryableLoad(retry) {
        combine(projects.observeSummaries(), currentProject.state, undo.pending) { rows, current, pending ->
            ProjectsUi(
                projects = rows.filter { UndoDeleteManager.projectKey(it.project.id) !in pending },
                currentId = current.current?.id,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoadState.Loading)

    fun retry() = retry.update { it + 1 }

    fun select(id: Long) {
        viewModelScope.launch { currentProject.select(id) }
    }

    fun delete(project: ProjectEntity) {
        undo.schedule(UndoDeleteManager.projectKey(project.id), UiText.of(R.string.project_deleted)) {
            projects.delete(project)
        }
    }
}
