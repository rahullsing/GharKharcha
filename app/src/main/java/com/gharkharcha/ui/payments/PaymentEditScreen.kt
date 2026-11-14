package com.gharkharcha.ui.payments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gharkharcha.R
import com.gharkharcha.data.entity.LabourerEntity
import com.gharkharcha.data.entity.PaymentMode
import com.gharkharcha.data.entity.PaymentType
import com.gharkharcha.data.entity.WorkEntity
import com.gharkharcha.ui.common.labelRes
import com.gharkharcha.ui.components.AppTextField
import com.gharkharcha.ui.components.CollectMessages
import com.gharkharcha.ui.components.ConfirmDialog
import com.gharkharcha.ui.components.DateField
import com.gharkharcha.ui.components.DropdownField
import com.gharkharcha.ui.components.FormScaffold
import com.gharkharcha.ui.components.LoadingState
import com.gharkharcha.ui.components.MenuAction
import com.gharkharcha.ui.components.MoneyField
import com.gharkharcha.ui.photos.AddPhotoSheet
import com.gharkharcha.ui.photos.PhotoPreviewDialog
import com.gharkharcha.ui.photos.PhotoStrip
import com.gharkharcha.ui.photos.rememberPreviewPath
import com.gharkharcha.ui.theme.Dimens
import com.gharkharcha.util.DateFormats
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PaymentEditScreen(
    onDone: () -> Unit,
    onOpenCamera: () -> Unit = {},
    capturedPhoto: String? = null,
    onCaptureConsumed: () -> Unit = {},
    viewModel: PaymentEditViewModel = hiltViewModel(),
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    var preview by rememberPreviewPath()

    LaunchedEffect(ui.done) { if (ui.done) onDone() }
    LaunchedEffect(capturedPhoto) {
        capturedPhoto?.let {
            viewModel.onCaptured(it)
            onCaptureConsumed()
        }
    }
    CollectMessages(viewModel.messages)

    FormScaffold(
        title = stringResource(if (ui.isNew) R.string.payment_new else R.string.payment_edit),
        onBack = onDone,
        onSave = viewModel::save,
        isDirty = ui.isDirty,
        isSaving = ui.saving || ui.loading || ui.importing,
        menuItems = if (ui.isNew) emptyList() else listOf(MenuAction(stringResource(R.string.action_delete)) { confirmDelete = true }),
    ) {
        if (ui.loading) {
            LoadingState()
            return@FormScaffold
        }
        val f = ui.form

        if (ui.isNew && ui.labourer == null && ui.activeLabourers.isNotEmpty()) {
            val selectedLabourer = ui.activeLabourers.firstOrNull { it.id == f.labourerId }
            val chooseLabel = stringResource(R.string.payment_select_labourer)
            DropdownField(
                selected = selectedLabourer,
                options = listOf<LabourerEntity?>(null) + ui.activeLabourers,
                optionLabel = { it?.name ?: chooseLabel },
                onSelect = { viewModel.onLabourer(it) },
                label = stringResource(R.string.payment_select_labourer),
            )
            if (ui.labourerError) {
                Text(
                    stringResource(R.string.field_required),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                )
            }
        } else {
            ui.labourer?.let {
                Text(
                    stringResource(R.string.payment_to, it.name),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = Dimens.SmallSpacing),
                )
            }
        }

        MoneyField(
            value = f.amount,
            onValueChange = viewModel::onAmount,
            label = stringResource(R.string.payment_amount),
            error = if (ui.amountError) stringResource(R.string.field_amount_required) else null,
        )

        Text(stringResource(R.string.payment_type), style = MaterialTheme.typography.labelLarge)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            PaymentType.entries.forEachIndexed { i, type ->
                SegmentedButton(
                    selected = f.type == type,
                    onClick = { viewModel.onType(type) },
                    shape = SegmentedButtonDefaults.itemShape(i, PaymentType.entries.size),
                    label = { Text(stringResource(type.labelRes)) },
                )
            }
        }

        Text(
            stringResource(R.string.payment_mode),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = Dimens.SmallSpacing),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing)) {
            PaymentMode.entries.forEach { mode ->
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

        DateField(
            date = f.date,
            onDateChange = viewModel::onDate,
            label = stringResource(R.string.payment_date),
            allowClear = false,
        )

        if (ui.works.isNotEmpty()) {
            val noneLabel = stringResource(R.string.payment_work_none)
            DropdownField(
                selected = ui.works.firstOrNull { it.id == f.workId },
                options = listOf<WorkEntity?>(null) + ui.works,
                optionLabel = { it?.name ?: noneLabel },
                onSelect = viewModel::onWork,
                label = stringResource(R.string.payment_work),
            )
        }

        if (f.mode != PaymentMode.CASH) {
            AppTextField(
                value = f.reference,
                onValueChange = viewModel::onReference,
                label = stringResource(R.string.payment_reference),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Next),
            )
        }

        // --- Period covered (week range picker) ---------------------------------------------------
        PeriodCoveredSection(
            from = f.periodFrom,
            to = f.periodTo,
            onPeriodChange = viewModel::onPeriod,
            onClear = viewModel::clearPeriod,
        )

        // --- Photos / Receipts -------------------------------------------------------------------
        if (ui.importing) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        PhotoStrip(
            title = stringResource(R.string.payment_receipts),
            photos = f.photos,
            onAdd = { sheetOpen = true },
            onOpen = { preview = it.path },
            onRemove = viewModel::removePhoto,
        )

        AppTextField(
            value = f.notes,
            onValueChange = viewModel::onNotes,
            label = stringResource(R.string.notes),
            singleLine = false,
            minLines = 2,
        )

        if (ui.saveFailed) {
            Text(stringResource(R.string.save_failed), color = MaterialTheme.colorScheme.error)
        }
    }

    AddPhotoSheet(
        visible = sheetOpen,
        onDismiss = { sheetOpen = false },
        onCamera = {
            sheetOpen = false
            onOpenCamera()
        },
        onPicked = { uris ->
            sheetOpen = false
            viewModel.onPicked(uris)
        },
    )

    preview?.let { path ->
        PhotoPreviewDialog(path = path, onDismiss = { preview = null })
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.payment_delete_title),
            message = stringResource(R.string.payment_delete_message),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                confirmDelete = false
                viewModel.delete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PeriodCoveredSection(
    from: LocalDate?,
    to: LocalDate?,
    onPeriodChange: (LocalDate?, LocalDate?) -> Unit,
    onClear: () -> Unit,
) {
    var customMode by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(stringResource(R.string.payment_period), style = MaterialTheme.typography.labelLarge)
                Text(
                    stringResource(R.string.payment_period_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (from != null || to != null) {
                TextButton(onClick = {
                    customMode = false
                    onClear()
                }) {
                    Text(stringResource(R.string.payment_period_clear))
                }
            }
        }

        val today = LocalDate.now()
        val thisWeekMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val thisWeekSunday = thisWeekMonday.plusDays(6)
        val lastWeekMonday = thisWeekMonday.minusWeeks(1)
        val lastWeekSunday = lastWeekMonday.plusDays(6)

        val isThisWeek = from == thisWeekMonday && to == thisWeekSunday
        val isLastWeek = from == lastWeekMonday && to == lastWeekSunday

        FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing)) {
            FilterChip(
                selected = isThisWeek,
                onClick = {
                    customMode = false
                    if (isThisWeek) onClear() else onPeriodChange(thisWeekMonday, thisWeekSunday)
                },
                label = { Text(stringResource(R.string.payment_period_this_week)) },
            )
            FilterChip(
                selected = isLastWeek,
                onClick = {
                    customMode = false
                    if (isLastWeek) onClear() else onPeriodChange(lastWeekMonday, lastWeekSunday)
                },
                label = { Text(stringResource(R.string.payment_period_last_week)) },
            )
            FilterChip(
                selected = customMode || (!isThisWeek && !isLastWeek && (from != null || to != null)),
                onClick = { customMode = true },
                label = { Text(stringResource(R.string.payment_period_custom)) },
            )
        }

        if (customMode || (!isThisWeek && !isLastWeek && (from != null || to != null))) {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing)) {
                DateField(
                    date = from,
                    onDateChange = { newFrom -> onPeriodChange(newFrom, to) },
                    label = stringResource(R.string.payment_period_from),
                    modifier = Modifier.weight(1f),
                )
                DateField(
                    date = to,
                    onDateChange = { newTo -> onPeriodChange(from, newTo) },
                    label = stringResource(R.string.payment_period_to),
                    modifier = Modifier.weight(1f),
                )
            }
        } else if (from != null && to != null) {
            Text(
                stringResource(R.string.payment_period_range, DateFormats.format(from), DateFormats.format(to)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
