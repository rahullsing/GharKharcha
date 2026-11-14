package com.gharkharcha.ui.labour

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gharkharcha.R
import com.gharkharcha.data.entity.AttendanceStatus
import com.gharkharcha.domain.model.BalanceState
import com.gharkharcha.domain.model.LabourSummary
import com.gharkharcha.ui.common.LoadState
import com.gharkharcha.ui.common.labelRes
import com.gharkharcha.ui.common.skillLabel
import com.gharkharcha.ui.components.BackTopBar
import com.gharkharcha.ui.components.ConfirmDialog
import com.gharkharcha.ui.components.DetailScaffold
import com.gharkharcha.ui.components.EmptyState
import com.gharkharcha.ui.components.FooterContentInsets
import com.gharkharcha.ui.components.LoadStateContent
import com.gharkharcha.ui.components.LocalSnackbarHostState
import com.gharkharcha.ui.components.MenuAction
import com.gharkharcha.ui.components.OverflowMenu
import com.gharkharcha.ui.theme.Dimens
import com.gharkharcha.util.ContactIntents
import com.gharkharcha.util.DateFormats
import com.gharkharcha.util.Money
import com.gharkharcha.util.Quantity
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import java.time.LocalDate

private val ContentMaxWidth = 720.dp

// Column weights: Date | Work | Hours | Pay | Paid
private const val W_DATE = 1.15f
private const val W_WORK = 1.7f
private const val W_HOURS = 0.75f
private const val W_PAY = 1.1f
private const val W_PAID = 1.1f

