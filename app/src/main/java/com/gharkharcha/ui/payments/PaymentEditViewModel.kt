package com.gharkharcha.ui.payments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gharkharcha.R
import com.gharkharcha.data.entity.LabourerEntity
import com.gharkharcha.data.entity.PaymentEntity
import com.gharkharcha.data.entity.PaymentMode
import com.gharkharcha.data.entity.PaymentType
import com.gharkharcha.data.entity.WorkEntity
import com.gharkharcha.data.repo.LabourerRepository
import com.gharkharcha.data.repo.PaymentRepository
import com.gharkharcha.data.repo.WorkRepository
import com.gharkharcha.domain.usecase.CurrentProjectUseCase
import com.gharkharcha.ui.common.SavedForm
import com.gharkharcha.ui.common.UiText
import com.gharkharcha.ui.navigation.PaymentEditRoute
import com.gharkharcha.util.Money
import com.gharkharcha.util.UndoDeleteManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.LocalDate
import javax.inject.Inject

@Serializable
data class PaymentForm(
    val amount: String = "",
    val type: PaymentType = PaymentType.WAGE,
    val mode: PaymentMode = PaymentMode.CASH,
    val day: Long = LocalDate.now().toEpochDay(),
    val workId: Long? = null,
    val reference: String = "",
    val notes: String = "",
) {
    val date: LocalDate get() = LocalDate.ofEpochDay(day)
}

data class PaymentEditUi(
    val form: PaymentForm,
    val labourer: LabourerEntity?,
    val works: List<WorkEntity>,
    val isNew: Boolean,
    val loading: Boolean,
    val isDirty: Boolean,
    val amountError: Boolean,
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
    labourers: LabourerRepository,
    private val currentProject: CurrentProjectUseCase,
    private val undo: UndoDeleteManager,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<PaymentEditRoute>()
    private val isNew = route.paymentId == 0L

    private val form = SavedForm(savedStateHandle, "payment_form", PaymentForm.serializer()) {
        PaymentForm(amount = if (route.prefillPaise > 0) Money.toInput(route.prefillPaise) else "")
    }
    private val baseline = SavedForm(savedStateHandle, "payment_baseline", PaymentForm.serializer()) { form.value }
    private var existing: PaymentEntity? = null

    private data class Flags(
        val loading: Boolean,
        val showErrors: Boolean = false,
        val saving: Boolean = false,
        val done: Boolean = false,
        val saveFailed: Boolean = false,
    )

    private val flags = MutableStateFlow(Flags(loading = !isNew))

    private val projectWorks = currentProject.state.flatMapLatest { s ->
        s.current?.let { works.observeByProject(it.id) } ?: flowOf(emptyList())
    }

    val state: StateFlow<PaymentEditUi> = combine(
        form.state,
        baseline.state,
        flags,
        labourers.observe(route.labourerId),
        projectWorks,
    ) { f, base, fl, labourer, workList ->
        PaymentEditUi(
            form = f,
            labourer = labourer,
            works = workList,
            isNew = isNew,
            loading = fl.loading,
            isDirty = f != base,
            amountError = fl.showErrors && amountInvalid(f),
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
                    val loaded = PaymentForm(
                        amount = Money.toInput(p.amountPaise),
                        type = p.type,
                        mode = p.mode,
                        day = p.date.toEpochDay(),
                        workId = p.workId,
                        reference = p.reference,
                        notes = p.notes,
                    )
                    form.update { loaded }
                    baseline.update { loaded }
                }
                flags.update { it.copy(loading = false) }
            }
        }
    }

    private fun initialUi() = PaymentEditUi(
        form.value, null, emptyList(), isNew, !isNew, false, false, false, false, false,
    )

    fun onAmount(v: String) = form.update { it.copy(amount = v) }
    fun onType(v: PaymentType) = form.update { it.copy(type = v) }
    fun onMode(v: PaymentMode) = form.update { it.copy(mode = v) }
    fun onDate(v: LocalDate?) {
        if (v != null) form.update { it.copy(day = v.toEpochDay()) }
    }
    fun onWork(v: WorkEntity?) = form.update { it.copy(workId = v?.id) }
    fun onReference(v: String) = form.update { it.copy(reference = v) }
    fun onNotes(v: String) = form.update { it.copy(notes = v) }

    fun save() {
        val f = form.value
        if (amountInvalid(f)) {
            flags.update { it.copy(showErrors = true) }
            return
        }
        if (flags.value.saving) return // ignore double taps
        flags.update { it.copy(saving = true, saveFailed = false) }
        viewModelScope.launch {
            try {
                val work = f.workId?.let { works.get(it) }
                val projectId = work?.projectId ?: existing?.projectId ?: currentProject.state.first().current?.id
                val base = existing ?: PaymentEntity(labourerId = route.labourerId, date = f.date, amountPaise = 0)
                payments.save(
                    base.copy(
                        projectId = projectId,
                        workId = work?.id,
                        date = f.date,
                        amountPaise = Money.parse(f.amount) ?: 0,
                        type = f.type,
                        mode = f.mode,
                        reference = f.reference.trim(),
                        notes = f.notes.trim(),
                    ),
                )
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
