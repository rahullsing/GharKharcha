package com.gharkharcha.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gharkharcha.R
import com.gharkharcha.ui.common.LoadState
import com.gharkharcha.ui.theme.Dimens

/**
 * Friendly empty state: icon, title, one line of help and an optional primary action.
 * Use [inline] = true inside a LazyColumn item (a nested vertical scroll would crash there).
 */
@Composable
fun EmptyState(
    @DrawableRes icon: Int,
    title: String,
    message: String?,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    inline: Boolean = false,
) {
    val container = if (inline) modifier.padding(32.dp) else modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp)
    Box(
        modifier = container,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
            modifier = Modifier.widthIn(max = 360.dp),
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(88.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painterResource(icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
            Spacer(Modifier.height(Dimens.SmallSpacing))
            Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            if (message != null) {
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(Dimens.SmallSpacing))
                Button(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.state_loading)
    Box(
        modifier.fillMaxSize().semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorState(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
        ) {
            Icon(
                painterResource(R.drawable.ic_error),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp),
            )
            Text(stringResource(R.string.state_error_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.state_error_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
        }
    }
}

/** Renders loading / error automatically and hands ready data to [content]. */
@Composable
fun <T> LoadStateContent(
    state: LoadState<T>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    when (state) {
        LoadState.Loading -> LoadingState(modifier)
        is LoadState.Error -> ErrorState(onRetry, modifier)
        is LoadState.Ready -> content(state.data)
    }
}
