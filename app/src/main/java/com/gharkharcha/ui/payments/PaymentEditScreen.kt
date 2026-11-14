package com.gharkharcha.ui.payments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gharkharcha.R
import com.gharkharcha.data.entity.PaymentMode
import com.gharkharcha.data.entity.PaymentType
import com.gharkharcha.data.entity.WorkEntity
import com.gharkharcha.ui.common.labelRes
import com.gharkharcha.ui.components.AppTextField
import com.gharkharcha.ui.components.ConfirmDialog
import com.gharkharcha.ui.components.DateField
import com.gharkharcha.ui.components.DropdownField
import com.gharkharcha.ui.components.FormScaffold
import com.gharkharcha.ui.components.LoadingState
import com.gharkharcha.ui.components.MenuAction
import com.gharkharcha.ui.components.MoneyField
import com.gharkharcha.ui.theme.Dimens

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PaymentEditScreen(
    onDone: () -> Unit,
    viewModel: PaymentEditViewModel = hiltViewModel(),
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(ui.done) { if (ui.done) onDone() }

    FormScaffold(
        title = stringResource(if (ui.isNew) R.string.payment_new else R.string.payment_edit),
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
        ui.labourer?.let {
            Text(
                stringResource(R.string.payment_to, it.name),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = Dimens.SmallSpacing),
            )
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
