package com.gharkharcha.ui.works

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gharkharcha.R
import com.gharkharcha.data.entity.WorkEntity
import com.gharkharcha.data.repo.WorkRepository
import com.gharkharcha.domain.model.WorkWithSpend
import com.gharkharcha.ui.common.LoadState
import com.gharkharcha.ui.common.UiText
import com.gharkharcha.ui.common.retryableLoad
import com.gharkharcha.ui.navigation.WorkDetailRoute
import com.gharkharcha.util.UndoDeleteManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class WorkDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val works: WorkRepository,
    private val undo: UndoDeleteManager,
) : ViewModel() {
    private val workId = savedStateHandle.toRoute<WorkDetailRoute>().workId
    private val retry = MutableStateFlow(0)

    /** Ready(null) means the work doesn't exist (deleted elsewhere). */
    val state: StateFlow<LoadState<WorkWithSpend?>> = retryableLoad(retry) { works.observeOneWithSpend(workId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoadState.Loading)

    fun retry() = retry.update { it + 1 }

    fun delete(work: WorkEntity) {
        undo.schedule(UndoDeleteManager.workKey(work.id), UiText.of(R.string.work_deleted, work.name)) {
            works.delete(work)
        }
    }
}
