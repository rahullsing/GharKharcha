package com.gharkharcha.ui.labour

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gharkharcha.R
import com.gharkharcha.ui.common.LoadState
import com.gharkharcha.ui.common.labelRes
import com.gharkharcha.ui.common.skillLabel
import com.gharkharcha.ui.components.EmptyState
import com.gharkharcha.ui.components.LoadStateContent
import com.gharkharcha.ui.components.TopLevelScaffold
import com.gharkharcha.ui.projects.ProjectSwitcherTitle
import com.gharkharcha.ui.theme.Dimens
import com.gharkharcha.util.Money

private val ContentMaxWidth = 600.dp

@Composable
fun LabourersScreen(
    onAddProject: () -> Unit,
    onManageProjects: () -> Unit,
    onAddLabourer: () -> Unit,
    onOpenLabourer: (Long) -> Unit,
    onMarkAttendance: () -> Unit,
    viewModel: LabourersViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val hasAny = ((state as? LoadState.Ready)?.data?.totalCount ?: 0) > 0

    TopLevelScaffold(
        title = {
            ProjectSwitcherTitle(
                fallbackTitle = stringResource(R.string.labourers_title),
                onAddProject = onAddProject,
                onManageProjects = onManageProjects,
            )
        },
        floatingActionButton = {
            if (hasAny) {
                ExtendedFloatingActionButton(
                    onClick = onAddLabourer,
                    icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                    text = { Text(stringResource(R.string.labourer_add)) },
                )
            }
        },
    ) { padding ->
        LoadStateContent(state, onRetry = viewModel::retry, modifier = Modifier.padding(padding)) { ui ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(
                    start = Dimens.ScreenPadding,
                    end = Dimens.ScreenPadding,
                    top = Dimens.SmallSpacing,
                    bottom = Dimens.FabClearance,
                ),
                verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (ui.hasProject && ui.totalCount > 0) {
                    item(key = "attendance") {
                        AttendanceCard(ui.hasWorks, onMarkAttendance, Modifier.widthIn(max = ContentMaxWidth))
                    }
                }
                if (ui.totalCount == 0) {
                    item(key = "empty") {
                        EmptyState(
                            icon = R.drawable.ic_groups,
                            title = stringResource(R.string.labourers_empty_title),
                            message = stringResource(R.string.labourers_empty_message),
                            actionLabel = stringResource(R.string.labourer_add),
                            onAction = onAddLabourer,
                            modifier = Modifier.padding(top = 48.dp),
                            inline = true,
                        )
                    }
                    return@LazyColumn
                }
                if (ui.totalDuePaise > 0) {
                    item(key = "due") {
                        Text(
                            stringResource(R.string.labourers_total_due, Money.format(ui.totalDuePaise)),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth().padding(top = Dimens.SmallSpacing),
                        )
                    }
                }
                item(key = "search") {
                    OutlinedTextField(
                        value = query,
                        onValueChange = viewModel::onQuery,
                        placeholder = { Text(stringResource(R.string.labourers_search)) },
                        leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
                        trailingIcon = if (query.isNotEmpty()) {
                            {
                                IconButton(onClick = { viewModel.onQuery("") }) {
                                    Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.action_clear))
                                }
                            }
                        } else {
                            null
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth(),
                    )
                }
                item(key = "filters") {
                    FilterRow(ui, filter, viewModel::onFilter, Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth())
                }
                if (ui.rows.isEmpty()) {
                    item(key = "nomatch") {
                        Text(
                            stringResource(R.string.labourers_no_match),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 32.dp),
                        )
                    }
                }
                items(ui.rows, key = { it.labourer.id }) { row ->
                    LabourerListRow(
                        row,
                        onClick = { onOpenLabourer(row.labourer.id) },
                        modifier = Modifier.widthIn(max = ContentMaxWidth).animateItem(),
                    )
                }
            }
        }
    }
}

@Composable
private fun AttendanceCard(hasWorks: Boolean, onMark: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            Modifier.padding(Dimens.CardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(if (hasWorks) R.string.attendance_card_title else R.string.attendance_no_works_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    stringResource(if (hasWorks) R.string.attendance_card_message else R.string.attendance_no_works_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            if (hasWorks) {
                Button(onClick = onMark) { Text(stringResource(R.string.attendance_mark)) }
            }
        }
    }
}

@Composable
private fun FilterRow(
    ui: LabourersUi,
    selected: LabourerFilter,
    onSelect: (LabourerFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = buildList {
        add(LabourerFilter.All)
        ui.skills.forEach { add(LabourerFilter.BySkill(it)) }
        if (ui.hasInactive) add(LabourerFilter.Inactive)
    }
    if (options.size <= 1) return
    LazyRow(modifier, horizontalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing)) {
        items(options, key = { it.toString() }) { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = {
                    Text(
                        when (option) {
                            LabourerFilter.All -> stringResource(R.string.filter_all)
                            LabourerFilter.Inactive -> stringResource(R.string.filter_inactive)
                            is LabourerFilter.BySkill -> stringResource(option.skill.labelRes)
                        },
                    )
                },
            )
        }
    }
}

@Composable
private fun LabourerListRow(row: LabourerRow, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val l = row.labourer
    Card(modifier = modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing)) {
                    Text(l.name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (!l.isActive) {
                        Text(
                            stringResource(R.string.labourer_inactive_tag),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            supportingContent = {
                val rate = rateSummary(l)
                Text(
                    listOfNotNull(l.skillLabel(), rate).joinToString(stringResource(R.string.list_separator)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingContent = { LabourerAvatar(l.name, photoPath = row.photoPath) },
            trailingContent = { BalanceBlock(row.summary) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.clickable(onClick = onClick),
        )
    }
}
