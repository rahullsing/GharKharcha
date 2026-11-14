package com.gharkharcha.ui.works

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gharkharcha.R
import com.gharkharcha.data.entity.WorkEntity
import com.gharkharcha.ui.common.LoadState
import com.gharkharcha.ui.components.ConfirmDialog
import com.gharkharcha.ui.components.EmptyState
import com.gharkharcha.ui.components.LoadStateContent
import com.gharkharcha.ui.components.MenuAction
import com.gharkharcha.ui.components.TopLevelScaffold
import com.gharkharcha.ui.projects.ProjectSwitcherTitle
import com.gharkharcha.ui.theme.Dimens

@Composable
fun WorksScreen(
    onAddProject: () -> Unit,
    onManageProjects: () -> Unit,
    onOpenWork: (Long) -> Unit,
    onAddWork: (projectId: Long) -> Unit,
    onEditWork: (WorkEntity) -> Unit,
    viewModel: WorksViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val ready = (state as? LoadState.Ready)?.data
    val project = ready?.project
    var confirmDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }

    TopLevelScaffold(
        title = {
            ProjectSwitcherTitle(
                fallbackTitle = stringResource(R.string.works_title),
                onAddProject = onAddProject,
                onManageProjects = onManageProjects,
            )
        },
        floatingActionButton = {
            // Hidden while the list is empty: the empty state has its own "Add work" button.
            if (project != null && ready.works.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { onAddWork(project.id) },
                    icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                    text = { Text(stringResource(R.string.work_add)) },
                )
            }
        },
    ) { padding ->
        LoadStateContent(state, onRetry = viewModel::retry, modifier = Modifier.padding(padding)) { ui ->
            when {
                ui.project == null -> EmptyState(
                    icon = R.drawable.ic_house,
                    title = stringResource(R.string.projects_empty_title),
                    message = stringResource(R.string.projects_empty_message),
                    actionLabel = stringResource(R.string.project_add),
                    onAction = onAddProject,
                    modifier = Modifier.padding(padding),
                )
                ui.works.isEmpty() -> EmptyState(
                    icon = R.drawable.ic_build,
                    title = stringResource(R.string.works_empty_title),
                    message = stringResource(R.string.works_empty_message),
                    actionLabel = stringResource(R.string.work_add),
                    onAction = { onAddWork(ui.project.id) },
                    modifier = Modifier.padding(padding),
                )
                else -> {
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
                        items(ui.works, key = { it.work.id }) { item ->
                            WorkCard(
                                item = item,
                                onClick = { onOpenWork(item.work.id) },
                                menu = listOf(
                                    MenuAction(editLabel) { onEditWork(item.work) },
                                    MenuAction(deleteLabel) { confirmDeleteId = item.work.id },
                                ),
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }
        }
    }

    val toDelete = (state as? LoadState.Ready)?.data?.works?.firstOrNull { it.work.id == confirmDeleteId }?.work
    if (toDelete != null) {
        ConfirmDialog(
            title = stringResource(R.string.work_delete_title),
            message = stringResource(R.string.work_delete_message, toDelete.name),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                viewModel.delete(toDelete)
                confirmDeleteId = null
            },
            onDismiss = { confirmDeleteId = null },
        )
    }
}
