package com.gharkharcha

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gharkharcha.data.prefs.ThemeMode
import com.gharkharcha.ui.app.AppViewModel
import com.gharkharcha.ui.app.GharKharchaAppUi
import com.gharkharcha.ui.theme.GharKharchaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Until DataStore loads (a few ms) the system theme is used.
            val prefs by appViewModel.preferences.collectAsStateWithLifecycle()
            val dark = when (prefs?.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                else -> isSystemInDarkTheme()
            }
            GharKharchaTheme(darkTheme = dark, dynamicColor = prefs?.dynamicColor ?: true) {
                GharKharchaAppUi(viewModel = appViewModel)
            }
        }
    }
}
