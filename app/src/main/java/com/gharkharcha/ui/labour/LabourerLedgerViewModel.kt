package com.gharkharcha.ui.labour

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gharkharcha.R
import com.gharkharcha.data.dao.AttendanceWithWork
import com.gharkharcha.data.entity.LabourerEntity
import com.gharkharcha.data.entity.PaymentEntity
import com.gharkharcha.data.prefs.UserPreferencesRepository
import com.gharkharcha.data.repo.AttendanceRepository
import com.gharkharcha.data.repo.LabourerRepository
import com.gharkharcha.data.entity.OwnerType
import com.gharkharcha.data.repo.AttachmentRepository
import com.gharkharcha.data.repo.PaymentRepository
import com.gharkharcha.domain.calc.LedgerCalculator
import com.gharkharcha.domain.model.LabourSummary
import com.gharkharcha.ui.common.LoadState
import com.gharkharcha.ui.common.UiText
import com.gharkharcha.ui.common.retryableLoad
import com.gharkharcha.ui.navigation.LabourerLedgerRoute
import com.gharkharcha.util.UndoDeleteManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** One line of the day-by-day table: a day of work, or money given. */
sealed interface LedgerLine {
    val date: LocalDate

    data class Day(val item: AttendanceWithWork) : LedgerLine {
        override val date: LocalDate get() = item.attendance.date
    }

    data class Payment(val payment: PaymentEntity, val photoCount: Int = 0) : LedgerLine {
        override val date: LocalDate get() = payment.date
    }
}

data class LedgerUi(
    val labourer: LabourerEntity,
    val lines: List<LedgerLine>,
    val summary: LabourSummary,
    val photoPath: String? = null,
)

/** Delete flow outcomes the screen reacts to. */
enum class LedgerDialog { NONE, CONFIRM_DELETE, HAS_HISTORY }

@HiltViewModel
class LabourerLedgerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val labourers: LabourerRepository,
    attendance: AttendanceRepository,
    payments: PaymentRepository,
    prefs: UserPreferencesRepository,
    private val undo: UndoDeleteManager,
    attachments: AttachmentRepository,
) : ViewModel() {
    private val labourerId = savedStateHandle.toRoute<LabourerLedgerRoute>().labourerId
    private val retry = MutableStateFlow(0)

    private val _dialog = MutableStateFlow(LedgerDialog.NONE)
    val dialog: StateFlow<LedgerDialog> = _dialog.asStateFlow()

    /** Set once the labourer has been scheduled for deletion; the screen then closes. */
    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    /** Ready(null) = labourer not found. */
    val state: StateFlow<LoadState<LedgerUi?>> = retryableLoad(retry) {
        val paymentsWithPhotos = combine(
            payments.observeForLabourer(labourerId),
            attachments.observeLabourerPaymentPhotoCounts(labourerId),
            undo.pending,
        ) { list, counts, pending ->
            list.filter { UndoDeleteManager.paymentKey(it.id) !in pending }
                .map { p -> p to (counts[p.id] ?: 0) }
        }

        combine(
            labourers.observe(labourerId),
            attendance.observeForLabourer(labourerId),
            paymentsWithPhotos,
            prefs.hoursPerDay,
            attachments.observeFor(OwnerType.LABOURER, labourerId),
        ) { labourer, days, paidWithPhotos, hpd, photos ->
            labourer ?: return@combine null
            val paid = paidWithPhotos.map { it.first }
            val lines = days.map { LedgerLine.Day(it) } + paidWithPhotos.map { LedgerLine.Payment(it.first, it.second) }
            LedgerUi(
                labourer = labourer,
                // Newest first; on the same day the work comes before the money.
                lines = lines.sortedWith(compareByDescending<LedgerLine> { it.date }.thenBy { if (it is LedgerLine.Day) 0 else 1 }),
                summary = LedgerCalculator.summarize(days.map { it.attendance }, paid, hpd),
                photoPath = photos.firstOrNull()?.let { attachments.fileOf(it).absolutePath },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoadState.Loading)

    fun retry() = retry.update { it + 1 }

    fun toggleActive() {
        val l = (state.value as? LoadState.Ready)?.data?.labourer ?: return
        viewModelScope.launch { labourers.setActive(l, !l.isActive) }
    }

    fun requestDelete() {
        val l = (state.value as? LoadState.Ready)?.data?.labourer ?: return
        viewModelScope.launch {
            _dialog.value = if (labourers.hasHistory(l.id)) LedgerDialog.HAS_HISTORY else LedgerDialog.CONFIRM_DELETE
        }
    }

    fun confirmDelete() {
        val l = (state.value as? LoadState.Ready)?.data?.labourer ?: return
        _dialog.value = LedgerDialog.NONE
        undo.schedule(UndoDeleteManager.labourerKey(l.id), UiText.of(R.string.labourer_deleted, l.name)) {
            labourers.delete(l)
        }
        _deleted.value = true
    }

    /** From the "can't delete" dialog: hide them from the attendance sheet instead. */
    fun deactivateInstead() {
        val l = (state.value as? LoadState.Ready)?.data?.labourer ?: return
        _dialog.value = LedgerDialog.NONE
        if (l.isActive) viewModelScope.launch { labourers.setActive(l, false) }
    }

    fun dismissDialog() {
        _dialog.value = LedgerDialog.NONE
    }
}
