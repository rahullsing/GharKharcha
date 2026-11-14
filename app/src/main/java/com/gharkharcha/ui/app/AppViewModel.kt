package com.gharkharcha.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gharkharcha.data.prefs.UserPreferences
import com.gharkharcha.data.prefs.UserPreferencesRepository
import com.gharkharcha.util.UndoDeleteManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    prefs: UserPreferencesRepository,
    private val undoManager: UndoDeleteManager,
) : ViewModel() {
    /** Null until DataStore has loaded, so the theme doesn't flash. */
    val preferences: StateFlow<UserPreferences?> =
        prefs.preferences.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val undoRequests: Flow<UndoDeleteManager.Request> = undoManager.requestFlow

    fun undo(request: UndoDeleteManager.Request) = undoManager.undo(request)

    fun commit(request: UndoDeleteManager.Request) = undoManager.commit(request)
}
