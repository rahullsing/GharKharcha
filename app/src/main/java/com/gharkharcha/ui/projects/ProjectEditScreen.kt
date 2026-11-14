package com.gharkharcha.ui.projects

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gharkharcha.R
import com.gharkharcha.ui.components.AppTextField
import com.gharkharcha.ui.components.DateField
import com.gharkharcha.ui.components.FormScaffold
import com.gharkharcha.ui.components.LoadingState
import com.gharkharcha.ui.components.MoneyField
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gharkharcha.ui.photos.AddPhotoSheet
import com.gharkharcha.ui.photos.PhotoPreviewDialog
import com.gharkharcha.ui.photos.SinglePhotoField
import com.gharkharcha.ui.photos.rememberPreviewPath

@Composable
fun ProjectEditScreen(
    onDone: () -> Unit,
    onOpenCamera: () -> Unit,
    capturedPhoto: String?,
    onCaptureConsumed: () -> Unit,
    viewModel: ProjectEditViewModel = hiltViewModel(),
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(ui.saved) { if (ui.saved) onDone() }
    LaunchedEffect(capturedPhoto) {
        capturedPhoto?.let {
            viewModel.onCoverCaptured(it)
            onCaptureConsumed()
        }
    }
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    var preview by rememberPreviewPath()

    FormScaffold(
        title = stringResource(if (ui.isNew) R.string.project_new else R.string.project_edit),
        onBack = onDone,
        onSave = viewModel::save,
        isDirty = ui.isDirty,
        isSaving = ui.saving || ui.loading || ui.importing,
    ) {
        if (ui.loading) {
            LoadingState()
            return@FormScaffold
        }
        val form = ui.form
        AppTextField(
            value = form.name,
            onValueChange = viewModel::onName,
            label = stringResource(R.string.project_name),
            placeholder = stringResource(R.string.project_name_hint),
            error = if (ui.nameMissing) stringResource(R.string.field_required) else null,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            autoFocus = ui.isNew,
        )
        MoneyField(
            value = form.budget,
            onValueChange = viewModel::onBudget,
            label = stringResource(R.string.project_total_budget),
            error = if (ui.budgetInvalid) stringResource(R.string.field_invalid_amount) else null,
            supportingText = stringResource(R.string.field_optional),
        )
        DateField(
            date = form.startDate,
            onDateChange = viewModel::onStart,
            label = stringResource(R.string.project_start_date),
        )
        AppTextField(
            value = form.address,
            onValueChange = viewModel::onAddress,
            label = stringResource(R.string.project_address),
            singleLine = false,
            minLines = 2,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        )
        AppTextField(
            value = form.notes,
            onValueChange = viewModel::onNotes,
            label = stringResource(R.string.notes),
            singleLine = false,
            minLines = 3,
        )
        SinglePhotoField(
            label = stringResource(R.string.cover_photo),
            photo = form.cover,
            onAdd = { sheetOpen = true },
            onRemove = viewModel::removeCover,
            onOpen = { preview = form.cover?.path },
            round = false,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        if (ui.saveFailed) {
            Text(stringResource(R.string.save_failed), color = MaterialTheme.colorScheme.error)
        }
    }

    AddPhotoSheet(
        visible = sheetOpen,
        onDismiss = { sheetOpen = false },
        onCamera = onOpenCamera,
        onPicked = { uris -> uris.firstOrNull()?.let(viewModel::onCoverPicked) },
        multiple = false,
    )
    preview?.let { PhotoPreviewDialog(it, onDismiss = { preview = null }) }
}
