package com.gharkharcha.ui.materials

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gharkharcha.R
import com.gharkharcha.data.entity.AttachmentKind
import com.gharkharcha.data.entity.MaterialPaymentMode
import com.gharkharcha.data.entity.MaterialUnit
import com.gharkharcha.ui.common.labelRes
import com.gharkharcha.ui.components.AppTextField
import com.gharkharcha.ui.components.AutocompleteField
import com.gharkharcha.ui.components.CollectMessages
import com.gharkharcha.ui.components.DateField
import com.gharkharcha.ui.components.DropdownField
import com.gharkharcha.ui.components.FormScaffold
import com.gharkharcha.ui.components.LoadingState
import com.gharkharcha.ui.components.MoneyField
import com.gharkharcha.ui.photos.AddPhotoSheet
import com.gharkharcha.ui.photos.PhotoPreviewDialog
import com.gharkharcha.ui.photos.PhotoStrip
import com.gharkharcha.ui.photos.rememberPreviewPath
import com.gharkharcha.ui.theme.Dimens
import com.gharkharcha.util.Money

/**
 * Add/edit a material. [capturedPhoto] is a camera result handed back by navigation; it is
 * imported once and then cleared through [onCaptureConsumed].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MaterialEditScreen(
    onDone: () -> Unit,
    onOpenCamera: () -> Unit,
    capturedPhoto: String?,
    onCaptureConsumed: () -> Unit,
    viewModel: MaterialEditViewModel = hiltViewModel(),
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(ui.done) { if (ui.done) onDone() }
    LaunchedEffect(capturedPhoto) {
        capturedPhoto?.let {
            viewModel.onCaptured(it)
            onCaptureConsumed()
        }
    }
    CollectMessages(viewModel.messages)

    // Which photo strip the add sheet is for. Kept after the sheet closes because the Photo
    // Picker result arrives later.
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    var addKind by rememberSaveable { mutableStateOf(AttachmentKind.PHOTO) }
    var preview by rememberPreviewPath()

    FormScaffold(
        title = stringResource(if (ui.isNew) R.string.material_new else R.string.material_edit),
        onBack = onDone,
        onSave = viewModel::save,
        isDirty = ui.isDirty,
        isSaving = ui.saving || ui.loading || ui.importing,
    ) {
        if (ui.loading) {
            LoadingState()
            return@FormScaffold
        }
        val f = ui.form

        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf(true, false).forEachIndexed { i, itemised ->
                SegmentedButton(
                    selected = f.itemised == itemised,
                    onClick = { viewModel.onItemised(itemised) },
                    shape = SegmentedButtonDefaults.itemShape(i, 2),
                    label = { Text(stringResource(if (itemised) R.string.material_mode_itemised else R.string.material_mode_total)) },
                )
            }
        }

        AutocompleteField(
            value = f.item,
            onValueChange = viewModel::onItem,
            suggestions = ui.itemSuggestions,
            suggestionLabel = { it },
            onSuggestion = viewModel::onItem,
            label = stringResource(R.string.material_item),
            placeholder = stringResource(if (f.itemised) R.string.material_item_hint else R.string.material_item_total_hint),
            error = if (ui.errors.itemMissing) stringResource(R.string.field_required) else null,
        )

        if (f.itemised) {
            AutocompleteField(
                value = f.brand,
                onValueChange = viewModel::onBrand,
                suggestions = ui.brandSuggestions,
                suggestionLabel = { it },
                onSuggestion = viewModel::onBrand,
                label = stringResource(R.string.material_brand),
                supportingText = stringResource(R.string.field_optional),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing)) {
                AppTextField(
                    value = f.qty,
                    onValueChange = { v -> viewModel.onQty(v.filter { it.isDigit() || it == '.' }) },
                    label = stringResource(R.string.material_qty),
                    error = if (ui.errors.qtyInvalid) stringResource(R.string.field_invalid_amount) else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f),
                )
                DropdownField(
                    selected = f.unit,
                    options = MaterialUnit.entries,
                    optionLabel = { stringResource(it.labelRes) },
                    onSelect = viewModel::onUnit,
                    label = stringResource(R.string.material_unit),
                    modifier = Modifier.weight(1f),
                )
            }
            if (f.unit == MaterialUnit.CUSTOM) {
                AppTextField(
                    value = f.customUnit,
                    onValueChange = viewModel::onCustomUnit,
                    label = stringResource(R.string.material_custom_unit),
                )
            }
            MoneyField(
                value = f.rate,
                onValueChange = viewModel::onRate,
                label = stringResource(R.string.material_rate),
                error = if (ui.errors.rateInvalid) stringResource(R.string.field_invalid_amount) else null,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                MoneyField(
                    value = if (f.totalOverridden) f.total else f.autoTotalPaise?.let(Money::toInput).orEmpty(),
                    onValueChange = viewModel::onTotal,
                    label = stringResource(R.string.material_total),
                    error = if (ui.errors.totalMissing) stringResource(R.string.field_amount_required) else null,
                    supportingText = if (f.totalOverridden) null else stringResource(R.string.material_total_auto),
                    modifier = Modifier.weight(1f),
                )
                if (f.totalOverridden) {
                    TextButton(onClick = viewModel::resetTotal) { Text(stringResource(R.string.material_total_reset)) }
                }
            }
        } else {
            MoneyField(
                value = f.total,
                onValueChange = viewModel::onTotal,
                label = stringResource(R.string.material_total),
                error = if (ui.errors.totalMissing) stringResource(R.string.field_amount_required) else null,
            )
        }

        SectionTitle(stringResource(R.string.material_section_shop))
        AutocompleteField(
            value = f.shop,
            onValueChange = viewModel::onShop,
            suggestions = ui.shopSuggestions,
            suggestionLabel = { it.name },
            onSuggestion = viewModel::onShopPicked,
            label = stringResource(R.string.material_shop),
            supportingText = stringResource(R.string.field_optional),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
        )
        if (f.shop.isNotBlank()) {
            AppTextField(
                value = f.shopPhone,
                onValueChange = viewModel::onShopPhone,
                label = stringResource(R.string.material_shop_phone),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing)) {
            DateField(
                date = f.date,
                onDateChange = viewModel::onDate,
                label = stringResource(R.string.material_date),
                allowClear = false,
                modifier = Modifier.weight(1f),
            )
            AppTextField(
                value = f.billNo,
                onValueChange = viewModel::onBillNo,
                label = stringResource(R.string.material_bill_no),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Next),
                modifier = Modifier.weight(1f),
            )
        }
        Text(stringResource(R.string.material_payment_mode), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing)) {
            MaterialPaymentMode.entries.forEach { mode ->
                val selected = f.mode == mode
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.onMode(mode) },
                    label = { Text(stringResource(mode.labelRes)) },
                    leadingIcon = if (selected) {
                        { Icon(painterResource(R.drawable.ic_check), contentDescription = null) }
                    } else {
                        null
                    },
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.material_paid), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.material_paid_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = f.isPaid, onCheckedChange = viewModel::onPaid)
        }

        SectionTitle(stringResource(R.string.material_section_photos))
        if (ui.importing) LinearProgressIndicator(Modifier.fillMaxWidth())
        PhotoStrip(
            title = stringResource(R.string.material_bill_photos),
            photos = f.photos.filter { it.kind == AttachmentKind.BILL },
            onAdd = {
                addKind = AttachmentKind.BILL
                sheetOpen = true
            },
            onRemove = viewModel::removePhoto,
            onOpen = { preview = it.path },
        )
        PhotoStrip(
            title = stringResource(R.string.material_item_photos),
            photos = f.photos.filter { it.kind != AttachmentKind.BILL },
            onAdd = {
                addKind = AttachmentKind.PHOTO
                sheetOpen = true
            },
            onRemove = viewModel::removePhoto,
            onOpen = { preview = it.path },
            modifier = Modifier.padding(top = Dimens.SmallSpacing),
        )

        AppTextField(
            value = f.notes,
            onValueChange = viewModel::onNotes,
            label = stringResource(R.string.notes),
            singleLine = false,
            minLines = 2,
            modifier = Modifier.padding(top = Dimens.SmallSpacing),
        )
        if (ui.saveFailed) {
            Text(stringResource(R.string.save_failed), color = MaterialTheme.colorScheme.error)
        }
    }

    AddPhotoSheet(
        visible = sheetOpen,
        onDismiss = { sheetOpen = false },
        onCamera = {
            viewModel.startCamera(addKind)
            onOpenCamera()
        },
        onPicked = { uris -> viewModel.onPicked(uris, addKind) },
    )
    preview?.let { PhotoPreviewDialog(it, onDismiss = { preview = null }) }
}

@Composable
private fun SectionTitle(text: String) {
    Column(Modifier.padding(top = Dimens.ItemSpacing), verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing)) {
        HorizontalDivider()
        Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    }
}
