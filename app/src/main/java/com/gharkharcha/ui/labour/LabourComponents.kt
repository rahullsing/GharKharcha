package com.gharkharcha.ui.labour

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import java.io.File
import com.gharkharcha.R
import com.gharkharcha.data.entity.LabourerEntity
import com.gharkharcha.domain.model.BalanceState
import com.gharkharcha.domain.model.LabourSummary
import com.gharkharcha.ui.theme.LocalExtendedColors
import com.gharkharcha.util.Money

/** Profile photo when there is one, otherwise the labourer's initials. */
@Composable
fun LabourerAvatar(name: String, modifier: Modifier = Modifier, size: Dp = 44.dp, photoPath: String? = null) {
    if (photoPath != null) {
        AsyncImage(
            model = File(photoPath),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        )
        return
    }
    val initials = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        .take(2).joinToString("") { it.first().uppercase() }
    Box(
        modifier
            .size(size)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initials,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = (size.value * 0.38f).sp),
        )
    }
}

/** Red when money is due to the labourer, green when they hold an advance. */
@Composable
fun balanceColor(state: BalanceState): Color = when (state) {
    BalanceState.DUE -> MaterialTheme.colorScheme.error
    BalanceState.ADVANCE -> LocalExtendedColors.current.positive
    BalanceState.SETTLED -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
fun balanceLabel(state: BalanceState): String = stringResource(
    when (state) {
        BalanceState.DUE -> R.string.balance_due
        BalanceState.ADVANCE -> R.string.balance_advance
        BalanceState.SETTLED -> R.string.balance_settled
    },
)

/** Right-aligned "₹2,300 / Due" block for list rows. */
@Composable
fun BalanceBlock(summary: LabourSummary?, modifier: Modifier = Modifier, amountStyle: TextStyle = MaterialTheme.typography.titleSmall) {
    val s = summary ?: LabourSummary.EMPTY
    val color = balanceColor(s.balanceState)
    Column(modifier, horizontalAlignment = Alignment.End) {
        if (s.balanceState != BalanceState.SETTLED) {
            Text(Money.format(kotlin.math.abs(s.pendingPaise)), style = amountStyle, color = color, textAlign = TextAlign.End)
        }
        Text(balanceLabel(s.balanceState), style = MaterialTheme.typography.labelSmall, color = color)
    }
}

/** "₹700/day · ₹90/hour" (only the rates that are set). */
@Composable
fun rateSummary(labourer: LabourerEntity): String? {
    val parts = buildList {
        if (labourer.dailyWagePaise > 0) add(stringResource(R.string.labourer_per_day, Money.format(labourer.dailyWagePaise)))
        if (labourer.hourlyRatePaise > 0) add(stringResource(R.string.labourer_per_hour, Money.format(labourer.hourlyRatePaise)))
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(stringResource(R.string.list_separator))
}
