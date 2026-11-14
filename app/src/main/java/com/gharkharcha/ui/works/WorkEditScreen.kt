package com.gharkharcha.ui.works

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gharkharcha.R
import com.gharkharcha.data.entity.WorkStatus
import com.gharkharcha.data.entity.WorkType
import com.gharkharcha.ui.common.defaultNameRes
import com.gharkharcha.ui.common.labelRes
import com.gharkharcha.ui.components.AppTextField
import com.gharkharcha.ui.components.DateField
import com.gharkharcha.ui.components.DropdownField
import com.gharkharcha.ui.components.FormScaffold
import com.gharkharcha.ui.components.LoadingState
import com.gharkharcha.ui.components.MoneyField
import com.gharkharcha.ui.theme.Dimens

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkEditScreen(
    onDone: () -> Unit,
    viewModel: WorkEditViewModel = hiltViewModel(),
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(ui.saved) { if (ui.saved) onDone() }

    // Suggested names per type, resolved here because the ViewModel has no resources.
    val defaultNames = WorkType.entries.associateWith { type -> type.defaultNameRes?.let { stringResource(it) } }

    FormScaffold(
        title = stringResource(if (ui.isNew) R.string.work_new else R.string.work_edit),
        onBack = onDone,
        onSave = viewModel::save,
        isDirty = ui.isDirty,
        isSaving = ui.saving || ui.loading,
    ) {
        if (ui.loading) {
            LoadingState()
            return@FormScaffold
        }
        val form = ui.form

        DropdownField(
            selected = form.type,
            options = WorkType.entries,
            optionLabel = { stringResource(it.labelRes) },
            onSelect = { viewModel.onType(it, defaultNames[it], defaultNames[form.type]) },
            label = stringResource(R.string.work_type),
        )
        AppTextField(
            value = form.name,
            onValueChange = viewModel::onName,
            label = stringResource(R.string.work_name),
            placeholder = stringResource(R.string.work_name_hint),
            error = if (ui.errors.nameMissing) stringResource(R.string.field_required) else null,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
        )
        MoneyField(
            value = form.budget,
            onValueChange = viewModel::onBudget,
            label = stringResource(R.string.work_budget),
            error = if (ui.errors.budgetInvalid) stringResource(R.string.field_invalid_amount) else null,
            supportingText = stringResource(R.string.field_optional),
        )

        Text(
            stringResource(R.string.work_status),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = Dimens.SmallSpacing),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing)) {
            WorkStatus.entries.forEach { status ->
                val selected = form.status == status
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.onStatus(status) },
                    label = { Text(stringResource(status.labelRes)) },
                    leadingIcon = if (selected) {
                        { Icon(painterResource(R.drawable.ic_check), contentDescription = null) }
                    } else {
                        null
                    },
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing)) {
            DateField(
                date = form.startDate,
                onDateChange = viewModel::onStart,
                label = stringResource(R.string.work_start_date),
                modifier = Modifier.weight(1f),
            )
            DateField(
                date = form.endDate,
                onDateChange = viewModel::onEnd,
                label = stringResource(R.string.work_end_date),
                modifier = Modifier.weight(1f),
                error = if (ui.errors.endBeforeStart) stringResource(R.string.field_end_before_start) else null,
            )
        }
        AppTextField(
            value = form.notes,
            onValueChange = viewModel::onNotes,
            label = stringResource(R.string.notes),
            singleLine = false,
            minLines = 3,
        )
        if (ui.saveFailed) {
            Text(stringResource(R.string.save_failed), color = MaterialTheme.colorScheme.error)
        }
    }
}
