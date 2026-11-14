package com.gharkharcha.ui.works

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gharkharcha.R
import com.gharkharcha.data.entity.WorkEntity
import com.gharkharcha.data.entity.LabourerEntity
import com.gharkharcha.data.entity.PaymentEntity
import com.gharkharcha.data.repo.LabourerRepository
import com.gharkharcha.data.repo.LedgerRepository
import com.gharkharcha.data.repo.PaymentRepository
import com.gharkharcha.data.repo.WorkRepository
import com.gharkharcha.domain.model.LabourSummary
import kotlinx.coroutines.flow.combine
import com.gharkharcha.data.entity.AttachmentEntity
import com.gharkharcha.data.entity.MaterialEntity
import com.gharkharcha.data.repo.AttachmentRepository
import com.gharkharcha.data.repo.MaterialRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import com.gharkharcha.domain.model.WorkWithSpend
import com.gharkharcha.ui.common.LoadState
import com.gharkharcha.ui.common.UiText
import com.gharkharcha.ui.common.retryableLoad
import com.gharkharcha.ui.navigation.WorkDetailRoute
import com.gharkharcha.util.UndoDeleteManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** One labourer's totals on this work. */
data class WorkLabourRow(val labourer: LabourerEntity, val summary: LabourSummary)

data class WorkPaymentRow(val payment: PaymentEntity, val labourerName: String, val photoCount: Int = 0)

data class WorkMaterialRow(val material: MaterialEntity, val photoCount: Int)

data class WorkPhoto(val attachment: AttachmentEntity, val file: File)

@HiltViewModel
class WorkDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val works: WorkRepository,
    private val undo: UndoDeleteManager,
    labourers: LabourerRepository,
    ledger: LedgerRepository,
    private val paymentRepo: PaymentRepository,
    private val materials: MaterialRepository,
    attachments: AttachmentRepository,
) : ViewModel() {
    private val workId = savedStateHandle.toRoute<WorkDetailRoute>().workId
    private val retry = MutableStateFlow(0)

    /** Ready(null) means the work doesn't exist (deleted elsewhere). */
    val state: StateFlow<LoadState<WorkWithSpend?>> = retryableLoad(retry) { works.observeOneWithSpend(workId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoadState.Loading)

    /** Labourers who worked on this work, most earned first. */
    val labour: StateFlow<List<WorkLabourRow>> =
        combine(labourers.observeAll(), ledger.observeSummaries(workId = workId)) { people, summaries ->
            people.mapNotNull { p -> summaries[p.id]?.let { WorkLabourRow(p, it) } }
                .filter { it.summary.earnedPaise > 0 || it.summary.dayHundredths > 0 }
                .sortedByDescending { it.summary.earnedPaise }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val payments: StateFlow<List<WorkPaymentRow>> = combine(
        paymentRepo.observeForWork(workId),
        labourers.observeAll(),
        attachments.observePaymentPhotoCounts(workId),
        undo.pending,
    ) { list, people, photoCounts, pending ->
        val names = people.associate { it.id to it.name }
        list.filter { UndoDeleteManager.paymentKey(it.id) !in pending }
            .map { WorkPaymentRow(it, names[it.labourerId].orEmpty(), photoCounts[it.id] ?: 0) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val materialRows: StateFlow<List<WorkMaterialRow>> = combine(
        materials.observeByWork(workId),
        attachments.observeMaterialPhotoCounts(workId),
        undo.pending,
    ) { list, counts, pending ->
        list.filter { UndoDeleteManager.materialKey(it.id) !in pending }
            .map { WorkMaterialRow(it, counts[it.id] ?: 0) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val photos: StateFlow<List<WorkPhoto>> = attachments.observeForWorkTree(workId)
        .map { list -> list.map { WorkPhoto(it, attachments.fileOf(it)) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val messageChannel = Channel<UiText>(Channel.BUFFERED)
    val messages: Flow<UiText> = messageChannel.receiveAsFlow()

    fun duplicateMaterial(material: MaterialEntity) {
        viewModelScope.launch {
            materials.duplicate(material, LocalDate.now())
            messageChannel.send(UiText.of(R.string.material_duplicated))
        }
    }

    fun deleteMaterial(material: MaterialEntity) {
        undo.schedule(UndoDeleteManager.materialKey(material.id), UiText.of(R.string.material_deleted, material.itemName)) {
            materials.delete(material)
        }
    }

    fun deletePayment(payment: PaymentEntity) {
        undo.schedule(UndoDeleteManager.paymentKey(payment.id), UiText.of(R.string.payment_deleted)) {
            paymentRepo.delete(payment)
        }
    }

    fun retry() = retry.update { it + 1 }

    fun delete(work: WorkEntity) {
        undo.schedule(UndoDeleteManager.workKey(work.id), UiText.of(R.string.work_deleted, work.name)) {
            works.delete(work)
        }
    }
}
