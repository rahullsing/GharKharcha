package com.gharkharcha.ui.labour

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gharkharcha.R
import com.gharkharcha.data.entity.Skill
import com.gharkharcha.ui.common.labelRes
import com.gharkharcha.ui.components.AppTextField
import com.gharkharcha.ui.components.DropdownField
import com.gharkharcha.ui.components.FormScaffold
import com.gharkharcha.ui.components.LoadingState
import com.gharkharcha.ui.components.MoneyField
import com.gharkharcha.ui.theme.Dimens
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.gharkharcha.ui.photos.AddPhotoSheet
import com.gharkharcha.ui.photos.PhotoPreviewDialog
import com.gharkharcha.ui.photos.SinglePhotoField
import com.gharkharcha.ui.photos.rememberPreviewPath

/** [onSaved] receives the labourer id (new labourers open their ledger next). */
@Composable
fun LabourerEditScreen(
    onBack: () -> Unit,
    onSaved: (id: Long, isNew: Boolean) -> Unit,
    onOpenCamera: () -> Unit,
    capturedPhoto: String?,
    onCaptureConsumed: () -> Unit,
    viewModel: LabourerEditViewModel = hiltViewModel(),
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(ui.savedId) { ui.savedId?.let { onSaved(it, ui.isNew) } }
    LaunchedEffect(capturedPhoto) {
        capturedPhoto?.let {
            viewModel.onPhotoCaptured(it)
            onCaptureConsumed()
        }
    }
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    var preview by rememberPreviewPath()

    FormScaffold(
        title = stringResource(if (ui.isNew) R.string.labourer_new else R.string.labourer_edit),
        onBack = onBack,
        onSave = viewModel::save,
        isDirty = ui.isDirty,
        isSaving = ui.saving || ui.loading || ui.importing,
    ) {
        if (ui.loading) {
            LoadingState()
            return@FormScaffold
        }
        val f = ui.form
        SinglePhotoField(
            label = stringResource(R.string.profile_photo),
            photo = f.photo,
            onAdd = { sheetOpen = true },
            onRemove = viewModel::removePhoto,
            onOpen = { preview = f.photo?.path },
            modifier = Modifier.fillMaxWidth(),
        )
        AppTextField(
            value = f.name,
            onValueChange = viewModel::onName,
            label = stringResource(R.string.labourer_name),
            error = if (ui.errors.nameMissing) stringResource(R.string.field_required) else null,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            autoFocus = ui.isNew,
        )
        AppTextField(
            value = f.phone,
            onValueChange = viewModel::onPhone,
            label = stringResource(R.string.labourer_phone),
            supportingText = stringResource(R.string.field_optional),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
        )
        DropdownField(
            selected = f.skill,
            options = Skill.entries,
            optionLabel = { stringResource(it.labelRes) },
            onSelect = viewModel::onSkill,
            label = stringResource(R.string.labourer_skill),
        )
        if (f.skill == Skill.CUSTOM) {
            AppTextField(
                value = f.customSkill,
                onValueChange = viewModel::onCustomSkill,
                label = stringResource(R.string.labourer_custom_skill),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing)) {
            MoneyField(
                value = f.dailyWage,
                onValueChange = viewModel::onDailyWage,
                label = stringResource(R.string.labourer_daily_wage),
                error = if (ui.errors.dailyInvalid) stringResource(R.string.field_invalid_amount) else null,
                modifier = Modifier.weight(1f),
            )
            MoneyField(
                value = f.hourlyRate,
                onValueChange = viewModel::onHourlyRate,
                label = stringResource(R.string.labourer_hourly_rate),
                error = if (ui.errors.hourlyInvalid) stringResource(R.string.field_invalid_amount) else null,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            stringResource(R.string.labourer_wage_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = Dimens.ScreenPadding),
        )
        AppTextField(
            value = f.address,
            onValueChange = viewModel::onAddress,
            label = stringResource(R.string.labourer_address),
            singleLine = false,
            minLines = 2,
        )
        AppTextField(
            value = f.idNotes,
            onValueChange = viewModel::onIdNotes,
            label = stringResource(R.string.labourer_id_notes),
            placeholder = stringResource(R.string.labourer_id_notes_hint),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = Dimens.SmallSpacing),
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.labourer_active), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.labourer_active_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = f.isActive, onCheckedChange = viewModel::onActive)
        }
        if (ui.saveFailed) {
            Text(stringResource(R.string.save_failed), color = MaterialTheme.colorScheme.error)
        }
    }

    AddPhotoSheet(
        visible = sheetOpen,
        onDismiss = { sheetOpen = false },
        onCamera = onOpenCamera,
        onPicked = { uris -> uris.firstOrNull()?.let(viewModel::onPhotoPicked) },
        multiple = false,
    )
    preview?.let { PhotoPreviewDialog(it, onDismiss = { preview = null }) }
}
