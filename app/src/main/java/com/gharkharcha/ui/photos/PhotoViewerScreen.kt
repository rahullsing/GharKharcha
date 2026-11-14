package com.gharkharcha.ui.photos

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.gharkharcha.R
import com.gharkharcha.ui.common.LoadState
import com.gharkharcha.ui.components.ConfirmDialog
import com.gharkharcha.ui.components.EmptyState
import com.gharkharcha.ui.components.LoadStateContent
import java.io.File

@Composable
fun PhotoViewerScreen(
    onBack: () -> Unit,
    viewModel: PhotoViewerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val photos = (state as? LoadState.Ready)?.data
    // Closing when the last photo was deleted.
    LaunchedEffect(photos) { if (photos != null && photos.isEmpty()) onBack() }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        LoadStateContent(state, onRetry = viewModel::retry) { list ->
            if (list.isEmpty()) {
                EmptyState(R.drawable.ic_photo, stringResource(R.string.photos_empty_title), null)
                return@LoadStateContent
            }
            ViewerPager(list, viewModel.startId, onBack, viewModel::saveCaption, viewModel::delete)
        }
    }
}

@Composable
private fun ViewerPager(
    photos: List<ViewerPhoto>,
    startId: Long,
    onBack: () -> Unit,
    onSaveCaption: (ViewerPhoto, String) -> Unit,
    onDelete: (ViewerPhoto) -> Unit,
) {
    val context = LocalContext.current
    val initial = photos.indexOfFirst { it.attachment.id == startId }.coerceAtLeast(0)
    val pager = rememberPagerState(initialPage = initial) { photos.size }
    val zoom = remember { mutableStateMapOf<Long, ZoomState>() }
    var editCaption by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val current = photos.getOrNull(pager.currentPage.coerceAtMost(photos.lastIndex))

    HorizontalPager(
        state = pager,
        key = { photos[it].attachment.id },
        // Swiping is off while zoomed so a pan doesn't flip pages.
        userScrollEnabled = zoom[current?.attachment?.id]?.isZoomed != true,
        modifier = Modifier.fillMaxSize(),
    ) { page ->
        val photo = photos[page]
        val zs = zoom.getOrPut(photo.attachment.id) { ZoomState() }
        AsyncImage(
            model = photo.file,
            contentDescription = photo.attachment.caption.ifBlank { null },
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().zoomable(zs),
        )
    }

    // Top bar over a soft gradient so icons stay visible on bright photos.
    Row(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)))
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.action_back), tint = Color.White)
        }
        Text(
            stringResource(R.string.photo_count, pager.currentPage + 1, photos.size),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        if (current != null) {
            IconButton(onClick = { share(context, current.file) }) {
                Icon(painterResource(R.drawable.ic_share), contentDescription = stringResource(R.string.photo_share), tint = Color.White)
            }
            IconButton(onClick = { editCaption = true }) {
                Icon(painterResource(R.drawable.ic_edit), contentDescription = stringResource(R.string.photo_add_caption), tint = Color.White)
            }
            IconButton(onClick = { confirmDelete = true }) {
                Icon(painterResource(R.drawable.ic_delete), contentDescription = stringResource(R.string.action_delete), tint = Color.White)
            }
        }
    }

    current?.attachment?.caption?.takeIf { it.isNotBlank() }?.let { caption ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Text(
                caption,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            )
        }
    }

    if (editCaption && current != null) {
        CaptionDialog(
            initial = current.attachment.caption,
            onSave = {
                onSaveCaption(current, it)
                editCaption = false
            },
            onDismiss = { editCaption = false },
        )
    }
    if (confirmDelete && current != null) {
        ConfirmDialog(
            title = stringResource(R.string.photo_delete_title),
            message = current.attachment.caption.ifBlank { stringResource(R.string.photo_count, pager.currentPage + 1, photos.size) },
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                confirmDelete = false
                onDelete(current)
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun CaptionDialog(initial: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.photo_caption)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = false, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** Opens the share sheet for an attachment through the app's FileProvider. */
fun share(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val send = Intent(Intent.ACTION_SEND)
        .setType("image/jpeg")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        // Nothing can receive images; ignore.
    }
}
