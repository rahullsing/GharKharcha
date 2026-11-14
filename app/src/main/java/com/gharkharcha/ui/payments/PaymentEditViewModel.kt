package com.gharkharcha.ui.payments

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gharkharcha.R
import com.gharkharcha.data.entity.AttachmentKind
import com.gharkharcha.data.entity.LabourerEntity
import com.gharkharcha.data.entity.OwnerType
import com.gharkharcha.data.entity.PaymentEntity
import com.gharkharcha.data.entity.PaymentMode
import com.gharkharcha.data.entity.PaymentType
import com.gharkharcha.data.entity.WorkEntity
import com.gharkharcha.data.files.ImageImporter
import com.gharkharcha.data.files.SharedPhotoHolder
import com.gharkharcha.data.repo.AttachmentRepository
import com.gharkharcha.data.repo.LabourerRepository
import com.gharkharcha.data.repo.PaymentRepository
import com.gharkharcha.data.repo.WorkRepository
import com.gharkharcha.domain.model.PhotoItem
import com.gharkharcha.domain.usecase.CurrentProjectUseCase
import com.gharkharcha.ui.common.SavedForm
import com.gharkharcha.ui.common.UiText
import com.gharkharcha.ui.navigation.PaymentEditRoute
import com.gharkharcha.util.Money
import com.gharkharcha.util.UndoDeleteManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.LocalDate
import javax.inject.Inject

@Serializable
data class PaymentForm(
    val labourerId: Long = 0,
    val amount: String = "",
    val type: PaymentType = PaymentType.WAGE,
    val mode: PaymentMode = PaymentMode.CASH,
    val day: Long = LocalDate.now().toEpochDay(),
    val workId: Long? = null,
    val reference: String = "",
    val periodFromDay: Long? = null,
    val periodToDay: Long? = null,
    val notes: String = "",
    val photos: List<PhotoItem> = emptyList(),
    val removedPhotoIds: List<Long> = emptyList(),
) {
    val date: LocalDate get() = LocalDate.ofEpochDay(day)
    val periodFrom: LocalDate? get() = periodFromDay?.let(LocalDate::ofEpochDay)
    val periodTo: LocalDate? get() = periodToDay?.let(LocalDate::ofEpochDay)
}

