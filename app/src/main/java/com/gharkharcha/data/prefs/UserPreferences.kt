package com.gharkharcha.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gharkharcha.domain.calc.PayCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class UserPreferences(
    val hoursPerDay: Int = PayCalculator.DEFAULT_HOURS_PER_DAY,
    val selectedProjectId: Long? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val currencySymbol: String = "₹",
    val appLockEnabled: Boolean = false,
    val attendanceReminderEnabled: Boolean = false,
    val attendanceReminderHour: Int = 19,
    val attendanceReminderMinute: Int = 0,
    val paydayReminderEnabled: Boolean = false,
    val autoBackupEnabled: Boolean = false,
    val lastBackupTimestamp: Long = 0L,
    val backupFolderUri: String? = null,
    val hasCompletedWizard: Boolean = false,
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val preferences: Flow<UserPreferences> = dataStore.data.map { p ->
        UserPreferences(
            hoursPerDay = p[HOURS_PER_DAY]?.takeIf { it in 1..24 } ?: PayCalculator.DEFAULT_HOURS_PER_DAY,
            selectedProjectId = p[SELECTED_PROJECT],
            themeMode = p[THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            dynamicColor = p[DYNAMIC_COLOR] ?: true,
            currencySymbol = p[CURRENCY_SYMBOL] ?: "₹",
            appLockEnabled = p[APP_LOCK] ?: false,
            attendanceReminderEnabled = p[ATTENDANCE_REMINDER] ?: false,
            attendanceReminderHour = p[ATTENDANCE_HOUR]?.takeIf { it in 0..23 } ?: 19,
            attendanceReminderMinute = p[ATTENDANCE_MINUTE]?.takeIf { it in 0..59 } ?: 0,
            paydayReminderEnabled = p[PAYDAY_REMINDER] ?: false,
            autoBackupEnabled = p[AUTO_BACKUP] ?: false,
            lastBackupTimestamp = p[LAST_BACKUP] ?: 0L,
            backupFolderUri = p[BACKUP_URI],
            hasCompletedWizard = p[COMPLETED_WIZARD] ?: false,
        )
    }.distinctUntilChanged()

    val hoursPerDay: Flow<Int> = preferences.map { it.hoursPerDay }.distinctUntilChanged()

    suspend fun setHoursPerDay(hours: Int) {
        require(hours in 1..24)
        dataStore.edit { it[HOURS_PER_DAY] = hours }
    }

    suspend fun setSelectedProject(id: Long?) {
        dataStore.edit { if (id == null) it.remove(SELECTED_PROJECT) else it[SELECTED_PROJECT] = id }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[DYNAMIC_COLOR] = enabled }
    }

    suspend fun setCurrencySymbol(symbol: String) {
        dataStore.edit { it[CURRENCY_SYMBOL] = symbol }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        dataStore.edit { it[APP_LOCK] = enabled }
    }

    suspend fun setAttendanceReminder(enabled: Boolean, hour: Int = 19, minute: Int = 0) {
        dataStore.edit {
            it[ATTENDANCE_REMINDER] = enabled
            it[ATTENDANCE_HOUR] = hour
            it[ATTENDANCE_MINUTE] = minute
        }
    }

    suspend fun setPaydayReminder(enabled: Boolean) {
        dataStore.edit { it[PAYDAY_REMINDER] = enabled }
    }

    suspend fun setAutoBackup(enabled: Boolean, uri: String? = null) {
        dataStore.edit {
            it[AUTO_BACKUP] = enabled
            if (uri != null) it[BACKUP_URI] = uri
        }
    }

    suspend fun recordBackupCompleted(timestamp: Long = System.currentTimeMillis()) {
        dataStore.edit { it[LAST_BACKUP] = timestamp }
    }

    suspend fun setCompletedWizard(completed: Boolean) {
        dataStore.edit { it[COMPLETED_WIZARD] = completed }
    }

    private companion object {
        val HOURS_PER_DAY = intPreferencesKey("hours_per_day")
        val SELECTED_PROJECT = longPreferencesKey("selected_project_id")
        val THEME = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val APP_LOCK = booleanPreferencesKey("app_lock_enabled")
        val ATTENDANCE_REMINDER = booleanPreferencesKey("attendance_reminder_enabled")
        val ATTENDANCE_HOUR = intPreferencesKey("attendance_reminder_hour")
        val ATTENDANCE_MINUTE = intPreferencesKey("attendance_reminder_minute")
        val PAYDAY_REMINDER = booleanPreferencesKey("payday_reminder_enabled")
        val AUTO_BACKUP = booleanPreferencesKey("auto_backup_enabled")
        val LAST_BACKUP = longPreferencesKey("last_backup_timestamp")
        val BACKUP_URI = stringPreferencesKey("backup_folder_uri")
        val COMPLETED_WIZARD = booleanPreferencesKey("has_completed_wizard")
    }
}
