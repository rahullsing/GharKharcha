package com.gharkharcha

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Intent
import com.gharkharcha.data.prefs.ThemeMode
import com.gharkharcha.ui.app.AppViewModel
import com.gharkharcha.ui.app.GharKharchaAppUi
import com.gharkharcha.ui.theme.GharKharchaTheme
import com.gharkharcha.util.ShareIntentParser
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
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

    override fun onStop() {
        super.onStop()
        appViewModel.lockIfEnabled()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val uris = ShareIntentParser.extractImageUris(intent)
        if (uris.isNotEmpty()) {
            appViewModel.onShareReceived(uris)
        }
    }
}
