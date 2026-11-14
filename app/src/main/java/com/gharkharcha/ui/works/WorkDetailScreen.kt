package com.gharkharcha.ui.works

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gharkharcha.R
import com.gharkharcha.data.entity.WorkEntity
import com.gharkharcha.domain.model.WorkWithSpend
import com.gharkharcha.ui.common.LoadState
import com.gharkharcha.ui.common.labelRes
import com.gharkharcha.ui.components.BackTopBar
import com.gharkharcha.ui.components.CollectMessages
import com.gharkharcha.data.entity.MaterialEntity
import com.gharkharcha.ui.components.ConfirmDialog
import com.gharkharcha.ui.components.DetailScaffold
import com.gharkharcha.ui.components.EmptyState
import com.gharkharcha.ui.components.LabelValueRow
import com.gharkharcha.ui.components.LoadStateContent
import com.gharkharcha.ui.components.MenuAction
import com.gharkharcha.ui.components.OverflowMenu
import com.gharkharcha.ui.components.SpendOverviewCard
import com.gharkharcha.ui.theme.Dimens
import com.gharkharcha.util.DateFormats

private enum class WorkTab(@param:StringRes val title: Int) {
    SUMMARY(R.string.tab_summary),
    MATERIALS(R.string.tab_materials),
    LABOUR(R.string.tab_labour),
    PAYMENTS(R.string.tab_payments),
    PHOTOS(R.string.tab_photos),
}

@Composable
fun WorkDetailScreen(
    onBack: () -> Unit,
    onEdit: (WorkEntity) -> Unit,
    onMarkAttendance: (workId: Long) -> Unit,
    onOpenLabourer: (Long) -> Unit,
    onOpenPayment: (labourerId: Long, paymentId: Long) -> Unit,
    onAddPayment: (workId: Long) -> Unit = {},
    onAddMaterial: (workId: Long) -> Unit,
    onOpenMaterial: (MaterialEntity) -> Unit,
    onOpenPhoto: (workId: Long, attachmentId: Long) -> Unit,
    viewModel: WorkDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val materials by viewModel.materialRows.collectAsStateWithLifecycle()
    val photos by viewModel.photos.collectAsStateWithLifecycle()
    CollectMessages(viewModel.messages)
    val labour by viewModel.labour.collectAsStateWithLifecycle()
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val item = (state as? LoadState.Ready)?.data
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    DetailScaffold(
        topBar = {
            BackTopBar(
                title = item?.work?.name.orEmpty(),
                onBack = onBack,
                actions = {
                    if (item != null) {
                        IconButton(onClick = { onEdit(item.work) }) {
                            Icon(painterResource(R.drawable.ic_edit), contentDescription = stringResource(R.string.action_edit))
                        }
                        OverflowMenu(listOf(MenuAction(stringResource(R.string.action_delete)) { confirmDelete = true }))
                    }
                },
            )
        },
    ) { padding ->
        LoadStateContent(state, onRetry = viewModel::retry, modifier = Modifier.padding(padding)) { data ->
            if (data == null) {
                EmptyState(
                    icon = R.drawable.ic_error,
                    title = stringResource(R.string.state_not_found),
                    message = null,
                    actionLabel = stringResource(R.string.action_back),
                    onAction = onBack,
                    modifier = Modifier.padding(padding),
                )
                return@LoadStateContent
            }
            Column(Modifier.fillMaxSize().padding(padding)) {
                PrimaryScrollableTabRow(selectedTabIndex = tab, edgePadding = Dimens.ScreenPadding) {
                    WorkTab.entries.forEachIndexed { index, t ->
                        Tab(
                            selected = tab == index,
                            onClick = { tab = index },
                            text = { Text(stringResource(t.title)) },
                        )
                    }
                }
                when (WorkTab.entries[tab]) {
                    WorkTab.SUMMARY -> SummaryTab(data)
                    WorkTab.MATERIALS -> WorkMaterialsTab(
                        rows = materials,
                        onAdd = { onAddMaterial(data.work.id) },
                        onOpen = onOpenMaterial,
                        onDuplicate = viewModel::duplicateMaterial,
                        onDelete = viewModel::deleteMaterial,
                    )
                    WorkTab.LABOUR -> WorkLabourTab(
                        totalPaise = data.totals.labourPaise,
                        rows = labour,
                        onMarkAttendance = { onMarkAttendance(data.work.id) },
                        onOpenLabourer = onOpenLabourer,
                    )
                    WorkTab.PAYMENTS -> WorkPaymentsTab(
                        rows = payments,
                        onAddPayment = { onAddPayment(data.work.id) },
                        onOpenPayment = onOpenPayment,
                        onDeletePayment = viewModel::deletePayment,
                    )
                    WorkTab.PHOTOS -> WorkPhotosTab(photos) { onOpenPhoto(data.work.id, it) }
                }
            }
        }
    }

    if (confirmDelete && item != null) {
        ConfirmDialog(
            title = stringResource(R.string.work_delete_title),
            message = stringResource(R.string.work_delete_message, item.work.name),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                confirmDelete = false
                viewModel.delete(item.work)
                onBack()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun SummaryTab(item: WorkWithSpend) {
    val work = item.work
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item { SpendOverviewCard(item.totals, Modifier.widthIn(max = 600.dp)) }
        item {
            Card(Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                Column(
                    Modifier.padding(Dimens.CardPadding),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.work_details),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        WorkStatusChip(work.status)
                    }
                    LabelValueRow(stringResource(R.string.work_type), stringResource(work.type.labelRes))
                    LabelValueRow(
                        stringResource(R.string.work_start_date),
                        work.startDate?.let(DateFormats::format) ?: stringResource(R.string.no_date),
                    )
                    LabelValueRow(
                        stringResource(R.string.work_end_date),
                        work.endDate?.let(DateFormats::format) ?: stringResource(R.string.no_date),
                    )
                    if (work.notes.isNotBlank()) {
                        Text(
                            stringResource(R.string.notes),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = Dimens.SmallSpacing),
                        )
                        Text(work.notes, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
