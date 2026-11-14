package com.gharkharcha.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/** Shared snackbar host so app-level messages (undo) show on whichever screen is visible. */
val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> { error("No SnackbarHostState provided") }

/**
 * Scaffold for bottom-bar screens. The navigation bar/rail already handles bottom and side
 * insets, so only the top inset is applied here (avoids a double gap above the bar).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopLevelScaffold(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = title) },
        floatingActionButton = floatingActionButton,
        snackbarHost = { SnackbarHost(LocalSnackbarHostState.current) },
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Top),
        content = content,
    )
}

/** For screens with their own bottom footer that already pads for the navigation bar. */
val FooterContentInsets: WindowInsets
    @Composable get() = ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)

/** Scaffold for pushed screens (no bottom bar): full system-bar insets. */
@Composable
fun DetailScaffold(
    topBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    floatingActionButton: @Composable () -> Unit = {},
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        floatingActionButton = floatingActionButton,
        snackbarHost = { SnackbarHost(LocalSnackbarHostState.current) },
        contentWindowInsets = contentWindowInsets,
        content = content,
    )
}
