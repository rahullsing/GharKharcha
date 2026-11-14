package com.gharkharcha.ui.attendance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gharkharcha.R
import com.gharkharcha.data.entity.AttendanceEntity
import com.gharkharcha.data.entity.AttendanceStatus
import com.gharkharcha.ui.common.LoadState
import com.gharkharcha.ui.common.labelRes
import com.gharkharcha.ui.common.skillLabel
import com.gharkharcha.ui.components.BackTopBar
import com.gharkharcha.ui.components.CollectMessages
import com.gharkharcha.ui.components.DetailScaffold
import com.gharkharcha.ui.components.DropdownField
import com.gharkharcha.ui.components.EmptyState
import com.gharkharcha.ui.components.FooterContentInsets
import com.gharkharcha.ui.components.LoadStateContent
import com.gharkharcha.ui.components.MenuAction
import com.gharkharcha.ui.components.OverflowMenu
import com.gharkharcha.ui.components.PickDateDialog
import com.gharkharcha.ui.components.PickDateRangeDialog
import com.gharkharcha.ui.labour.LabourerAvatar
import com.gharkharcha.ui.theme.Dimens
import com.gharkharcha.util.DateFormats
import com.gharkharcha.util.Money
import com.gharkharcha.util.Quantity
import com.gharkharcha.data.entity.WorkEntity
import java.time.LocalDate

private val ContentMaxWidth = 600.dp

/** Order of the one-tap choices on each row. */
private val QuickStatuses = listOf(
    AttendanceStatus.FULL_DAY,
    AttendanceStatus.HALF_DAY,
    AttendanceStatus.HOURS,
    AttendanceStatus.ABSENT,
)

@Composable
fun AttendanceScreen(
    onBack: () -> Unit,
    onAddLabourer: () -> Unit,
    viewModel: AttendanceViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val editor by viewModel.editor.collectAsStateWithLifecycle()
    val ui = (state as? LoadState.Ready)?.data
    var pickDate by rememberSaveable { mutableStateOf(false) }
    var pickRange by rememberSaveable { mutableStateOf(false) }
    CollectMessages(viewModel.messages)

    val canCopy = ui?.work != null && ui.rows.isNotEmpty()
    DetailScaffold(
        contentWindowInsets = FooterContentInsets,
        topBar = {
            BackTopBar(
                title = stringResource(R.string.attendance_title),
                onBack = onBack,
                actions = {
                    if (canCopy) {
                        OverflowMenu(
                            listOf(
                                MenuAction(stringResource(R.string.attendance_copy_previous), viewModel::copyPreviousDay),
                                MenuAction(stringResource(R.string.attendance_copy_range)) { pickRange = true },
                            ),
                        )
                    }
                },
            )
        },
    ) { padding ->
        LoadStateContent(state, onRetry = viewModel::retry, modifier = Modifier.padding(padding)) { data ->
            when {
                data.work == null -> EmptyState(
                    icon = R.drawable.ic_build,
                    title = stringResource(R.string.attendance_no_works_title),
                    message = stringResource(R.string.attendance_no_works_message),
                    modifier = Modifier.padding(padding),
                )
                else -> Column(Modifier.fillMaxSize().padding(padding)) {
                    Column(
                        Modifier.weight(1f).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        SheetHeader(
                            data = data,
                            onPrev = { viewModel.shiftDay(-1) },
                            onNext = { viewModel.shiftDay(1) },
                            onPickDate = { pickDate = true },
                            onSelectWork = viewModel::selectWork,
                            modifier = Modifier.widthIn(max = ContentMaxWidth),
                        )
                        if (data.rows.isEmpty()) {
                            EmptyState(
                                icon = R.drawable.ic_groups,
                                title = stringResource(R.string.labourers_empty_title),
                                message = stringResource(R.string.attendance_no_labourers_message),
                                actionLabel = stringResource(R.string.labourer_add),
                                onAction = onAddLabourer,
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = Dimens.ScreenPadding,
                                    end = Dimens.ScreenPadding,
                                    bottom = Dimens.ScreenPadding,
                                ),
                                verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                items(data.rows, key = { it.labourer.id }) { row ->
                                    AttendanceRow(
                                        row = row,
                                        onQuick = { viewModel.quickMark(row, it) },
                                        onDetails = { viewModel.openEditor(row) },
                                        modifier = Modifier.widthIn(max = ContentMaxWidth),
                                    )
                                }
                            }
                        }
                    }
                    DayTotalBar(data.dayTotalPaise, data.presentCount)
                }
            }
        }
    }

    if (pickDate && ui != null) {
        PickDateDialog(initial = ui.date, onPick = viewModel::setDate, onDismiss = { pickDate = false })
    }
    if (pickRange) {
        PickDateRangeDialog(
            title = stringResource(R.string.attendance_copy_range_title),
            onPick = viewModel::repeatDay,
            onDismiss = { pickRange = false },
        )
    }
    val ed = editor
    val editingRow = ed?.let { e -> ui?.rows?.firstOrNull { it.labourer.id == e.labourerId } }
    if (ed != null && editingRow != null && ui != null) {
        AttendanceEditorSheet(
            labourer = editingRow.labourer,
            date = ui.date,
            state = ed,
            hoursPerDay = ui.hoursPerDay,
            onChange = viewModel::updateDraft,
            onSave = viewModel::saveEditor,
            onClear = viewModel::clearEditorEntry,
            onDismiss = viewModel::closeEditor,
        )
    }
}

