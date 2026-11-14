package com.gharkharcha.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gharkharcha.R
import com.gharkharcha.ui.theme.Dimens
import com.gharkharcha.util.DateFormats
import com.gharkharcha.util.Money
import java.time.LocalDate

val ChartPalette = listOf(
    Color(0xFF2563EB), // Blue
    Color(0xFF10B981), // Emerald
    Color(0xFFF59E0B), // Amber
    Color(0xFF8B5CF6), // Violet
    Color(0xFFEC4899), // Pink
    Color(0xFF06B6D4), // Cyan
    Color(0xFFF97316), // Orange
    Color(0xFF14B8A6), // Teal
    Color(0xFF6366F1), // Indigo
    Color(0xFF84CC16), // Lime
)

data class DonutSlice(
    val id: Long,
    val name: String,
    val amountPaise: Long,
    val percentage: Float,
    val color: Color,
)

data class DailySpendBar(
    val date: LocalDate,
    val totalPaise: Long,
    val isToday: Boolean,
)

/**
 * Spend by work Donut Chart drawn using Compose Canvas.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpendDonutChart(
    slices: List<DonutSlice>,
    totalPaise: Long,
    modifier: Modifier = Modifier,
) {
    Card(modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Dimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.chart_donut_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    Money.format(totalPaise),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            if (slices.isEmpty() || totalPaise <= 0L) {
                Text(
                    stringResource(R.string.chart_donut_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = Dimens.CardPadding),
                )
                return@Column
            }

            // Donut Canvas with center text
            val strokeColor = MaterialTheme.colorScheme.surfaceContainerHighest
            val animationProgress = remember { Animatable(0f) }
            LaunchedEffect(slices) {
                animationProgress.snapTo(0f)
                animationProgress.animateTo(1f, animationSpec = tween(700))
            }

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                Canvas(modifier = Modifier.size(180.dp)) {
                    val strokeWidth = 32.dp.toPx()
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

                    // Background track
                    drawArc(
                        color = strokeColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth),
                    )

                    // Draw slices
                    var currentAngle = -90f
                    for (slice in slices) {
                        val sweep = slice.percentage * 360f * animationProgress.value
                        if (sweep > 0.5f) {
                            drawArc(
                                color = slice.color,
                                startAngle = currentAngle,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                            )
                        }
                        currentAngle += sweep
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.grand_total),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        Money.format(totalPaise),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // Legend
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
                verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
            ) {
                slices.forEach { slice ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(slice.color, CircleShape),
                            )
                            Text(
                                slice.name,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                "${(slice.percentage * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 30-day Daily Spend Bar Chart drawn using Compose Canvas.
 */
@Composable
fun DailySpendBarChart(
    bars: List<DailySpendBar>,
    modifier: Modifier = Modifier,
) {
    var selectedIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    val total30Days = bars.sumOf { it.totalPaise }
    val maxPaise = (bars.maxOfOrNull { it.totalPaise } ?: 0L).coerceAtLeast(1L)

    Card(modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Dimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.chart_bars_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(R.string.chart_bars_total, Money.format(total30Days)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            if (bars.isEmpty() || total30Days <= 0L) {
                Text(
                    stringResource(R.string.chart_bars_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = Dimens.CardPadding),
                )
                return@Column
            }

            // Interactive selection pill
            val selected = selectedIndex?.let { bars.getOrNull(it) }
            if (selected != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text(
                        "${DateFormats.format(selected.date)}: ${Money.format(selected.totalPaise)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            val primaryColor = MaterialTheme.colorScheme.primary
            val tertiaryColor = MaterialTheme.colorScheme.tertiary
            val emptyBarColor = MaterialTheme.colorScheme.surfaceContainerHighest
            val axisColor = MaterialTheme.colorScheme.outlineVariant
            val selectedColor = MaterialTheme.colorScheme.error

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .pointerInput(bars) {
                        detectTapGestures { offset ->
                            val barSlot = size.width / bars.size.toFloat()
                            val index = (offset.x / barSlot).toInt().coerceIn(0, bars.lastIndex)
                            selectedIndex = if (selectedIndex == index) null else index
                        }
                    },
            ) {
                val chartHeight = size.height - 20.dp.toPx()
                val barCount = bars.size
                val slotWidth = size.width / barCount.toFloat()
                val barWidth = (slotWidth * 0.65f).coerceAtLeast(3f)

                // Baseline axis
                drawLine(
                    color = axisColor,
                    start = Offset(0f, chartHeight),
                    end = Offset(size.width, chartHeight),
                    strokeWidth = 1.dp.toPx(),
                )

                bars.forEachIndexed { i, bar ->
                    val x = i * slotWidth + (slotWidth - barWidth) / 2f
                    val heightRatio = (bar.totalPaise.toFloat() / maxPaise).coerceIn(0f, 1f)
                    val barHeight = (chartHeight * heightRatio).coerceAtLeast(if (bar.totalPaise > 0) 6.dp.toPx() else 2.dp.toPx())
                    val y = chartHeight - barHeight

                    val color = when {
                        selectedIndex == i -> selectedColor
                        bar.totalPaise <= 0 -> emptyBarColor
                        bar.isToday -> primaryColor
                        else -> tertiaryColor
                    }

                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                    )
                }
            }

            // X-axis date markers (first day, middle day, today)
            if (bars.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        DateFormats.formatCompact(bars.first().date),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        DateFormats.formatCompact(bars[bars.size / 2].date),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        DateFormats.formatCompact(bars.last().date),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
