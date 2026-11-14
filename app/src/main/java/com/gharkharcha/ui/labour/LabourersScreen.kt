package com.gharkharcha.ui.labour

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.gharkharcha.R
import com.gharkharcha.ui.components.EmptyState
import com.gharkharcha.ui.components.TopLevelScaffold
import com.gharkharcha.ui.projects.ProjectSwitcherTitle

/** Labour tab shell; the labourer master, attendance sheet and ledger arrive in Phase 3. */
@Composable
fun LabourersScreen(
    onAddProject: () -> Unit,
    onManageProjects: () -> Unit,
) {
    TopLevelScaffold(
        title = {
            ProjectSwitcherTitle(
                fallbackTitle = stringResource(R.string.labourers_title),
                onAddProject = onAddProject,
                onManageProjects = onManageProjects,
            )
        },
    ) { padding ->
        EmptyState(
            icon = R.drawable.ic_groups,
            title = stringResource(R.string.labourers_empty_title),
            message = stringResource(R.string.labourers_empty_message),
            modifier = Modifier.padding(padding),
        )
    }
}
