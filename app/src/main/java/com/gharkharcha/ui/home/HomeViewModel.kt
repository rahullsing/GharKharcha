package com.gharkharcha.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gharkharcha.data.entity.ProjectEntity
import com.gharkharcha.data.repo.WorkRepository
import com.gharkharcha.domain.model.SpendTotals
import com.gharkharcha.domain.model.WorkWithSpend
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class HomeUi(
    val project: ProjectEntity?,
    val totals: SpendTotals = SpendTotals.ZERO,
    /** Works sorted by spend, highest first. */
    val works: List<WorkWithSpend> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    currentProject: CurrentProjectUseCase,
    works: WorkRepository,
    undo: UndoDeleteManager,
) : ViewModel() {
    private val retry = MutableStateFlow(0)

    val state: StateFlow<LoadState<HomeUi>> = retryableLoad(retry) {
        currentProject.state.flatMapLatest { projects ->
            val project = projects.current ?: return@flatMapLatest flowOf(HomeUi(project = null))
            combine(works.observeWithSpend(project.id), undo.pending) { list, pending ->
                val visible = list.filter { UndoDeleteManager.workKey(it.work.id) !in pending }
                val sum = visible.fold(SpendTotals.ZERO) { acc, w -> acc + w.totals.copy(budgetPaise = 0) }
                HomeUi(
                    project = project,
                    totals = sum.copy(budgetPaise = project.budgetPaise),
                    works = visible.sortedByDescending { it.totals.grandTotalPaise },
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoadState.Loading)

    fun retry() = retry.update { it + 1 }
}
