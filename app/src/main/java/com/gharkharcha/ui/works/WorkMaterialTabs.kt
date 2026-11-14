package com.gharkharcha.ui.works

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.gharkharcha.R
import com.gharkharcha.data.entity.MaterialEntity
import com.gharkharcha.ui.common.unitLabel
import com.gharkharcha.ui.components.ConfirmDialog
import com.gharkharcha.ui.components.EmptyState
import com.gharkharcha.ui.components.MenuAction
import com.gharkharcha.ui.components.OverflowMenu
import com.gharkharcha.ui.theme.Dimens
import com.gharkharcha.util.DateFormats
import com.gharkharcha.util.Money
import com.gharkharcha.util.Quantity

private val ContentMaxWidth = 600.dp

/** Materials tab: totals, an add button and one card per purchase. */
@Composable
fun WorkMaterialsTab(
    rows: List<WorkMaterialRow>,
    onAdd: () -> Unit,
    onOpen: (MaterialEntity) -> Unit,
    onDuplicate: (MaterialEntity) -> Unit,
    onDelete: (MaterialEntity) -> Unit,
) {
    var confirmDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    val total = rows.sumOf { it.material.totalPaise }
    val unpaid = rows.filter { !it.material.isPaid }.sumOf { it.material.totalPaise }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item(key = "summary") {
            Card(Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth()) {
                Row(Modifier.padding(Dimens.CardPadding), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.materials_total),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(Money.format(total), style = MaterialTheme.typography.headlineSmall)
                        if (unpaid > 0) {
                            Text(
                                stringResource(R.string.materials_unpaid_total, Money.format(unpaid)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    Button(onClick = onAdd) {
                        Icon(painterResource(R.drawable.ic_add), contentDescription = null)
                        Text(stringResource(R.string.material_add), Modifier.padding(start = Dimens.SmallSpacing))
                    }
                }
            }
        }
        if (rows.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = R.drawable.ic_build,
                    title = stringResource(R.string.materials_empty_title),
                    message = stringResource(R.string.materials_empty_message),
                    inline = true,
                )
            }
        }
        items(rows, key = { it.material.id }) { row ->
            MaterialCard(
                row = row,
                onClick = { onOpen(row.material) },
                menu = listOf(
                    MenuAction(stringResource(R.string.action_edit)) { onOpen(row.material) },
                    MenuAction(stringResource(R.string.material_duplicate)) { onDuplicate(row.material) },
                    MenuAction(stringResource(R.string.action_delete)) { confirmDeleteId = row.material.id },
                ),
                modifier = Modifier.widthIn(max = ContentMaxWidth).animateItem(),
            )
        }
    }

    val toDelete = rows.firstOrNull { it.material.id == confirmDeleteId }?.material
    if (toDelete != null) {
        ConfirmDialog(
            title = stringResource(R.string.material_delete_title),
            message = stringResource(R.string.material_delete_message, toDelete.itemName),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                onDelete(toDelete)
                confirmDeleteId = null
            },
            onDismiss = { confirmDeleteId = null },
        )
    }
}

@Composable
private fun MaterialCard(row: WorkMaterialRow, onClick: () -> Unit, menu: List<MenuAction>, modifier: Modifier = Modifier) {
    val m = row.material
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(start = Dimens.CardPadding, top = Dimens.ItemSpacing, bottom = Dimens.ItemSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(m.itemName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val detail = listOfNotNull(
                    m.brand.takeIf { it.isNotBlank() },
                    m.quantityMilli?.let { q ->
                        m.ratePaise?.let { r ->
                            stringResource(R.string.material_qty_rate, Quantity.format(q), unitLabel(m.unit, m.customUnit), Money.format(r))
                        }
                    },
                ).joinToString(stringResource(R.string.list_separator))
                if (detail.isNotBlank()) {
                    Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                Text(
                    listOfNotNull(DateFormats.format(m.purchaseDate), m.shopName.takeIf { it.isNotBlank() })
                        .joinToString(stringResource(R.string.list_separator)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(Money.format(m.totalPaise), style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (row.photoCount > 0) {
                        Icon(
                            painterResource(R.drawable.ic_photo),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(row.photoCount.toString(), style = MaterialTheme.typography.labelSmall)
                    }
                    if (!m.isPaid) {
                        Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.small) {
                            Text(
                                stringResource(R.string.material_unpaid),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }
            OverflowMenu(menu)
        }
    }
}

/** Photos tab: grid of every bill and item photo of the work. */
@Composable
fun WorkPhotosTab(photos: List<WorkPhoto>, onOpen: (attachmentId: Long) -> Unit) {
    if (photos.isEmpty()) {
        EmptyState(
            icon = R.drawable.ic_photo,
            title = stringResource(R.string.photos_empty_title),
            message = stringResource(R.string.photos_empty_message),
        )
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 110.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
        verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
    ) {
        items(photos, key = { it.attachment.id }) { photo ->
            AsyncImage(
                model = photo.file,
                contentDescription = photo.attachment.caption.ifBlank { stringResource(R.string.photo_open) },
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .clickable { onOpen(photo.attachment.id) },
            )
        }
    }
}
