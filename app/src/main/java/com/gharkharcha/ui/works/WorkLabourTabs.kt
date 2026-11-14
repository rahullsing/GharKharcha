package com.gharkharcha.ui.works

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gharkharcha.R
import com.gharkharcha.data.entity.PaymentEntity
import com.gharkharcha.ui.common.labelRes
import com.gharkharcha.ui.common.skillLabel
import com.gharkharcha.ui.components.ConfirmDialog
import com.gharkharcha.ui.components.EmptyState
import com.gharkharcha.ui.components.MenuAction
import com.gharkharcha.ui.components.OverflowMenu
import com.gharkharcha.ui.labour.LabourerAvatar
import com.gharkharcha.ui.theme.Dimens
import com.gharkharcha.util.DateFormats
import com.gharkharcha.util.Money
import com.gharkharcha.util.Quantity

private val ContentMaxWidth = 600.dp

/** Labour tab of a work: total, mark-attendance button, and each labourer's days and earnings here. */
@Composable
fun WorkLabourTab(
    totalPaise: Long,
    rows: List<WorkLabourRow>,
    onMarkAttendance: () -> Unit,
    onOpenLabourer: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Card(Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth()) {
                Row(Modifier.padding(Dimens.CardPadding), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.labour),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(Money.format(totalPaise), style = MaterialTheme.typography.headlineSmall)
                    }
                    Button(onClick = onMarkAttendance) { Text(stringResource(R.string.attendance_mark)) }
                }
            }
        }
        if (rows.isEmpty()) {
            item {
                EmptyState(
                    icon = R.drawable.ic_groups,
                    title = stringResource(R.string.labour_empty_title),
                    message = stringResource(R.string.labour_empty_message),
                    inline = true,
                )
            }
        }
        items(rows, key = { it.labourer.id }) { row ->
            Card(Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text(row.labourer.name) },
                    supportingContent = {
                        Text(
                            listOf(
                                row.labourer.skillLabel(),
                                stringResource(
                                    R.string.ledger_days_hours,
                                    Quantity.formatDays(row.summary.dayHundredths),
                                    Quantity.formatHours(row.summary.totalMinutes),
                                ),
                            ).joinToString(stringResource(R.string.list_separator)),
                        )
                    },
                    leadingContent = { LabourerAvatar(row.labourer.name, size = 40.dp) },
                    trailingContent = {
                        Text(Money.format(row.summary.earnedPaise), style = MaterialTheme.typography.titleSmall)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { onOpenLabourer(row.labourer.id) },
                )
            }
        }
    }
}

/** Payments tab of a work: money given to labourers for this work. */
@Composable
fun WorkPaymentsTab(
    rows: List<WorkPaymentRow>,
    onAddPayment: () -> Unit,
    onOpenPayment: (labourerId: Long, paymentId: Long) -> Unit,
    onDeletePayment: (PaymentEntity) -> Unit,
) {
    var confirmDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    val total = rows.sumOf { it.payment.amountPaise }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item(key = "summary") {
            Card(Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth()) {
                Row(Modifier.padding(Dimens.CardPadding), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.payments_total),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(Money.format(total), style = MaterialTheme.typography.headlineSmall)
                    }
                    Button(onClick = onAddPayment) {
                        Icon(painterResource(R.drawable.ic_add), contentDescription = null)
                        Text(stringResource(R.string.payment_add), Modifier.padding(start = Dimens.SmallSpacing))
                    }
                }
            }
        }
        if (rows.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = R.drawable.ic_wallet,
                    title = stringResource(R.string.payments_empty_title),
                    message = stringResource(R.string.payments_empty_message),
                    inline = true,
                )
            }
        }
        items(rows, key = { it.payment.id }) { row ->
            val p = row.payment
            PaymentCard(
                row = row,
                onClick = { onOpenPayment(p.labourerId, p.id) },
                menu = listOf(
                    MenuAction(stringResource(R.string.action_edit)) { onOpenPayment(p.labourerId, p.id) },
                    MenuAction(stringResource(R.string.action_delete)) { confirmDeleteId = p.id },
                ),
                modifier = Modifier.widthIn(max = ContentMaxWidth),
            )
        }
    }

    val toDelete = rows.firstOrNull { it.payment.id == confirmDeleteId }?.payment
    if (toDelete != null) {
        ConfirmDialog(
            title = stringResource(R.string.payment_delete_title),
            message = stringResource(R.string.payment_delete_message),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                onDeletePayment(toDelete)
                confirmDeleteId = null
            },
            onDismiss = { confirmDeleteId = null },
        )
    }
}

@Composable
private fun PaymentCard(
    row: WorkPaymentRow,
    onClick: () -> Unit,
    menu: List<MenuAction>,
    modifier: Modifier = Modifier,
) {
    val p = row.payment
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(start = Dimens.CardPadding, top = Dimens.ItemSpacing, bottom = Dimens.ItemSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(row.labourerName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val detail = listOfNotNull(
                    DateFormats.format(p.date),
                    stringResource(p.type.labelRes),
                    stringResource(p.mode.labelRes),
                    p.reference.takeIf { it.isNotBlank() },
                ).joinToString(stringResource(R.string.list_separator))
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)

                if (p.periodFrom != null && p.periodTo != null) {
                    Text(
                        stringResource(R.string.payment_period_range, DateFormats.format(p.periodFrom), DateFormats.format(p.periodTo)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(Money.format(p.amountPaise), style = MaterialTheme.typography.titleSmall)
                if (row.photoCount > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(end = 4.dp),
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_photo),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(row.photoCount.toString(), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            OverflowMenu(menu)
        }
    }
}
