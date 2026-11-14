package com.gharkharcha.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gharkharcha.data.prefs.ThemeMode
import com.gharkharcha.data.prefs.UserPreferences
import com.gharkharcha.data.prefs.UserPreferencesRepository
import com.gharkharcha.data.repo.BackupRepository
import com.gharkharcha.data.repo.RestoreResult
import com.gharkharcha.data.sample.SampleDataSeeder
import com.gharkharcha.reminders.ReminderScheduler
import com.gharkharcha.security.AppLockManager
import com.gharkharcha.util.Money
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val prefs: UserPreferences = UserPreferences(),
    val isDeviceSecure: Boolean = false,
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val isSeeding: Boolean = false,
)

sealed interface SettingsEvent {
    data class Success(val message: String) : SettingsEvent
    data class Error(val message: String) : SettingsEvent
}

private data class OperationLoading(
    val backingUp: Boolean = false,
    val restoring: Boolean = false,
    val seeding: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val prefsRepo: UserPreferencesRepository,
    private val backupRepo: BackupRepository,
    private val appLockManager: AppLockManager,
    private val seeder: SampleDataSeeder,
) : ViewModel() {

    private val _loadingState = MutableStateFlow(OperationLoading())
    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val state: StateFlow<SettingsUiState> = combine(
        prefsRepo.preferences,
        _loadingState,
    ) { prefs, loading ->
        SettingsUiState(
            prefs = prefs,
            isDeviceSecure = appLockManager.isDeviceSecure(),
            isBackingUp = loading.backingUp,
            isRestoring = loading.restoring,
            isSeeding = loading.seeding,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefsRepo.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setDynamicColor(enabled) }
    }

    fun setHoursPerDay(hours: Int) {
        viewModelScope.launch { prefsRepo.setHoursPerDay(hours) }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled && !appLockManager.isDeviceSecure()) {
                _events.send(SettingsEvent.Error("Screen lock not configured on device"))
                return@launch
            }
            prefsRepo.setAppLockEnabled(enabled)
            if (!enabled) {
                appLockManager.unlock()
            }
        }
    }

    fun setAttendanceReminder(enabled: Boolean, hour: Int = 19, minute: Int = 0) {
        viewModelScope.launch {
            prefsRepo.setAttendanceReminder(enabled, hour, minute)
            if (enabled) {
                ReminderScheduler.scheduleDailyAttendance(context, hour, minute)
            } else {
                ReminderScheduler.cancelDailyAttendance(context)
            }
        }
    }

    fun setPaydayReminder(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setPaydayReminder(enabled)
            if (enabled) {
                ReminderScheduler.scheduleWeeklyPayday(context)
            } else {
                ReminderScheduler.cancelWeeklyPayday(context)
            }
        }
    }

    fun createBackup(targetUri: Uri) {
        viewModelScope.launch {
            _loadingState.update { it.copy(backingUp = true) }
            try {
                val metadata = backupRepo.createBackup(targetUri)
                val sizeKb = metadata.totalBytes / 1024
                _events.send(SettingsEvent.Success("Backup created ($sizeKb KB, ${metadata.photoCount} photos)"))
            } catch (e: Exception) {
                _events.send(SettingsEvent.Error(e.message ?: "Backup failed"))
            } finally {
                _loadingState.update { it.copy(backingUp = false) }
            }
        }
    }

    fun restoreBackup(sourceUri: Uri) {
        viewModelScope.launch {
            _loadingState.update { it.copy(restoring = true) }
            try {
                when (val result = backupRepo.restoreBackup(sourceUri)) {
                    is RestoreResult.Success -> {
                        _events.send(SettingsEvent.Success("Restore successful! ${result.photoCount} photos restored."))
                    }
                    is RestoreResult.Failure -> {
                        _events.send(SettingsEvent.Error(result.message))
                    }
                }
            } catch (e: Exception) {
                _events.send(SettingsEvent.Error(e.message ?: "Restore failed"))
            } finally {
                _loadingState.update { it.copy(restoring = false) }
            }
        }
    }

    fun loadSampleData() {
        viewModelScope.launch {
            _loadingState.update { it.copy(seeding = true) }
            try {
                seeder.seedSampleData()
                _events.send(SettingsEvent.Success("Sample data loaded successfully!"))
            } catch (e: Exception) {
                _events.send(SettingsEvent.Error(e.message ?: "Failed to seed sample data"))
            } finally {
                _loadingState.update { it.copy(seeding = false) }
            }
        }
    }
}
