package com.gharkharcha.ui.analytics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.gharkharcha.domain.model.BalanceState
import com.gharkharcha.ui.components.DetailScaffold
import com.gharkharcha.ui.components.LoadStateContent
import com.gharkharcha.ui.components.budgetStatusText
import com.gharkharcha.ui.theme.Dimens
import com.gharkharcha.util.DateFormats
import com.gharkharcha.util.Money
import com.gharkharcha.util.Quantity

private enum class BreakdownTab(val labelRes: Int) {
    WORK(R.string.breakdown_tab_work),
    CATEGORY(R.string.breakdown_tab_category),
    MONTH(R.string.breakdown_tab_month),
    LABOURER(R.string.breakdown_tab_labourer),
    VENDOR(R.string.breakdown_tab_vendor),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseBreakdownScreen(
    onBack: () -> Unit,
    onOpenWork: (Long) -> Unit,
    onOpenLabourer: (Long) -> Unit,
    onOpenVendor: (Long) -> Unit,
    viewModel: HouseBreakdownViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(BreakdownTab.WORK.ordinal) }

    DetailScaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.breakdown_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        LoadStateContent(state, onRetry = viewModel::retry, modifier = Modifier.padding(padding)) { ui ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                PrimaryScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = Dimens.ScreenPadding,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    BreakdownTab.entries.forEachIndexed { index, tab ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(stringResource(tab.labelRes)) },
                        )
                    }
                }

                when (BreakdownTab.entries[selectedTab]) {
                    BreakdownTab.WORK -> WorksBreakdownTab(ui, onOpenWork)
                    BreakdownTab.CATEGORY -> CategoryBreakdownTab(ui)
                    BreakdownTab.MONTH -> MonthBreakdownTab(ui)
                    BreakdownTab.LABOURER -> LabourersBreakdownTab(ui, onOpenLabourer)
                    BreakdownTab.VENDOR -> VendorsBreakdownTab(ui, onOpenVendor)
                }
            }
        }
    }
}

@Composable
private fun WorksBreakdownTab(
    ui: BreakdownUi,
    onOpenWork: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items(ui.works, key = { it.work.id }) { item ->
            Card(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .clickable { onOpenWork(item.work.id) },
            ) {
                Column(
                    modifier = Modifier.padding(Dimens.CardPadding),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(item.work.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(Money.format(item.totals.grandTotalPaise), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "${stringResource(R.string.breakdown_category_material)}: ${Money.format(item.totals.materialPaise)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "${stringResource(R.string.breakdown_category_labour)}: ${Money.format(item.totals.labourPaise)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (item.totals.budgetPaise > 0) {
                        val progress = (item.totals.grandTotalPaise.toFloat() / item.totals.budgetPaise).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            budgetStatusText(item.totals),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryBreakdownTab(ui: BreakdownUi) {
    val cat = ui.category
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Card(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(Dimens.CardPadding),
                    verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
                ) {
                    Text(
                        stringResource(R.string.grand_total),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        Money.format(cat.grandTotalPaise),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    if (cat.grandTotalPaise > 0L) {
                        val matRatio = (cat.materialPaise.toFloat() / cat.grandTotalPaise).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { matRatio },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(stringResource(R.string.breakdown_category_material), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Money.format(cat.materialPaise), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(stringResource(R.string.breakdown_category_labour), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Money.format(cat.labourPaise), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (cat.unpaidShopsPaise > 0L) {
                        Card(
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(Dimens.SmallSpacing),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    stringResource(R.string.breakdown_category_unpaid),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                                Text(
                                    Money.format(cat.unpaidShopsPaise),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthBreakdownTab(ui: BreakdownUi) {
    if (ui.months.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(Dimens.ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                stringResource(R.string.breakdown_empty_month),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items(ui.months, key = { it.month.toString() }) { item ->
            Card(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(Dimens.CardPadding),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(DateFormats.format(item.month), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(Money.format(item.totalPaise), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "${stringResource(R.string.breakdown_category_material)}: ${Money.format(item.materialPaise)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "${stringResource(R.string.breakdown_category_labour)}: ${Money.format(item.labourPaise)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LabourersBreakdownTab(
    ui: BreakdownUi,
    onOpenLabourer: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items(ui.labourers, key = { it.labourer.id }) { item ->
            Card(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .clickable { onOpenLabourer(item.labourer.id) },
            ) {
                Column(
                    modifier = Modifier.padding(Dimens.CardPadding),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(item.labourer.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(Money.format(item.summary.earnedPaise), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            Quantity.formatDays(item.summary.dayHundredths) + " days",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        val balanceColor = when (item.summary.balanceState) {
                            BalanceState.DUE -> MaterialTheme.colorScheme.error
                            BalanceState.ADVANCE -> MaterialTheme.colorScheme.tertiary
                            BalanceState.SETTLED -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        val balanceText = when (item.summary.balanceState) {
                            BalanceState.DUE -> "Due: ${Money.format(item.summary.pendingPaise)}"
                            BalanceState.ADVANCE -> "Advance: ${Money.format(-item.summary.pendingPaise)}"
                            BalanceState.SETTLED -> "Settled"
                        }
                        Text(balanceText, style = MaterialTheme.typography.bodySmall, color = balanceColor, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun VendorsBreakdownTab(
    ui: BreakdownUi,
    onOpenVendor: (Long) -> Unit,
) {
    if (ui.vendors.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(Dimens.ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                stringResource(R.string.breakdown_empty_vendor),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items(ui.vendors, key = { it.name + it.vendorId }) { item ->
            val clickableModifier = if (item.vendorId != null) {
                Modifier.clickable { onOpenVendor(item.vendorId) }
            } else {
                Modifier
            }

            Card(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .then(clickableModifier),
            ) {
                ListItem(
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_store),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    headlineContent = { Text(item.name, fontWeight = FontWeight.SemiBold) },
                    supportingContent = {
                        val unpaidText = if (item.unpaidAmountPaise > 0) {
                            " · Unpaid: ${Money.format(item.unpaidAmountPaise)}"
                        } else ""
                        Text("${item.purchaseCount} purchases$unpaidText")
                    },
                    trailingContent = {
                        Text(
                            Money.format(item.totalAmountPaise),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }
    }
}
