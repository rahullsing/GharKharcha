package com.gharkharcha.ui.works

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gharkharcha.R
import com.gharkharcha.data.entity.WorkStatus
import com.gharkharcha.domain.model.BudgetLevel
import com.gharkharcha.domain.model.WorkWithSpend
import com.gharkharcha.ui.common.labelRes
import com.gharkharcha.ui.components.BudgetProgress
import com.gharkharcha.ui.components.MenuAction
import com.gharkharcha.ui.components.MoneyText
import com.gharkharcha.ui.components.OverflowMenu
import com.gharkharcha.ui.components.budgetStatusText
import com.gharkharcha.ui.theme.Dimens
import com.gharkharcha.util.Money

@Composable
fun WorkStatusChip(status: WorkStatus, modifier: Modifier = Modifier) {
    val (container, content) = when (status) {
        WorkStatus.PLANNED -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        WorkStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        WorkStatus.DONE -> MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurfaceVariant
        WorkStatus.ON_HOLD -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    }
    Surface(color = container, contentColor = content, shape = MaterialTheme.shapes.small, modifier = modifier) {
        Text(
            stringResource(status.labelRes),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

/** List card: name + status, spend vs budget and a progress bar. */
@Composable
fun WorkCard(
    item: WorkWithSpend,
    onClick: () -> Unit,
    menu: List<MenuAction>,
    modifier: Modifier = Modifier,
) {
    val totals = item.totals
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(start = Dimens.CardPadding, bottom = Dimens.CardPadding, end = Dimens.TinySpacing),
            verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(top = Dimens.CardPadding)) {
                    Text(
                        item.work.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        stringResource(item.work.type.labelRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                WorkStatusChip(item.work.status, Modifier.padding(top = Dimens.CardPadding))
                OverflowMenu(menu)
            }
            Column(
                Modifier.padding(end = Dimens.ItemSpacing),
                verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
            ) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing)) {
                    MoneyText(totals.grandTotalPaise, style = MaterialTheme.typography.titleLarge)
                    if (totals.budgetPaise > 0) {
                        Text(
                            stringResource(R.string.of_amount, Money.format(totals.budgetPaise)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 2.dp),
                        )
                    }
                }
                BudgetProgress(totals)
                Text(
                    budgetStatusText(totals),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (totals.budgetLevel >= BudgetLevel.REACHED) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}