@Composable
private fun SheetHeader(
    data: AttendanceUi,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onPickDate: () -> Unit,
    onSelectWork: (WorkEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SmallSpacing),
        verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrev) {
                Icon(painterResource(R.drawable.ic_chevron_left), contentDescription = stringResource(R.string.attendance_prev_day))
            }
            val label = when {
                data.isToday -> stringResource(R.string.date_with_label, stringResource(R.string.attendance_today), DateFormats.format(data.date))
                data.date == LocalDate.now().minusDays(1) ->
                    stringResource(R.string.date_with_label, stringResource(R.string.attendance_yesterday), DateFormats.format(data.date))
                else -> DateFormats.format(data.date)
            }
            Row(
                Modifier
                    .weight(1f)
                    .clickable(role = Role.Button, onClickLabel = stringResource(R.string.select_date), onClick = onPickDate)
                    .padding(vertical = Dimens.SmallSpacing),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(R.drawable.ic_calendar), contentDescription = null, modifier = Modifier.padding(end = Dimens.SmallSpacing))
                Text(label, style = MaterialTheme.typography.titleMedium)
            }
            IconButton(onClick = onNext, enabled = data.canGoForward) {
                Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = stringResource(R.string.attendance_next_day))
            }
        }
        if (data.works.size > 1) {
            DropdownField(
                selected = data.work!!,
                options = data.works,
                optionLabel = { it.name },
                onSelect = onSelectWork,
                label = stringResource(R.string.attendance_work),
            )
        } else {
            Text(
                data.work?.name.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun AttendanceRow(
    row: AttendanceRowUi,
    onQuick: (AttendanceStatus) -> Unit,
    onDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val entry = row.entry
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(Dimens.ItemSpacing), verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
                modifier = Modifier.clickable(
                    onClickLabel = stringResource(R.string.attendance_more_details, row.labourer.name),
                    onClick = onDetails,
                ),
            ) {
                LabourerAvatar(row.labourer.name, size = 40.dp, photoPath = row.photoPath)
                Column(Modifier.weight(1f)) {
                    Text(row.labourer.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        entryNote(entry) ?: row.labourer.skillLabel(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (entry != null && entry.status != AttendanceStatus.ABSENT) {
                    Text(Money.format(entry.totalPayPaise), style = MaterialTheme.typography.titleSmall)
                }
                Icon(
                    painterResource(R.drawable.ic_edit),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(4.dp),
                )
            }
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                QuickStatuses.forEachIndexed { index, status ->
                    val selected = entry != null && entry.status.chip() == status
                    SegmentedButton(
                        selected = selected,
                        onClick = { onQuick(status) },
                        shape = SegmentedButtonDefaults.itemShape(index, QuickStatuses.size),
                        icon = {},
                        colors = statusSegmentColors(status),
                        label = { Text(stringResource(status.labelRes), maxLines = 1) },
                    )
                }
            }
        }
    }
}

/** Strong selected colour so the marked status is obvious at a glance; Absent is red. */
@Composable
internal fun statusSegmentColors(status: AttendanceStatus) = if (status == AttendanceStatus.ABSENT) {
    SegmentedButtonDefaults.colors(
        activeContainerColor = MaterialTheme.colorScheme.errorContainer,
        activeContentColor = MaterialTheme.colorScheme.onErrorContainer,
    )
} else {
    SegmentedButtonDefaults.colors(
        activeContainerColor = MaterialTheme.colorScheme.primary,
        activeContentColor = MaterialTheme.colorScheme.onPrimary,
    )
}

/** Overtime is shown on the "Full" chip. */
private fun AttendanceStatus.chip() = if (this == AttendanceStatus.OVERTIME) AttendanceStatus.FULL_DAY else this

/** "8 h · +2 h overtime · Advance ₹200", or null when there's nothing extra to say. */
@Composable
private fun entryNote(entry: AttendanceEntity?): String? {
    if (entry == null) return null
    val parts = buildList {
        if (entry.status == AttendanceStatus.HOURS) {
            add(stringResource(R.string.attendance_hours_note, Quantity.formatHours(entry.minutesWorked.toLong())))
        }
        if (entry.overtimeMinutes > 0) {
            add(stringResource(R.string.attendance_ot_note, Quantity.formatHours(entry.overtimeMinutes.toLong())))
        }
        if (entry.advancePaise > 0) add(stringResource(R.string.attendance_advance_note, Money.format(entry.advancePaise)))
        if (entry.notes.isNotBlank()) add(entry.notes)
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(stringResource(R.string.list_separator))
}

@Composable
private fun DayTotalBar(totalPaise: Long, present: Int) {
    Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .navigationBarsPadding()
                .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.ItemSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.attendance_day_total),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(Money.format(totalPaise), style = MaterialTheme.typography.titleLarge)
            }
            Text(
                pluralStringResource(R.plurals.attendance_present_count, present, present),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
