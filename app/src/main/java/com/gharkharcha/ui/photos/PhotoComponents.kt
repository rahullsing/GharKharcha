package com.gharkharcha.ui.photos

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.gharkharcha.R
import com.gharkharcha.domain.model.PhotoItem
import com.gharkharcha.ui.theme.Dimens
import java.io.File

private const val MAX_PICK = 10

/**
 * "Add photo" chooser: camera or the system Photo Picker (no storage permission needed).
 * [multiple] lets the picker return several images.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPhotoSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onCamera: () -> Unit,
    onPicked: (List<Uri>) -> Unit,
    multiple: Boolean = true,
) {
    val pickMany = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAX_PICK)) {
        if (it.isNotEmpty()) onPicked(it)
    }
    val pickOne = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onPicked(listOf(uri))
    }
    if (!visible) return
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.navigationBarsPadding().padding(bottom = Dimens.SmallSpacing)) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.photo_take)) },
                leadingContent = { Icon(painterResource(R.drawable.ic_camera), contentDescription = null) },
                modifier = Modifier.clickable {
                    onDismiss()
                    onCamera()
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.photo_pick)) },
                leadingContent = { Icon(painterResource(R.drawable.ic_photo), contentDescription = null) },
                modifier = Modifier.clickable {
                    onDismiss()
                    val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    if (multiple) pickMany.launch(request) else pickOne.launch(request)
                },
            )
        }
    }
}

/** Horizontal row of thumbnails with remove buttons and a trailing "Add" tile. */
@Composable
fun PhotoStrip(
    title: String,
    photos: List<PhotoItem>,
    onAdd: () -> Unit,
    onRemove: (PhotoItem) -> Unit,
    onOpen: (PhotoItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing)) {
            items(photos, key = { it.key }) { photo ->
                Box(Modifier.size(THUMB)) {
                    AsyncImage(
                        model = File(photo.path),
                        contentDescription = stringResource(R.string.photo_open),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .clickable { onOpen(photo) },
                    )
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.55f),
                        contentColor = Color.White,
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(28.dp),
                    ) {
                        IconButton(onClick = { onRemove(photo) }) {
                            Icon(
                                painterResource(R.drawable.ic_close),
                                contentDescription = stringResource(R.string.photo_remove),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
            item(key = "add") { AddTile(onAdd) }
        }
    }
}

@Composable
private fun AddTile(onAdd: () -> Unit, size: Dp = THUMB) {
    Box(
        Modifier
            .size(size)
            .clip(MaterialTheme.shapes.medium)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.photo_add), onClick = onAdd),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(painterResource(R.drawable.ic_add_photo), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                stringResource(R.string.photo_add),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** One round photo (labourer) or a wide banner (house cover) with change/remove. */
@Composable
fun SinglePhotoField(
    label: String,
    photo: PhotoItem?,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    round: Boolean = true,
) {
    val shape = if (round) CircleShape else MaterialTheme.shapes.large
    val boxModifier = if (round) Modifier.size(96.dp) else Modifier.size(width = 200.dp, height = 112.dp)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(boxModifier) {
            if (photo == null) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(shape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .clickable(role = Role.Button, onClickLabel = label, onClick = onAdd),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(R.drawable.ic_add_photo),
                        contentDescription = label,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            } else {
                AsyncImage(
                    model = File(photo.path),
                    contentDescription = label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(shape).clickable(onClick = onOpen),
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.align(Alignment.BottomEnd).size(32.dp),
                ) {
                    IconButton(onClick = onRemove) {
                        Icon(
                            painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.photo_remove),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
        Text(
            if (photo == null) label else stringResource(R.string.photo_change),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(onClick = onAdd).padding(4.dp),
        )
    }
}

/** Full-screen zoomable preview of a single file (used for unsaved photos in forms). */
@Composable
fun PhotoPreviewDialog(path: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AsyncImage(
                model = File(path),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().zoomable(rememberZoomState()),
            )
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopStart).padding(12.dp)) {
                Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.action_back), tint = Color.White)
            }
        }
    }
}

/** Holds which photo a preview dialog shows; survives rotation. */
@Composable
fun rememberPreviewPath() = rememberSaveable { mutableStateOf<String?>(null) }

private val THUMB = 88.dp
