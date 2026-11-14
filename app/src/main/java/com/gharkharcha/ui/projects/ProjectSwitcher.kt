package com.gharkharcha.ui.projects

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gharkharcha.R
import com.gharkharcha.domain.usecase.CurrentProjectUseCase
import com.gharkharcha.domain.usecase.ProjectsState
import com.gharkharcha.ui.theme.Dimens
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProjectSwitcherViewModel @Inject constructor(
    private val currentProject: CurrentProjectUseCase,
) : ViewModel() {
    val state: StateFlow<ProjectsState?> = currentProject.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun select(id: Long) {
        viewModelScope.launch { currentProject.select(id) }
    }
}

/**
 * Top-bar title showing the current house; tapping opens a sheet to switch, add or manage houses.
 * Shows [fallbackTitle] when there are no houses yet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectSwitcherTitle(
    fallbackTitle: String,
    onAddProject: () -> Unit,
    onManageProjects: () -> Unit,
    viewModel: ProjectSwitcherViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val current = state?.current
    var open by rememberSaveable { mutableStateOf(false) }

    if (current == null) {
        Text(fallbackTitle)
        return
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.TinySpacing),
        modifier = Modifier
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.project_switch)) { open = true }
            .padding(vertical = Dimens.TinySpacing),
    ) {
        Text(current.name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        Icon(painterResource(R.drawable.ic_unfold), contentDescription = null, modifier = Modifier.size(20.dp))
    }

    if (open) {
        ModalBottomSheet(onDismissRequest = { open = false }) {
            Column(Modifier.navigationBarsPadding()) {
                Text(
                    stringResource(R.string.project_switch),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SmallSpacing),
                )
                LazyColumn(Modifier.weight(1f, fill = false)) {
                    items(state?.projects.orEmpty(), key = { it.id }) { project ->
                        val selected = project.id == current.id
                        ListItem(
                            headlineContent = { Text(project.name) },
                            supportingContent = project.address.takeIf { it.isNotBlank() }?.let { { Text(it, maxLines = 1) } },
                            leadingContent = { Icon(painterResource(R.drawable.ic_house), contentDescription = null) },
                            trailingContent = if (selected) {
                                {
                                    Icon(
                                        painterResource(R.drawable.ic_check),
                                        contentDescription = stringResource(R.string.project_current),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            } else {
                                null
                            },
                            modifier = Modifier.clickable {
                                viewModel.select(project.id)
                                open = false
                            },
                        )
                    }
                }
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text(stringResource(R.string.project_add)) },
                    leadingContent = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                    modifier = Modifier.clickable {
                        open = false
                        onAddProject()
                    },
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.project_manage)) },
                    leadingContent = { Icon(painterResource(R.drawable.ic_edit), contentDescription = null) },
                    modifier = Modifier.clickable {
                        open = false
                        onManageProjects()
                    },
                )
            }
        }
    }
}
