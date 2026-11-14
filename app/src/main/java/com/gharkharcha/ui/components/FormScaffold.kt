package com.gharkharcha.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gharkharcha.R
import com.gharkharcha.ui.theme.Dimens

/**
 * Standard add/edit screen: back bar, scrollable fields, one pinned Save button above the keyboard.
 * Asks before discarding unsaved changes.
 */
@Composable
fun FormScaffold(
    title: String,
    onBack: () -> Unit,
    onSave: () -> Unit,
    isDirty: Boolean,
    isSaving: Boolean,
    modifier: Modifier = Modifier,
    menuItems: List<MenuAction> = emptyList(),
    content: @Composable ColumnScope.() -> Unit,
) {
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    val requestBack = { if (isDirty) confirmDiscard = true else onBack() }
    BackHandler(enabled = isDirty) { confirmDiscard = true }

    Scaffold(
        modifier = modifier,
        topBar = {
            BackTopBar(title = title, onBack = requestBack, actions = {
                if (menuItems.isNotEmpty()) OverflowMenu(menuItems)
            })
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = onSave,
                    enabled = !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.ItemSpacing),
                ) { Text(stringResource(R.string.action_save)) }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .padding(Dimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
                content = content,
            )
        }
    }

    if (confirmDiscard) {
        ConfirmDialog(
            title = stringResource(R.string.discard_changes_title),
            message = stringResource(R.string.discard_changes_message),
            confirmLabel = stringResource(R.string.action_discard),
            onConfirm = {
                confirmDiscard = false
                onBack()
            },
            onDismiss = { confirmDiscard = false },
        )
    }
}
