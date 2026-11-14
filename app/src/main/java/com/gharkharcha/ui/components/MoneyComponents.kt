package com.gharkharcha.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.gharkharcha.R
import com.gharkharcha.domain.model.BudgetLevel
import com.gharkharcha.domain.model.SpendTotals
import com.gharkharcha.ui.theme.Dimens
import com.gharkharcha.util.Money

@Composable
fun MoneyText(
    paise: Long,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = Color.Unspecified,
) {
    Text(Money.format(paise), modifier = modifier, style = style, color = color, maxLines = 1)
}

/** Progress colour by budget level: normal → primary, 80%+ → tertiary, 100%+ → error. */
@Composable
fun budgetColor(level: BudgetLevel): Color = when (level) {
    BudgetLevel.NO_BUDGET, BudgetLevel.OK -> MaterialTheme.colorScheme.primary
    BudgetLevel.WARNING -> MaterialTheme.colorScheme.tertiary
    BudgetLevel.REACHED, BudgetLevel.OVER -> MaterialTheme.colorScheme.error
}

@Composable
fun BudgetProgress(totals: SpendTotals, modifier: Modifier = Modifier) {
    if (totals.budgetLevel == BudgetLevel.NO_BUDGET) return
    val color = budgetColor(totals.budgetLevel)
    LinearProgressIndicator(
        progress = { totals.progress },
        modifier = modifier.fillMaxWidth().height(8.dp),
        color = color,
        trackColor = color.copy(alpha = 0.18f),
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}

/** "62% of budget used" / "Over by ₹12,000" / "No budget set". */
@Composable
fun budgetStatusText(totals: SpendTotals): String = when (totals.budgetLevel) {
    BudgetLevel.NO_BUDGET -> stringResource(R.string.no_budget_set)
    BudgetLevel.OVER -> stringResource(R.string.over_budget_by, Money.format(-totals.variancePaise))
    else -> stringResource(R.string.budget_used_percent, ((totals.usedBasisPoints ?: 0) / 100).toInt())
}

/** Headline card: total spent vs budget, with material / labour split. */
@Composable
fun SpendOverviewCard(totals: SpendTotals, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(Dimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
        ) {
            Text(
                stringResource(R.string.grand_total),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MoneyText(totals.grandTotalPaise, style = MaterialTheme.typography.headlineMedium)
            if (totals.budgetPaise > 0) {
                Text(
                    stringResource(R.string.spent_of_budget, Money.format(totals.grandTotalPaise), Money.format(totals.budgetPaise)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
            HorizontalDivider(Modifier.padding(vertical = Dimens.TinySpacing))
            LabelValueRow(stringResource(R.string.materials), Money.format(totals.materialPaise))
            LabelValueRow(stringResource(R.string.labour), Money.format(totals.labourPaise))
            if (totals.budgetPaise > 0 && totals.variancePaise >= 0) {
                LabelValueRow(stringResource(R.string.remaining), Money.format(totals.variancePaise))
            }
        }
    }
}

@Composable
fun LabelValueRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
