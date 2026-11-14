package com.gharkharcha.ui.works

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gharkharcha.R
import com.gharkharcha.data.entity.ProjectEntity
import com.gharkharcha.data.entity.WorkEntity
import com.gharkharcha.data.repo.WorkRepository
import com.gharkharcha.domain.model.WorkWithSpend
import com.gharkharcha.domain.usecase.CurrentProjectUseCase
import com.gharkharcha.ui.common.LoadState
import com.gharkharcha.ui.common.UiText
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class WorksUi(
    val project: ProjectEntity?,
    val works: List<WorkWithSpend> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WorksViewModel @Inject constructor(
    currentProject: CurrentProjectUseCase,
    private val works: WorkRepository,
    private val undo: UndoDeleteManager,
) : ViewModel() {
    private val retry = MutableStateFlow(0)

    val state: StateFlow<LoadState<WorksUi>> = retryableLoad(retry) {
        currentProject.state.flatMapLatest { projects ->
            val project = projects.current ?: return@flatMapLatest flowOf(WorksUi(project = null))
            combine(works.observeWithSpend(project.id), undo.pending) { list, pending ->
                WorksUi(project, list.filter { UndoDeleteManager.workKey(it.work.id) !in pending })
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoadState.Loading)

    fun retry() = retry.update { it + 1 }

    fun delete(work: WorkEntity) {
        undo.schedule(UndoDeleteManager.workKey(work.id), UiText.of(R.string.work_deleted, work.name)) {
            works.delete(work)
        }
    }
}
