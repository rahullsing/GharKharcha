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

    private companion object {
        val HOURS_PER_DAY = intPreferencesKey("hours_per_day")
        val SELECTED_PROJECT = longPreferencesKey("selected_project_id")
        val THEME = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    }
}
