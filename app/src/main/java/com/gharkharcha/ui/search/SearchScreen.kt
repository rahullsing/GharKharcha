package com.gharkharcha.ui.search

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
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gharkharcha.R
import com.gharkharcha.ui.components.DetailScaffold
import com.gharkharcha.ui.theme.Dimens
import com.gharkharcha.util.DateFormats
import com.gharkharcha.util.Money

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenWork: (Long) -> Unit,
    onOpenLabourer: (Long) -> Unit,
    onOpenMaterial: (workId: Long, materialId: Long) -> Unit,
    onOpenPayment: (labourerId: Long, paymentId: Long) -> Unit,
    onOpenVendor: (Long) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    DetailScaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.action_back))
                    }
                },
                title = {
                    TextField(
                        value = query,
                        onValueChange = viewModel::onQueryChange,
                        placeholder = { Text(stringResource(R.string.search_hint)) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = viewModel::clearQuery) {
                                    Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.action_cancel))
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                    )
                },
            )
        },
    ) { padding ->
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(Dimens.ScreenPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Dimens.SmallSpacing),
                )
                Text(
                    stringResource(R.string.search_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@DetailScaffold
        }

        if (results.isEmpty) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(Dimens.ScreenPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    stringResource(R.string.search_empty, query),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@DetailScaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Works
            if (results.works.isNotEmpty()) {
                item {
                    SearchSectionHeader(stringResource(R.string.search_section_works, results.works.size))
                }
                items(results.works, key = { "w_${it.id}" }) { work ->
                    Card(
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                            .clickable { onOpenWork(work.id) },
                    ) {
                        ListItem(
                            leadingContent = {
                                Icon(painterResource(R.drawable.ic_build), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            headlineContent = { Text(work.name, fontWeight = FontWeight.Medium) },
                            supportingContent = {
                                if (work.notes.isNotBlank()) {
                                    Text(work.notes)
                                } else {
                                    Text(stringResource(R.string.search_type_work))
                                }
                            },
                            trailingContent = {
                                if (work.budgetPaise > 0) {
                                    Text(Money.format(work.budgetPaise), style = MaterialTheme.typography.labelMedium)
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
                    }
                }
            }

            // Labourers
            if (results.labourers.isNotEmpty()) {
                item {
                    SearchSectionHeader(stringResource(R.string.search_section_labourers, results.labourers.size))
                }
                items(results.labourers, key = { "l_${it.id}" }) { labourer ->
                    Card(
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                            .clickable { onOpenLabourer(labourer.id) },
                    ) {
                        ListItem(
                            leadingContent = {
                                Icon(painterResource(R.drawable.ic_groups), contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            },
                            headlineContent = { Text(labourer.name, fontWeight = FontWeight.Medium) },
                            supportingContent = {
                                val roleText = labourer.customSkill.ifBlank { stringResource(R.string.search_type_labourer) }
                                val phoneText = if (labourer.phone.isNotBlank()) " · ${labourer.phone}" else ""
                                Text("$roleText$phoneText")
                            },
                            trailingContent = {
                                if (labourer.dailyWagePaise > 0) {
                                    Text("${Money.format(labourer.dailyWagePaise)}/day", style = MaterialTheme.typography.labelMedium)
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
                    }
                }
            }

            // Materials
            if (results.materials.isNotEmpty()) {
                item {
                    SearchSectionHeader(stringResource(R.string.search_section_materials, results.materials.size))
                }
                items(results.materials, key = { "m_${it.id}" }) { material ->
                    Card(
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                            .clickable { onOpenMaterial(material.workId, material.id) },
                    ) {
                        ListItem(
                            leadingContent = {
                                Icon(painterResource(R.drawable.ic_store), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            headlineContent = { Text(material.itemName, fontWeight = FontWeight.Medium) },
                            supportingContent = {
                                val brandText = if (material.brand.isNotBlank()) "${material.brand} · " else ""
                                val shopText = if (material.shopName.isNotBlank()) "${material.shopName} · " else ""
                                Text("$brandText$shopText${DateFormats.format(material.purchaseDate)}")
                            },
                            trailingContent = {
                                Text(
                                    Money.format(material.totalPaise),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
                    }
                }
            }

            // Payments
            if (results.payments.isNotEmpty()) {
                item {
                    SearchSectionHeader(stringResource(R.string.search_section_payments, results.payments.size))
                }
                items(results.payments, key = { "p_${it.id}" }) { payment ->
                    Card(
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                            .clickable { onOpenPayment(payment.labourerId, payment.id) },
                    ) {
                        ListItem(
                            leadingContent = {
                                Icon(painterResource(R.drawable.ic_wallet), contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            },
                            headlineContent = { Text(Money.format(payment.amountPaise), fontWeight = FontWeight.SemiBold) },
                            supportingContent = {
                                val refText = if (payment.reference.isNotBlank()) "${payment.reference} · " else ""
                                Text("$refText${DateFormats.format(payment.date)}")
                            },
                            trailingContent = {
                                Text(payment.type.name, style = MaterialTheme.typography.labelSmall)
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
                    }
                }
            }

            // Vendors
            if (results.vendors.isNotEmpty()) {
                item {
                    SearchSectionHeader(stringResource(R.string.search_section_vendors, results.vendors.size))
                }
                items(results.vendors, key = { "v_${it.id}" }) { vendor ->
                    Card(
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                            .clickable { onOpenVendor(vendor.id) },
                    ) {
                        ListItem(
                            leadingContent = {
                                Icon(painterResource(R.drawable.ic_store), contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            },
                            headlineContent = { Text(vendor.name, fontWeight = FontWeight.Medium) },
                            supportingContent = {
                                val addressText = if (vendor.address.isNotBlank()) " · ${vendor.address}" else ""
                                Text("${vendor.phone}$addressText")
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchSectionHeader(title: String) {
    Row(
        modifier = Modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth()
            .padding(top = Dimens.SmallSpacing),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
