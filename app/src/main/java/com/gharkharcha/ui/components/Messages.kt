package com.gharkharcha.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalResources
import com.gharkharcha.ui.common.UiText
import kotlinx.coroutines.flow.Flow

/** Shows one-off ViewModel messages ("3 entries copied") in the shared snackbar. */
@Composable
fun CollectMessages(messages: Flow<UiText>) {
    val host = LocalSnackbarHostState.current
    val resources = LocalResources.current
    LaunchedEffect(messages) {
        messages.collect { host.showSnackbar(it.asString(resources)) }
    }
}
