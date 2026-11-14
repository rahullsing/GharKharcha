package com.gharkharcha.ui.app

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gharkharcha.data.prefs.UserPreferences
import com.gharkharcha.data.prefs.UserPreferencesRepository
import com.gharkharcha.security.AppLockManager
import com.gharkharcha.util.UndoDeleteManager
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val prefsRepo: UserPreferencesRepository,
    private val undoManager: UndoDeleteManager,
    private val appLockManager: AppLockManager,
) : ViewModel() {
    /** Null until DataStore has loaded, so the theme doesn't flash. */
    val preferences: StateFlow<UserPreferences?> =
        prefsRepo.preferences.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val isLocked: StateFlow<Boolean> = appLockManager.isLocked

    val undoRequests: Flow<UndoDeleteManager.Request> = undoManager.requestFlow

    private val _sharedUris = MutableStateFlow<List<Uri>>(emptyList())
    val sharedUris: StateFlow<List<Uri>> = _sharedUris.asStateFlow()

    private val _manualWizardOpen = MutableStateFlow(false)
    val manualWizardOpen: StateFlow<Boolean> = _manualWizardOpen.asStateFlow()

    fun openWizard() {
        _manualWizardOpen.value = true
    }

    fun dismissWizard() {
        _manualWizardOpen.value = false
        viewModelScope.launch {
            prefsRepo.setCompletedWizard(true)
        }
    }

    fun authenticate(activity: FragmentActivity) {
        appLockManager.authenticate(activity) { }
    }

    fun lockIfEnabled() {
        appLockManager.lockIfEnabled()
    }

    fun onShareReceived(uris: List<Uri>) {
        if (uris.isNotEmpty()) {
            _sharedUris.value = uris
        }
    }

    fun clearSharedUris() {
        _sharedUris.value = emptyList()
    }

    fun undo(request: UndoDeleteManager.Request) = undoManager.undo(request)

    fun commit(request: UndoDeleteManager.Request) = undoManager.commit(request)
}
