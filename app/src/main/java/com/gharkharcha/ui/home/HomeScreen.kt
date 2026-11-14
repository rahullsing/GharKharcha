package com.gharkharcha.ui.home

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gharkharcha.R
import com.gharkharcha.ui.common.LoadState
import com.gharkharcha.ui.common.labelRes
import com.gharkharcha.ui.components.EmptyState
import com.gharkharcha.ui.components.LoadStateContent
import com.gharkharcha.ui.components.SpendOverviewCard
import com.gharkharcha.ui.components.TopLevelScaffold
import com.gharkharcha.ui.components.budgetStatusText
import com.gharkharcha.ui.projects.ProjectSwitcherTitle
import com.gharkharcha.ui.theme.Dimens
import com.gharkharcha.util.DateFormats
import com.gharkharcha.util.Money

@Composable
fun HomeScreen(
    onAddProject: () -> Unit,
    onManageProjects: () -> Unit,
    onOpenWork: (Long) -> Unit,
    onSeeAllWorks: () -> Unit,
    onAddWork: (projectId: Long) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenBreakdown: (projectId: Long) -> Unit,
    onAddMaterial: (workId: Long) -> Unit,
    onMarkAttendance: (workId: Long) -> Unit,
    onAddPayment: (workId: Long) -> Unit,
    onOpenLabourer: (labourerId: Long) -> Unit,
    onOpenPayment: (labourerId: Long, paymentId: Long) -> Unit,
    onOpenMaterial: (workId: Long, materialId: Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentUi = (state as? LoadState.Ready<HomeUi>)?.data

    TopLevelScaffold(
        title = {
            ProjectSwitcherTitle(
                fallbackTitle = stringResource(R.string.app_name),
                onAddProject = onAddProject,
                onManageProjects = onManageProjects,
            )
        },
        actions = {
            IconButton(onClick = onOpenSearch) {
                Icon(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = stringResource(R.string.search_title),
                )
            }
        },
        floatingActionButton = {
            if (currentUi?.project != null) {
                val firstWorkId = currentUi.works.firstOrNull()?.work?.id ?: 0L
                QuickAddFab(
                    onAddMaterial = { onAddMaterial(firstWorkId) },
                    onMarkAttendance = { onMarkAttendance(firstWorkId) },
                    onAddPayment = { onAddPayment(firstWorkId) },
                )
            }
        },
    ) { padding ->
        LoadStateContent(state, onRetry = viewModel::retry, modifier = Modifier.padding(padding)) { ui ->
            if (ui.project == null) {
                EmptyState(
                    icon = R.drawable.ic_house,
                    title = stringResource(R.string.projects_empty_title),
                    message = stringResource(R.string.projects_empty_message),
                    actionLabel = stringResource(R.string.project_add),
                    onAction = onAddProject,
                    modifier = Modifier.padding(padding),
                )
                return@LoadStateContent
            }
            HomeContent(
                ui = ui,
                padding = padding,
                onOpenWork = onOpenWork,
                onSeeAllWorks = onSeeAllWorks,
                onAddWork = { onAddWork(ui.project.id) },
                onOpenBreakdown = { onOpenBreakdown(ui.project.id) },
                onOpenLabourer = onOpenLabourer,
                onOpenPayment = onOpenPayment,
                onOpenMaterial = onOpenMaterial,
            )
        }
    }
}

@Composable
private fun HomeContent(
    ui: HomeUi,
    padding: PaddingValues,
    onOpenWork: (Long) -> Unit,
    onSeeAllWorks: () -> Unit,
    onAddWork: () -> Unit,
    onOpenBreakdown: () -> Unit,
    onOpenLabourer: (Long) -> Unit,
    onOpenPayment: (Long, Long) -> Unit,
    onOpenMaterial: (Long, Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(
            start = Dimens.ScreenPadding,
            top = Dimens.ScreenPadding,
            end = Dimens.ScreenPadding,
            bottom = Dimens.FabClearance,
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Budget alerts
        if (ui.budgetAlerts.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
                ) {
                    ui.budgetAlerts.forEach { alert ->
                        val isExceeded = alert is BudgetAlert.Exceeded
                        val containerColor = if (isExceeded) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.tertiaryContainer
                        }
                        val contentColor = if (isExceeded) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        }
                        val percent = if (alert.totals.budgetPaise > 0) {
                            ((alert.totals.grandTotalPaise * 100) / alert.totals.budgetPaise).toInt()
                        } else 0
                        val spentFormatted = Money.format(alert.totals.grandTotalPaise)
                        val budgetFormatted = Money.format(alert.totals.budgetPaise)
                        val alertText = if (isExceeded) {
                            stringResource(R.string.alert_budget_exceeded, alert.work.name, percent, spentFormatted, budgetFormatted)
                        } else {
                            stringResource(R.string.alert_budget_near, alert.work.name, percent, spentFormatted, budgetFormatted)
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = containerColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenWork(alert.work.id) },
                        ) {
                            Row(
                                modifier = Modifier.padding(Dimens.CardPadding),
                                horizontalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_error),
                                    contentDescription = null,
                                    tint = contentColor,
                                    modifier = Modifier.size(24.dp),
                                )
                                Text(
                                    text = alertText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = contentColor,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Spend overview card
        item {
            SpendOverviewCard(ui.totals, Modifier.widthIn(max = 600.dp))
        }

        // Today's labour & Pending dues 2-card row
        item {
            Row(
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                ) {
                    Column(
                        modifier = Modifier.padding(Dimens.CardPadding),
                        verticalArrangement = Arrangement.spacedBy(Dimens.TinySpacing),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_groups),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                stringResource(R.string.home_today_labour),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            Money.format(ui.todayLabourCostPaise),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                ) {
                    Column(
                        modifier = Modifier.padding(Dimens.CardPadding),
                        verticalArrangement = Arrangement.spacedBy(Dimens.TinySpacing),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_wallet),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error,
                            )
                            Text(
                                stringResource(R.string.home_pending_dues),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (ui.pendingLabourDuesPaise > 0L) {
                            Text(
                                Money.format(ui.pendingLabourDuesPaise),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                            )
                        } else {
                            Text(
                                stringResource(R.string.home_no_dues),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }

        // Breakdown banner card
        item {
            OutlinedCard(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .clickable(onClick = onOpenBreakdown),
            ) {
                Row(
                    modifier = Modifier.padding(Dimens.CardPadding),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_chart),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.breakdown_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            stringResource(R.string.breakdown_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_right),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Donut Chart
        item {
            SpendDonutChart(
                slices = ui.donutSlices,
                totalPaise = ui.totals.grandTotalPaise,
                modifier = Modifier.widthIn(max = 600.dp),
            )
        }

        // Daily Spend 30-Day Bar Chart
        item {
            DailySpendBarChart(
                bars = ui.dailySpendBars,
                modifier = Modifier.widthIn(max = 600.dp),
            )
        }

        // Spend by work list
        if (ui.works.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(top = Dimens.SmallSpacing),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.home_by_work),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    TextButton(onClick = onSeeAllWorks) {
                        Text(stringResource(R.string.home_see_all_works))
                    }
                }
            }
            item {
                Card(Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                    ui.works.take(MAX_WORKS).forEach { item ->
                        ListItem(
                            headlineContent = { Text(item.work.name, fontWeight = FontWeight.Medium) },
                            supportingContent = { Text(budgetStatusText(item.totals)) },
                            trailingContent = {
                                Text(
                                    Money.format(item.totals.grandTotalPaise),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { onOpenWork(item.work.id) },
                        )
                    }
                }
            }
        } else {
            item {
                Card(Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                    Column(
                        Modifier.padding(Dimens.CardPadding),
                        verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
                    ) {
                        Text(stringResource(R.string.works_empty_title), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.works_empty_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(onClick = onAddWork) {
                            Icon(painterResource(R.drawable.ic_add), contentDescription = null)
                            Text(stringResource(R.string.work_add), Modifier.padding(start = Dimens.SmallSpacing))
                        }
                    }
                }
            }
        }

        // Recent activity feed
        if (ui.recentActivity.isNotEmpty()) {
            item {
                Text(
                    stringResource(R.string.activity_recent_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(top = Dimens.SmallSpacing),
                )
            }
            item {
                Card(Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                    ui.recentActivity.forEach { activity ->
                        when (activity) {
                            is RecentActivityItem.Material -> {
                                val brandText = if (activity.material.brand.isNotBlank()) " · ${activity.material.brand}" else ""
                                ListItem(
                                    leadingContent = {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_store),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    },
                                    headlineContent = { Text(activity.material.itemName, fontWeight = FontWeight.Medium) },
                                    supportingContent = {
                                        Text("${activity.workName}$brandText · ${DateFormats.format(activity.date)}")
                                    },
                                    trailingContent = {
                                        Text(
                                            Money.format(activity.amountPaise),
                                            style = MaterialTheme.typography.titleSmall,
                                        )
                                    },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    modifier = Modifier.clickable {
                                        onOpenMaterial(activity.material.workId, activity.material.id)
                                    },
                                )
                            }
                            is RecentActivityItem.Attendance -> {
                                val statusLabel = stringResource(activity.entry.attendance.status.labelRes)
                                ListItem(
                                    leadingContent = {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_groups),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.tertiary,
                                        )
                                    },
                                    headlineContent = {
                                        Text(activity.entry.labourerName, fontWeight = FontWeight.Medium)
                                    },
                                    supportingContent = {
                                        Text("${activity.entry.workName} · $statusLabel · ${DateFormats.format(activity.date)}")
                                    },
                                    trailingContent = {
                                        Text(
                                            Money.format(activity.amountPaise),
                                            style = MaterialTheme.typography.titleSmall,
                                        )
                                    },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    modifier = Modifier.clickable {
                                        onOpenLabourer(activity.entry.attendance.labourerId)
                                    },
                                )
                            }
                            is RecentActivityItem.Payment -> {
                                val workLabel = activity.workName?.let { "$it · " }.orEmpty()
                                ListItem(
                                    leadingContent = {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_wallet),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                        )
                                    },
                                    headlineContent = {
                                        Text(activity.labourerName, fontWeight = FontWeight.Medium)
                                    },
                                    supportingContent = {
                                        Text("$workLabel${DateFormats.format(activity.date)}")
                                    },
                                    trailingContent = {
                                        Text(
                                            Money.format(activity.amountPaise),
                                            style = MaterialTheme.typography.titleSmall,
                                        )
                                    },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    modifier = Modifier.clickable {
                                        onOpenPayment(activity.payment.labourerId, activity.payment.id)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val MAX_WORKS = 6
