package com.gharkharcha.ui.vendors

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gharkharcha.R
import com.gharkharcha.ui.components.AppTextField
import com.gharkharcha.ui.components.BackTopBar
import com.gharkharcha.ui.components.ConfirmDialog
import com.gharkharcha.ui.components.DetailScaffold
import com.gharkharcha.ui.components.EmptyState
import com.gharkharcha.ui.components.FormScaffold
import com.gharkharcha.ui.components.LoadStateContent
import com.gharkharcha.ui.components.LoadingState
import com.gharkharcha.ui.components.MenuAction
import com.gharkharcha.ui.theme.Dimens

@Composable
fun VendorsScreen(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    viewModel: VendorsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DetailScaffold(
        topBar = { BackTopBar(title = stringResource(R.string.vendors_title), onBack = onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                text = { Text(stringResource(R.string.vendor_add)) },
            )
        },
    ) { padding ->
        LoadStateContent(state, onRetry = viewModel::retry, modifier = Modifier.padding(padding)) { list ->
            if (list.isEmpty()) {
                EmptyState(
                    icon = R.drawable.ic_house,
                    title = stringResource(R.string.vendors_empty_title),
                    message = stringResource(R.string.vendors_empty_message),
                    modifier = Modifier.padding(padding),
                )
                return@LoadStateContent
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, bottom = Dimens.FabClearance),
                verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                items(list, key = { it.id }) { v ->
                    Card(Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                        ListItem(
                            headlineContent = { Text(v.name) },
                            supportingContent = listOf(v.phone, v.gstin).filter { it.isNotBlank() }
                                .takeIf { it.isNotEmpty() }
                                ?.let { parts -> { Text(parts.joinToString(stringResource(R.string.list_separator))) } },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { onOpen(v.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VendorEditScreen(onDone: () -> Unit, viewModel: VendorEditViewModel = hiltViewModel()) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(ui.done) { if (ui.done) onDone() }

    FormScaffold(
        title = stringResource(if (ui.isNew) R.string.vendor_new else R.string.vendor_edit),
        onBack = onDone,
        onSave = viewModel::save,
        isDirty = ui.isDirty,
        isSaving = ui.saving || ui.loading,
        menuItems = if (ui.isNew) emptyList() else listOf(MenuAction(stringResource(R.string.action_delete)) { confirmDelete = true }),
    ) {
        if (ui.loading) {
            LoadingState()
            return@FormScaffold
        }
        val f = ui.form
        AppTextField(
            value = f.name,
            onValueChange = viewModel::onName,
            label = stringResource(R.string.vendor_name),
            error = when {
                ui.nameMissing -> stringResource(R.string.field_required)
                ui.nameTaken -> stringResource(R.string.vendor_name_taken)
                else -> null
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            autoFocus = ui.isNew,
        )
        AppTextField(
            value = f.phone,
            onValueChange = viewModel::onPhone,
            label = stringResource(R.string.vendor_phone),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
        )
        AppTextField(
            value = f.gstin,
            onValueChange = viewModel::onGstin,
            label = stringResource(R.string.vendor_gstin),
            supportingText = stringResource(R.string.field_optional),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Next),
        )
        AppTextField(
            value = f.address,
            onValueChange = viewModel::onAddress,
            label = stringResource(R.string.vendor_address),
            singleLine = false,
            minLines = 2,
        )
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.vendor_delete_title),
            message = stringResource(R.string.vendor_delete_message),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                confirmDelete = false
                viewModel.delete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
