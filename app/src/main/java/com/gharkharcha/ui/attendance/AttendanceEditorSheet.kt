package com.gharkharcha.ui.attendance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.gharkharcha.R
import com.gharkharcha.data.entity.AttendanceStatus
import com.gharkharcha.data.entity.LabourerEntity
import com.gharkharcha.data.entity.RateType
import com.gharkharcha.domain.calc.AttendanceDraft
import com.gharkharcha.ui.common.labelRes
import com.gharkharcha.ui.components.AppTextField
import com.gharkharcha.ui.components.MoneyField
import com.gharkharcha.ui.theme.Dimens
import com.gharkharcha.util.DateFormats
import com.gharkharcha.util.Money
import java.time.LocalDate

private val EditorStatuses = listOf(
    AttendanceStatus.FULL_DAY,
    AttendanceStatus.HALF_DAY,
    AttendanceStatus.HOURS,
    AttendanceStatus.ABSENT,
)

/** Full details for one labourer's day: hours, rate, pay override, overtime, advance, notes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceEditorSheet(
    labourer: LabourerEntity,
    date: LocalDate,
    state: EditorState,
    hoursPerDay: Int,
    onChange: ((AttendanceDraft) -> AttendanceDraft) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val d = state.draft
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SmallSpacing),
            verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
        ) {
            Text(labourer.name, style = MaterialTheme.typography.titleLarge)
            Text(
                DateFormats.format(date),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = Dimens.SmallSpacing)) {
                EditorStatuses.forEachIndexed { i, status ->
                    SegmentedButton(
                        selected = d.status == status,
                        // Changing status recalculates pay, so drop any manual override.
                        onClick = { onChange { it.copy(status = status, payOverridden = false) } },
                        shape = SegmentedButtonDefaults.itemShape(i, EditorStatuses.size),
                        icon = {},
                        colors = statusSegmentColors(status),
                        label = { Text(stringResource(status.labelRes), maxLines = 1) },
                    )
                }
            }

            if (d.status == AttendanceStatus.HOURS) {
                AppTextField(
                    value = d.hours,
                    onValueChange = { v -> onChange { it.copy(hours = decimal(v)) } },
                    label = stringResource(R.string.attendance_hours_worked),
                    error = if (state.showErrors && d.hoursMissing) stringResource(R.string.attendance_hours_required) else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    autoFocus = d.hours.isEmpty(),
                )
            }

            if (d.status != AttendanceStatus.ABSENT) {
                Text(stringResource(R.string.attendance_rate_type), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing), verticalAlignment = Alignment.CenterVertically) {
                    SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
                        RateType.entries.forEachIndexed { i, type ->
                            SegmentedButton(
                                selected = d.rateType == type,
                                onClick = {
                                    onChange {
                                        val default = if (type == RateType.DAILY) labourer.dailyWagePaise else labourer.hourlyRatePaise
                                        it.copy(
                                            rateType = type,
                                            rate = if (default > 0) Money.toInput(default) else it.rate,
                                            payOverridden = false,
                                        )
                                    }
                                },
                                shape = SegmentedButtonDefaults.itemShape(i, RateType.entries.size),
                                icon = {},
                                label = {
                                    Text(stringResource(if (type == RateType.DAILY) R.string.rate_daily else R.string.rate_hourly))
                                },
                            )
                        }
                    }
                    MoneyField(
                        value = d.rate,
                        onValueChange = { v -> onChange { it.copy(rate = v, payOverridden = false) } },
                        label = stringResource(if (d.rateType == RateType.DAILY) R.string.labourer_daily_wage else R.string.labourer_hourly_rate),
                        modifier = Modifier.weight(1f),
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    MoneyField(
                        value = if (d.payOverridden) d.pay else Money.toInput(d.autoBasePay(hoursPerDay)),
                        onValueChange = { v -> onChange { it.copy(pay = v, payOverridden = true) } },
                        label = stringResource(R.string.attendance_pay),
                        supportingText = if (d.payOverridden) null else stringResource(R.string.attendance_pay_auto),
                        modifier = Modifier.weight(1f),
                    )
                    if (d.payOverridden) {
                        TextButton(onClick = { onChange { it.copy(payOverridden = false) } }) {
                            Text(stringResource(R.string.attendance_pay_reset))
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing)) {
                    AppTextField(
                        value = d.otHours,
                        onValueChange = { v -> onChange { it.copy(otHours = decimal(v)) } },
                        label = stringResource(R.string.attendance_ot_hours),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f),
                    )
                    MoneyField(
                        value = d.otRate,
                        onValueChange = { v -> onChange { it.copy(otRate = v) } },
                        label = stringResource(R.string.attendance_ot_rate),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            MoneyField(
                value = d.advance,
                onValueChange = { v -> onChange { it.copy(advance = v) } },
                label = stringResource(R.string.attendance_advance),
                supportingText = stringResource(R.string.field_optional),
            )
            AppTextField(
                value = d.notes,
                onValueChange = { v -> onChange { it.copy(notes = v) } },
                label = stringResource(R.string.notes),
            )

            HorizontalDivider(Modifier.padding(vertical = Dimens.SmallSpacing))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.attendance_total), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(Money.format(d.totalPay(hoursPerDay)), style = MaterialTheme.typography.titleLarge)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
                modifier = Modifier.padding(vertical = Dimens.SmallSpacing),
            ) {
                if (state.isExisting) {
                    OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.attendance_clear))
                    }
                }
                Button(onClick = onSave, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}

private fun decimal(input: String): String {
    val filtered = input.filter { it.isDigit() || it == '.' }
    return if (filtered.count { it == '.' } <= 1) filtered else filtered.substringBeforeLast('.')
}
