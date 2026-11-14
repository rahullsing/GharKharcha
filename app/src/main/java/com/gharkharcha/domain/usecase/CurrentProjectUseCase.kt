package com.gharkharcha.domain.usecase

import com.gharkharcha.data.entity.ProjectEntity
import com.gharkharcha.data.prefs.UserPreferencesRepository
import com.gharkharcha.data.repo.ProjectRepository
import com.gharkharcha.util.UndoDeleteManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

data class ProjectsState(
    val projects: List<ProjectEntity>,
    val current: ProjectEntity?,
)

/**
 * The house the user is looking at. Falls back to the newest project when the saved selection is
 * missing (deleted / first run). Projects pending an undo-delete are hidden.
 */
class CurrentProjectUseCase @Inject constructor(
    projects: ProjectRepository,
    private val prefs: UserPreferencesRepository,
    undo: UndoDeleteManager,
) {
    val state: Flow<ProjectsState> = combine(
        projects.observeAll(),
        prefs.preferences.map { it.selectedProjectId }.distinctUntilChanged(),
        undo.pending,
    ) { all, selectedId, pending ->
        val visible = all.filter { UndoDeleteManager.projectKey(it.id) !in pending }
        ProjectsState(
            projects = visible,
            current = visible.firstOrNull { it.id == selectedId } ?: visible.firstOrNull(),
        )
    }

    suspend fun select(projectId: Long) = prefs.setSelectedProject(projectId)
}
