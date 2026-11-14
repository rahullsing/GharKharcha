package com.gharkharcha.ui.projects

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gharkharcha.R
import com.gharkharcha.data.dao.ProjectSummaryRow
import com.gharkharcha.domain.model.SpendTotals
import com.gharkharcha.ui.common.LoadState
import com.gharkharcha.ui.components.BackTopBar
import com.gharkharcha.ui.components.BudgetProgress
import com.gharkharcha.ui.components.ConfirmDialog
import com.gharkharcha.ui.components.DetailScaffold
import com.gharkharcha.ui.components.EmptyState
import com.gharkharcha.ui.components.LoadStateContent
import com.gharkharcha.ui.components.MenuAction
import com.gharkharcha.ui.components.MoneyText
import com.gharkharcha.ui.components.OverflowMenu
import com.gharkharcha.ui.components.budgetStatusText
import com.gharkharcha.ui.theme.Dimens
import androidx.compose.foundation.layout.height
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import java.io.File

@Composable
fun ProjectsScreen(
    onBack: () -> Unit,
    onAddProject: () -> Unit,
    onEditProject: (Long) -> Unit,
    viewModel: ProjectsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }

    DetailScaffold(
        topBar = { BackTopBar(title = stringResource(R.string.projects_title), onBack = onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddProject,
                icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                text = { Text(stringResource(R.string.project_add)) },
            )
        },
    ) { padding ->
        LoadStateContent(state, onRetry = viewModel::retry, modifier = Modifier.padding(padding)) { ui ->
            if (ui.projects.isEmpty()) {
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
            val editLabel = stringResource(R.string.action_edit)
            val deleteLabel = stringResource(R.string.action_delete)
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 320.dp),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(
                    start = Dimens.ScreenPadding,
                    end = Dimens.ScreenPadding,
                    top = Dimens.SmallSpacing,
                    bottom = Dimens.FabClearance,
                ),
                verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
                horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
            ) {
                items(ui.projects, key = { it.project.id }) { row ->
                    ProjectCard(
                        row = row,
                        coverPath = ui.covers[row.project.id],
                        isCurrent = row.project.id == ui.currentId,
                        onClick = {
                            viewModel.select(row.project.id)
                            onBack()
                        },
                        menu = listOf(
                            MenuAction(editLabel) { onEditProject(row.project.id) },
                            MenuAction(deleteLabel) { confirmDeleteId = row.project.id },
                        ),
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }

    val toDelete = (state as? LoadState.Ready)?.data?.projects?.firstOrNull { it.project.id == confirmDeleteId }?.project
    if (toDelete != null) {
        ConfirmDialog(
            title = stringResource(R.string.project_delete_title),
            message = stringResource(R.string.project_delete_message, toDelete.name),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                viewModel.delete(toDelete)
                confirmDeleteId = null
            },
            onDismiss = { confirmDeleteId = null },
        )
    }
}

@Composable
private fun ProjectCard(
    row: ProjectSummaryRow,
    coverPath: String?,
    isCurrent: Boolean,
    onClick: () -> Unit,
    menu: List<MenuAction>,
    modifier: Modifier = Modifier,
) {
    val totals = SpendTotals(row.materialPaise, row.labourPaise, row.project.budgetPaise)
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        border = if (isCurrent) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(),
    ) {
        if (coverPath != null) {
            AsyncImage(
                model = File(coverPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(120.dp),
            )
        }
        Column(
            Modifier.padding(start = Dimens.CardPadding, bottom = Dimens.CardPadding, end = Dimens.TinySpacing),
            verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(top = Dimens.CardPadding)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            row.project.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (isCurrent) {
                            Icon(
                                painterResource(R.drawable.ic_check),
                                contentDescription = stringResource(R.string.project_current),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    val subtitle = listOfNotNull(
                        row.project.address.takeIf { it.isNotBlank() },
                        pluralStringResource(R.plurals.project_works_count, row.workCount, row.workCount),
                    ).joinToString(stringResource(R.string.list_separator))
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                OverflowMenu(menu)
            }
            Column(Modifier.padding(end = Dimens.ItemSpacing), verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing)) {
                MoneyText(totals.grandTotalPaise, style = MaterialTheme.typography.titleLarge)
                BudgetProgress(totals)
                Text(
                    budgetStatusText(totals),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