data class PaymentEditUi(
    val form: PaymentForm,
    val labourer: LabourerEntity?,
    val activeLabourers: List<LabourerEntity>,
    val works: List<WorkEntity>,
    val isNew: Boolean,
    val loading: Boolean,
    val isDirty: Boolean,
    val amountError: Boolean,
    val labourerError: Boolean,
    val importing: Boolean,
    val saving: Boolean,
    val done: Boolean,
    val saveFailed: Boolean,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PaymentEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val payments: PaymentRepository,
    private val works: WorkRepository,
    private val labourers: LabourerRepository,
    private val currentProject: CurrentProjectUseCase,
    private val undo: UndoDeleteManager,
    private val attachments: AttachmentRepository,
    private val importer: ImageImporter,
    sharedPhotoHolder: SharedPhotoHolder,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<PaymentEditRoute>()
    private val isNew = route.paymentId == 0L

    private val form = SavedForm(savedStateHandle, "payment_form", PaymentForm.serializer()) {
        PaymentForm(
            labourerId = route.labourerId,
            workId = if (route.workId != 0L) route.workId else null,
            amount = if (route.prefillPaise > 0) Money.toInput(route.prefillPaise) else "",
        )
    }
    private val baseline = SavedForm(savedStateHandle, "payment_baseline", PaymentForm.serializer()) { form.value }
    private var existing: PaymentEntity? = null

    private data class Flags(
        val loading: Boolean,
        val showErrors: Boolean = false,
        val saving: Boolean = false,
        val done: Boolean = false,
        val saveFailed: Boolean = false,
        val importing: Boolean = false,
    )

    private val flags = MutableStateFlow(Flags(loading = !isNew))

    private val messageChannel = Channel<UiText>(Channel.BUFFERED)
    val messages: Flow<UiText> = messageChannel.receiveAsFlow()

    private val projectWorks = currentProject.state.flatMapLatest { s ->
        s.current?.let { works.observeByProject(it.id) } ?: flowOf(emptyList())
    }

    private val allLabourers = labourers.observeActive()

    val state: StateFlow<PaymentEditUi> = combine(
        form.state,
        baseline.state,
        flags,
        allLabourers,
        projectWorks,
    ) { f, base, fl, people, workList ->
        val chosenLabourer = people.firstOrNull { it.id == f.labourerId }
            ?: (if (route.labourerId != 0L) labourers.get(route.labourerId) else null)
        PaymentEditUi(
            form = f,
            labourer = chosenLabourer,
            activeLabourers = people,
            works = workList,
            isNew = isNew,
            loading = fl.loading,
            isDirty = f != base,
            amountError = fl.showErrors && amountInvalid(f),
            labourerError = fl.showErrors && f.labourerId == 0L,
            importing = fl.importing,
            saving = fl.saving,
            done = fl.done,
            saveFailed = fl.saveFailed,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialUi())

    init {
        if (!isNew) {
            viewModelScope.launch {
                val p = payments.get(route.paymentId)
                existing = p
                if (p != null && !form.restored) {
                    val photoItems = attachments.loadItems(OwnerType.PAYMENT, p.id)
                    val loaded = PaymentForm(
                        labourerId = p.labourerId,
                        amount = Money.toInput(p.amountPaise),
                        type = p.type,
                        mode = p.mode,
                        day = p.date.toEpochDay(),
                        workId = p.workId,
                        reference = p.reference,
                        periodFromDay = p.periodFrom?.toEpochDay(),
                        periodToDay = p.periodTo?.toEpochDay(),
                        notes = p.notes,
                        photos = photoItems,
                    )
                    form.update { loaded }
                    baseline.update { loaded }
                }
                flags.update { it.copy(loading = false) }
            }
        } else {
            // If photos were passed from an incoming share intent, pre-populate them.
            val shared = sharedPhotoHolder.consume()
            if (shared.isNotEmpty()) {
                form.update { it.copy(photos = it.photos + shared) }
            }
        }
    }

    private fun initialUi() = PaymentEditUi(
        form = form.value,
        labourer = null,
        activeLabourers = emptyList(),
        works = emptyList(),
        isNew = isNew,
        loading = !isNew,
        isDirty = false,
        amountError = false,
        labourerError = false,
        importing = false,
        saving = false,
        done = false,
        saveFailed = false,
    )

    fun onLabourer(v: LabourerEntity?) = form.update { it.copy(labourerId = v?.id ?: 0) }
    fun onAmount(v: String) = form.update { it.copy(amount = v) }
    fun onType(v: PaymentType) = form.update { it.copy(type = v) }
    fun onMode(v: PaymentMode) = form.update { it.copy(mode = v) }
    fun onDate(v: LocalDate?) {
        if (v != null) form.update { it.copy(day = v.toEpochDay()) }
    }
    fun onWork(v: WorkEntity?) = form.update { it.copy(workId = v?.id) }
    fun onReference(v: String) = form.update { it.copy(reference = v) }
    fun onNotes(v: String) = form.update { it.copy(notes = v) }

    fun onPeriod(from: LocalDate?, to: LocalDate?) = form.update {
        it.copy(
            periodFromDay = from?.toEpochDay(),
            periodToDay = to?.toEpochDay(),
        )
    }

    fun clearPeriod() = form.update { it.copy(periodFromDay = null, periodToDay = null) }

    // --- Photos ----------------------------------------------------------------------------------

    fun onCaptured(path: String) {
        importPhotos { importer.importCapture(path, AttachmentKind.PHOTO)?.let { listOf(it) to 0 } ?: (emptyList<PhotoItem>() to 1) }
    }

    fun onPicked(uris: List<Uri>) = importPhotos { importer.importAll(uris, AttachmentKind.PHOTO) }

    private fun importPhotos(block: suspend () -> Pair<List<PhotoItem>, Int>) {
        flags.update { it.copy(importing = true) }
        viewModelScope.launch {
            try {
                val (items, failed) = block()
                form.update { it.copy(photos = it.photos + items) }
                if (failed > 0) messageChannel.send(UiText.of(R.string.photo_import_failed))
            } finally {
                flags.update { it.copy(importing = false) }
            }
        }
    }

    fun removePhoto(photo: PhotoItem) {
        form.update {
            it.copy(
                photos = it.photos - photo,
                removedPhotoIds = photo.attachmentId?.let { id -> it.removedPhotoIds + id } ?: it.removedPhotoIds,
            )
        }
        viewModelScope.launch { attachments.discardPending(photo) }
    }

    fun onPhotoCaption(photo: PhotoItem, caption: String) {
        form.update {
            it.copy(photos = it.photos.map { p -> if (p.key == photo.key) p.copy(caption = caption.trim()) else p })
        }
    }

    fun onReorderPhotos(from: Int, to: Int) {
        form.update {
            val list = it.photos.toMutableList()
            val item = list.removeAt(from)
            list.add(to, item)
            it.copy(photos = list)
        }
    }

    // --- Save & Delete ---------------------------------------------------------------------------

    fun save() {
        val f = form.value
        if (amountInvalid(f) || f.labourerId == 0L) {
            flags.update { it.copy(showErrors = true) }
            return
        }
        if (flags.value.saving) return // ignore double taps
        flags.update { it.copy(saving = true, saveFailed = false) }
        viewModelScope.launch {
            try {
                val work = f.workId?.let { works.get(it) }
                val projectId = work?.projectId ?: existing?.projectId ?: currentProject.state.first().current?.id
                val base = existing ?: PaymentEntity(labourerId = f.labourerId, date = f.date, amountPaise = 0)
                val entity = base.copy(
                    labourerId = f.labourerId,
                    projectId = projectId,
                    workId = work?.id,
                    date = f.date,
                    amountPaise = Money.parse(f.amount) ?: 0,
                    type = f.type,
                    mode = f.mode,
                    reference = f.reference.trim(),
                    periodFrom = f.periodFrom,
                    periodTo = f.periodTo,
                    notes = f.notes.trim(),
                )
                payments.saveWithPhotos(entity, f.photos, f.removedPhotoIds)
                // Stay "saving" so the button can't be pressed again before the screen closes.
                flags.update { it.copy(done = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                flags.update { it.copy(saving = false, saveFailed = true) }
            }
        }
    }

    fun delete() {
        val p = existing ?: return
        undo.schedule(UndoDeleteManager.paymentKey(p.id), UiText.of(R.string.payment_deleted)) { payments.delete(p) }
        flags.update { it.copy(done = true) }
    }

    private fun amountInvalid(f: PaymentForm) = (Money.parse(f.amount) ?: 0) <= 0
}