@Composable
fun LabourerLedgerScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onAddPayment: (labourerId: Long, prefillPaise: Long) -> Unit,
    onOpenPayment: (labourerId: Long, paymentId: Long) -> Unit,
    onOpenDay: (workId: Long, date: LocalDate) -> Unit,
    onOpenPhoto: (labourerId: Long) -> Unit,
    onExportStatement: ((labourerId: Long) -> Unit)? = null,
    viewModel: LabourerLedgerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dialog by viewModel.dialog.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()
    val ui = (state as? LoadState.Ready)?.data
    LaunchedEffect(deleted) { if (deleted) onBack() }

    DetailScaffold(
        contentWindowInsets = FooterContentInsets,
        topBar = {
            BackTopBar(
                title = ui?.labourer?.name.orEmpty(),
                onBack = onBack,
                actions = {
                    if (ui != null) {
                        IconButton(onClick = { onEdit(ui.labourer.id) }) {
                            Icon(painterResource(R.drawable.ic_edit), contentDescription = stringResource(R.string.action_edit))
                        }
                        OverflowMenu(
                            listOfNotNull(
                                onExportStatement?.let { export ->
                                    MenuAction(stringResource(R.string.report_export_statement)) { export(ui.labourer.id) }
                                },
                                MenuAction(
                                    stringResource(if (ui.labourer.isActive) R.string.labourer_mark_inactive else R.string.labourer_mark_active),
                                    viewModel::toggleActive,
                                ),
                                MenuAction(stringResource(R.string.action_delete), viewModel::requestDelete),
                            ),
                        )
                    }
                },
            )
        },
    ) { padding ->
        LoadStateContent(state, onRetry = viewModel::retry, modifier = Modifier.padding(padding)) { data ->
            if (data == null) {
                EmptyState(
                    icon = R.drawable.ic_error,
                    title = stringResource(R.string.state_not_found),
                    message = null,
                    actionLabel = stringResource(R.string.action_back),
                    onAction = onBack,
                    modifier = Modifier.padding(padding),
                )
                return@LoadStateContent
            }
            Column(Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally) {
                Column(Modifier.weight(1f).widthIn(max = ContentMaxWidth)) {
                    ContactCard(
                        data,
                        onOpenPhoto = { onOpenPhoto(data.labourer.id) },
                        modifier = Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SmallSpacing),
                    )
                    if (data.lines.isEmpty()) {
                        Text(
                            stringResource(R.string.ledger_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                        )
                    } else {
                        LedgerTable(
                            lines = data.lines,
                            onOpenPayment = { onOpenPayment(data.labourer.id, it) },
                            onOpenDay = onOpenDay,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                SummaryFooter(
                    summary = data.summary,
                    onSettle = { onAddPayment(data.labourer.id, data.summary.pendingPaise.coerceAtLeast(0)) },
                )
            }
        }
    }

    when (dialog) {
        LedgerDialog.CONFIRM_DELETE -> ConfirmDialog(
            title = stringResource(R.string.labourer_delete_title),
            message = stringResource(R.string.labourer_delete_message, ui?.labourer?.name.orEmpty()),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::dismissDialog,
        )
        LedgerDialog.HAS_HISTORY -> ConfirmDialog(
            title = stringResource(R.string.labourer_has_history_title),
            message = stringResource(R.string.labourer_has_history_message, ui?.labourer?.name.orEmpty()),
            confirmLabel = stringResource(R.string.labourer_mark_inactive),
            onConfirm = viewModel::deactivateInstead,
            onDismiss = viewModel::dismissDialog,
            destructive = false,
        )
        LedgerDialog.NONE -> Unit
    }
}

@Composable
private fun ContactCard(data: LedgerUi, onOpenPhoto: () -> Unit, modifier: Modifier = Modifier) {
    val l = data.labourer
    val context = LocalContext.current
    val snackbar = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    val noApp = stringResource(R.string.no_app_found)
    fun launch(ok: Boolean) {
        if (!ok) scope.launch { snackbar.showSnackbar(noApp) }
    }
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(Dimens.CardPadding), verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
                LabourerAvatar(
                    l.name,
                    size = 56.dp,
                    photoPath = data.photoPath,
                    modifier = if (data.photoPath != null) Modifier.clickable(onClick = onOpenPhoto) else Modifier,
                )
                Column(Modifier.weight(1f)) {
                    Text(l.skillLabel(), style = MaterialTheme.typography.titleMedium)
                    rateSummary(l)?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (l.phone.isNotBlank()) {
                        Text(l.phone, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (!l.isActive) {
                        Text(
                            stringResource(R.string.labourer_inactive_tag),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            if (l.phone.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing)) {
                    OutlinedButton(onClick = { launch(ContactIntents.dial(context, l.phone)) }, modifier = Modifier.weight(1f)) {
                        Icon(painterResource(R.drawable.ic_phone), contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.labourer_call), Modifier.padding(start = Dimens.SmallSpacing))
                    }
                    OutlinedButton(onClick = { launch(ContactIntents.whatsApp(context, l.phone)) }, modifier = Modifier.weight(1f)) {
                        Icon(painterResource(R.drawable.ic_chat), contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.labourer_whatsapp), Modifier.padding(start = Dimens.SmallSpacing))
                    }
                }
            }
        }
    }
}

@Composable
private fun LedgerTable(
    lines: List<LedgerLine>,
    onOpenPayment: (Long) -> Unit,
    onOpenDay: (workId: Long, date: LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        // Header stays put while the rows scroll.
        val header = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SmallSpacing),
        ) {
            Cell(stringResource(R.string.ledger_col_date), W_DATE, header)
            Cell(stringResource(R.string.ledger_col_work), W_WORK, header)
            Cell(stringResource(R.string.ledger_col_hours), W_HOURS, header, TextAlign.End)
            Cell(stringResource(R.string.ledger_col_pay), W_PAY, header, TextAlign.End)
            Cell(stringResource(R.string.ledger_col_paid), W_PAID, header, TextAlign.End)
        }
        LazyColumn(Modifier.fillMaxWidth()) {
            itemsIndexed(lines, key = { _, line -> line.key() }) { index, line ->
                val zebra = if (index % 2 == 1) MaterialTheme.colorScheme.surfaceContainerLow else Color.Transparent
                val body = MaterialTheme.typography.bodySmall
                when (line) {
                    is LedgerLine.Day -> {
                        val a = line.item.attendance
                        val absent = a.status == AttendanceStatus.ABSENT
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(zebra)
                                .clickable { onOpenDay(a.workId, a.date) }
                                .padding(horizontal = Dimens.ScreenPadding, vertical = 10.dp),
                        ) {
                            Cell(DateFormats.formatCompact(a.date), W_DATE, body)
                            Cell(
                                if (absent) stringResource(R.string.ledger_absent, line.item.workName) else line.item.workName,
                                W_WORK,
                                body,
                            )
                            val minutes = if (absent) 0 else a.minutesWorked + a.overtimeMinutes
                            Cell(if (minutes > 0) Quantity.formatHours(minutes.toLong()) else "", W_HOURS, body, TextAlign.End)
                            Cell(if (absent) "" else Money.format(a.totalPayPaise), W_PAY, body, TextAlign.End)
                            Cell(if (a.advancePaise > 0) Money.format(a.advancePaise) else "", W_PAID, body, TextAlign.End)
                        }
                    }
                    is LedgerLine.Payment -> {
                        val p = line.payment
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(zebra)
                                .clickable { onOpenPayment(p.id) }
                                .padding(horizontal = Dimens.ScreenPadding, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Cell(DateFormats.formatCompact(p.date), W_DATE, body)
                            Column(Modifier.weight(W_WORK).padding(end = 4.dp)) {
                                val detail = listOfNotNull(
                                    stringResource(p.type.labelRes),
                                    stringResource(p.mode.labelRes),
                                    p.reference.takeIf { it.isNotBlank() },
                                ).joinToString(stringResource(R.string.list_separator))
                                Text(
                                    detail,
                                    style = body.copy(color = MaterialTheme.colorScheme.primary),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (p.periodFrom != null && p.periodTo != null) {
                                    Text(
                                        stringResource(R.string.payment_period_range, DateFormats.format(p.periodFrom), DateFormats.format(p.periodTo)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            Cell("", W_HOURS, body)
                            Cell("", W_PAY, body)
                            Row(
                                modifier = Modifier.weight(W_PAID),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (line.photoCount > 0) {
                                    Icon(
                                        painterResource(R.drawable.ic_photo),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp).padding(end = 2.dp),
                                    )
                                    Text(
                                        line.photoCount.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(end = 4.dp),
                                    )
                                }
                                Text(
                                    Money.format(p.amountPaise),
                                    style = body.copy(fontWeight = FontWeight.SemiBold),
                                    textAlign = TextAlign.End,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun LedgerLine.key(): String = when (this) {
    is LedgerLine.Day -> "d${item.attendance.id}"
    is LedgerLine.Payment -> "p${payment.id}"
}

@Composable
private fun RowScope.Cell(text: String, weight: Float, style: TextStyle, align: TextAlign = TextAlign.Start) {
    Text(
        text,
        style = style,
        textAlign = align,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(weight).padding(end = 4.dp),
    )
}

@Composable
private fun SummaryFooter(summary: LabourSummary, onSettle: () -> Unit) {
    Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .navigationBarsPadding()
                .widthIn(max = ContentMaxWidth)
                .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.ItemSpacing),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        ) {
            Row(Modifier.fillMaxWidth()) {
                Stat(stringResource(R.string.ledger_days), Quantity.formatDays(summary.dayHundredths), Modifier.weight(0.8f))
                Stat(stringResource(R.string.ledger_earned), Money.format(summary.earnedPaise), Modifier.weight(1f))
                Stat(stringResource(R.string.ledger_paid), Money.format(summary.paidPaise), Modifier.weight(1f))
                Stat(
                    if (summary.balanceState == BalanceState.ADVANCE) stringResource(R.string.balance_advance) else stringResource(R.string.ledger_pending),
                    Money.format(kotlin.math.abs(summary.pendingPaise)),
                    Modifier.weight(1.1f),
                    valueColor = balanceColor(summary.balanceState),
                )
            }
            HorizontalDivider()
            Button(onClick = onSettle, modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (summary.pendingPaise > 0) {
                        stringResource(R.string.ledger_settle_up, Money.format(summary.pendingPaise))
                    } else {
                        stringResource(R.string.ledger_add_payment)
                    },
                )
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = Color.Unspecified) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, color = valueColor, maxLines = 1)
    }
}
