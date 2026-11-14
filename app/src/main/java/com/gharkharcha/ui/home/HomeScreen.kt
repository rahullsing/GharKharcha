package com.gharkharcha.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gharkharcha.R
import com.gharkharcha.ui.components.EmptyState
import com.gharkharcha.ui.components.LoadStateContent
import com.gharkharcha.ui.components.SpendOverviewCard
import com.gharkharcha.ui.components.TopLevelScaffold
import com.gharkharcha.ui.components.budgetStatusText
import com.gharkharcha.ui.projects.ProjectSwitcherTitle
import com.gharkharcha.ui.theme.Dimens
import com.gharkharcha.util.Money

@Composable
fun HomeScreen(
    onAddProject: () -> Unit,
    onManageProjects: () -> Unit,
    onOpenWork: (Long) -> Unit,
    onSeeAllWorks: () -> Unit,
    onAddWork: (projectId: Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TopLevelScaffold(
        title = {
            ProjectSwitcherTitle(
                fallbackTitle = stringResource(R.string.app_name),
                onAddProject = onAddProject,
                onManageProjects = onManageProjects,
            )
        },
    ) { padding ->
        // The modifier applies to the loading / error states only.
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
            HomeContent(ui, padding, onOpenWork, onSeeAllWorks, onAddWork = { onAddWork(ui.project.id) })
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
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            SpendOverviewCard(ui.totals, Modifier.widthIn(max = 600.dp))
        }
        if (ui.works.isNotEmpty()) {
            item {
                Text(
                    stringResource(R.string.home_by_work),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(top = Dimens.SmallSpacing),
                )
            }
            item {
                Card(Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                    ui.works.take(MAX_WORKS).forEach { item ->
                        ListItem(
                            headlineContent = { Text(item.work.name) },
                            supportingContent = { Text(budgetStatusText(item.totals)) },
                            trailingContent = {
                                Text(Money.format(item.totals.grandTotalPaise), style = MaterialTheme.typography.titleSmall)
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { onOpenWork(item.work.id) },
                        )
                    }
                }
            }
        }
        if (ui.works.isEmpty()) {
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
        } else {
            item {
                TextButton(onClick = onSeeAllWorks) { Text(stringResource(R.string.home_see_all_works)) }
            }
        }
    }
}

private const val MAX_WORKS = 6
