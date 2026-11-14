package com.gharkharcha.ui.more

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.gharkharcha.R
import com.gharkharcha.ui.components.TopLevelScaffold

@Composable
fun MoreScreen(onOpenProjects: () -> Unit, onOpenVendors: () -> Unit) {
    TopLevelScaffold(title = { Text(stringResource(R.string.more_title)) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            MoreItem(
                icon = R.drawable.ic_house,
                title = stringResource(R.string.projects_title),
                subtitle = stringResource(R.string.more_projects_subtitle),
                onClick = onOpenProjects,
            )
            MoreItem(
                icon = R.drawable.ic_store,
                title = stringResource(R.string.vendors_title),
                subtitle = stringResource(R.string.more_vendors_subtitle),
                onClick = onOpenVendors,
            )
            MoreItem(
                icon = R.drawable.ic_chart,
                title = stringResource(R.string.more_reports),
                subtitle = stringResource(R.string.coming_soon),
                onClick = null,
            )
            MoreItem(
                icon = R.drawable.ic_settings,
                title = stringResource(R.string.more_settings),
                subtitle = stringResource(R.string.coming_soon),
                onClick = null,
            )
        }
    }
}

@Composable
private fun MoreItem(
    @DrawableRes icon: Int,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(painterResource(icon), contentDescription = null) },
        trailingContent = if (onClick != null) {
            { Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null) }
        } else {
            null
        },
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
    )
}
