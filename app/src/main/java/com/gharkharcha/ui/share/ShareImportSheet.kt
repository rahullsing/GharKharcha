package com.gharkharcha.ui.share

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.gharkharcha.R
import com.gharkharcha.data.entity.AttachmentKind
import com.gharkharcha.data.entity.LabourerEntity
import com.gharkharcha.data.entity.WorkEntity
import com.gharkharcha.data.files.ImageImporter
import com.gharkharcha.data.files.SharedPhotoHolder
import com.gharkharcha.data.repo.LabourerRepository
import com.gharkharcha.data.repo.WorkRepository
import com.gharkharcha.domain.usecase.CurrentProjectUseCase
import com.gharkharcha.ui.components.DropdownField
import com.gharkharcha.ui.theme.Dimens
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ShareImportViewModel @Inject constructor(
    private val importer: ImageImporter,
    private val sharedPhotoHolder: SharedPhotoHolder,
    labourers: LabourerRepository,
    works: WorkRepository,
    currentProject: CurrentProjectUseCase,
) : ViewModel() {
    val activeLabourers: StateFlow<List<LabourerEntity>> = labourers.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val projectWorks: StateFlow<List<WorkEntity>> = currentProject.state.flatMapLatest { s ->
        s.current?.let { works.observeByProject(it.id) } ?: flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _importing = MutableStateFlow(false)
    val importing: StateFlow<Boolean> = _importing.asStateFlow()

    fun attachToPayment(
        uris: List<Uri>,
        labourerId: Long,
        workId: Long,
        onDone: (labourerId: Long, workId: Long) -> Unit,
    ) {
        if (_importing.value) return
        _importing.value = true
        viewModelScope.launch {
            try {
                val (items, _) = importer.importAll(uris, AttachmentKind.PHOTO)
                sharedPhotoHolder.set(items)
                onDone(labourerId, workId)
            } finally {
                _importing.value = false
            }
        }
    }

    fun attachToMaterial(
        uris: List<Uri>,
        workId: Long,
        onDone: (workId: Long) -> Unit,
    ) {
        if (_importing.value) return
        _importing.value = true
        viewModelScope.launch {
            try {
                val (items, _) = importer.importAll(uris, AttachmentKind.PHOTO)
                sharedPhotoHolder.set(items)
                onDone(workId)
            } finally {
                _importing.value = false
            }
        }
    }
}

private enum class ShareMode { CHOOSING, SELECT_LABOURER, SELECT_WORK }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareImportSheet(
    uris: List<Uri>,
    onDismiss: () -> Unit,
    onNavigateToPayment: (labourerId: Long, workId: Long) -> Unit,
    onNavigateToMaterial: (workId: Long) -> Unit,
    viewModel: ShareImportViewModel = hiltViewModel(),
) {
    val importing by viewModel.importing.collectAsStateWithLifecycle()
    val labourers by viewModel.activeLabourers.collectAsStateWithLifecycle()
    val works by viewModel.projectWorks.collectAsStateWithLifecycle()
    var mode by rememberSaveable { mutableStateOf(ShareMode.CHOOSING) }

    ModalBottomSheet(
        onDismissRequest = { if (!importing) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Dimens.ScreenPadding)
                .padding(bottom = Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        ) {
            // Header
            val title = if (uris.size > 1) {
                stringResource(R.string.share_title_multiple, uris.size)
            } else {
                stringResource(R.string.share_title)
            }
            Text(title, style = MaterialTheme.typography.titleLarge)

            // Thumbnails preview
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(uris) { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    )
                }
            }

            if (importing) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            when (mode) {
                ShareMode.CHOOSING -> {
                    // Option 1: Attach to Payment
                    OutlinedCard(
                        onClick = {
                            if (labourers.size <= 1) {
                                viewModel.attachToPayment(
                                    uris = uris,
                                    labourerId = labourers.firstOrNull()?.id ?: 0L,
                                    workId = 0L,
                                    onDone = onNavigateToPayment,
                                )
                            } else {
                                mode = ShareMode.SELECT_LABOURER
                            }
                        },
                        enabled = !importing,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.share_attach_payment)) },
                            supportingContent = { Text(stringResource(R.string.share_attach_payment_desc)) },
                            leadingContent = {
                                Icon(painterResource(R.drawable.ic_wallet), contentDescription = null)
                            },
                            trailingContent = {
                                Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null)
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
                    }

                    // Option 2: Attach to Material
                    OutlinedCard(
                        onClick = {
                            if (works.size == 1) {
                                viewModel.attachToMaterial(
                                    uris = uris,
                                    workId = works.first().id,
                                    onDone = onNavigateToMaterial,
                                )
                            } else {
                                mode = ShareMode.SELECT_WORK
                            }
                        },
                        enabled = !importing && works.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.share_attach_material)) },
                            supportingContent = {
                                Text(
                                    if (works.isEmpty()) {
                                        stringResource(R.string.share_no_works)
                                    } else {
                                        stringResource(R.string.share_attach_material_desc)
                                    },
                                )
                            },
                            leadingContent = {
                                Icon(painterResource(R.drawable.ic_build), contentDescription = null)
                            },
                            trailingContent = if (works.isNotEmpty()) {
                                { Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null) }
                            } else {
                                null
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
                    }
                }

                ShareMode.SELECT_LABOURER -> {
                    Text(
                        stringResource(R.string.share_select_labourer),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
                    ) {
                        item {
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.payment_select_labourer)) },
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.small)
                                    .clickable(enabled = !importing) {
                                        viewModel.attachToPayment(
                                            uris = uris,
                                            labourerId = 0L,
                                            workId = 0L,
                                            onDone = onNavigateToPayment,
                                        )
                                    },
                            )
                        }
                        items(labourers, key = { it.id }) { labourer ->
                            ListItem(
                                headlineContent = { Text(labourer.name) },
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.small)
                                    .clickable(enabled = !importing) {
                                        viewModel.attachToPayment(
                                            uris = uris,
                                            labourerId = labourer.id,
                                            workId = 0L,
                                            onDone = onNavigateToPayment,
                                        )
                                    },
                            )
                        }
                    }
                    TextButton(
                        onClick = { mode = ShareMode.CHOOSING },
                        enabled = !importing,
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text(stringResource(R.string.action_back))
                    }
                }

                ShareMode.SELECT_WORK -> {
                    Text(
                        stringResource(R.string.share_select_work),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (works.isEmpty()) {
                        Text(
                            stringResource(R.string.share_no_works),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
                        ) {
                            items(works, key = { it.id }) { work ->
                                ListItem(
                                    headlineContent = { Text(work.name) },
                                    modifier = Modifier
                                        .clip(MaterialTheme.shapes.small)
                                        .clickable(enabled = !importing) {
                                            viewModel.attachToMaterial(
                                                uris = uris,
                                                workId = work.id,
                                                onDone = onNavigateToMaterial,
                                            )
                                        },
                                )
                            }
                        }
                    }
                    TextButton(
                        onClick = { mode = ShareMode.CHOOSING },
                        enabled = !importing,
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text(stringResource(R.string.action_back))
                    }
                }
            }
        }
    }
}
