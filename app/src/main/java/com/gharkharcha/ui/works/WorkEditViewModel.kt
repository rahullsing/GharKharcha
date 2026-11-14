package com.gharkharcha.ui.works

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gharkharcha.data.entity.WorkEntity
import com.gharkharcha.data.entity.WorkStatus
import com.gharkharcha.data.entity.WorkType
import com.gharkharcha.data.repo.WorkRepository
import com.gharkharcha.ui.common.SavedForm
import com.gharkharcha.ui.navigation.WorkEditRoute
import com.gharkharcha.util.Money
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.LocalDate
import javax.inject.Inject

@Serializable
data class WorkForm(
    val name: String = "",
    val type: WorkType = WorkType.CUSTOM,
    val budget: String = "",
    val startDay: Long? = null,
    val endDay: Long? = null,
    val status: WorkStatus = WorkStatus.IN_PROGRESS,
    val notes: String = "",
) {
    val startDate: LocalDate? get() = startDay?.let(LocalDate::ofEpochDay)
    val endDate: LocalDate? get() = endDay?.let(LocalDate::ofEpochDay)
}

data class WorkFormErrors(
    val nameMissing: Boolean = false,
    val budgetInvalid: Boolean = false,
    val endBeforeStart: Boolean = false,
) {
    val any get() = nameMissing || budgetInvalid || endBeforeStart
}

data class WorkEditUi(
    val form: WorkForm,
    val isNew: Boolean,
    val loading: Boolean,
    val isDirty: Boolean,
    val errors: WorkFormErrors,
    val saving: Boolean,
    val saved: Boolean,
    val saveFailed: Boolean,
)

@HiltViewModel
class WorkEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val works: WorkRepository,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<WorkEditRoute>()
    private val isNew = route.workId == 0L

    private val form = SavedForm(savedStateHandle, "work_form", WorkForm.serializer()) {
        WorkForm(startDay = if (isNew) LocalDate.now().toEpochDay() else null)
    }
    private val baseline = SavedForm(savedStateHandle, "work_baseline", WorkForm.serializer()) { form.value }

    /** Existing entity (to keep createdAt etc. on update). */
    private var existing: WorkEntity? = null

    private data class Flags(
        val loading: Boolean = false,
        val showErrors: Boolean = false,
        val saving: Boolean = false,
        val saved: Boolean = false,
        val saveFailed: Boolean = false,
    )

    private val flags = MutableStateFlow(Flags(loading = !isNew))

    val state: StateFlow<WorkEditUi> = combine(form.state, baseline.state, flags) { f, base, fl ->
        WorkEditUi(
            form = f,
            isNew = isNew,
            loading = fl.loading,
            isDirty = f != base,
            errors = if (fl.showErrors) validate(f) else WorkFormErrors(),
            saving = fl.saving,
            saved = fl.saved,
            saveFailed = fl.saveFailed,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialUi())

    init {
        if (!isNew) {
            viewModelScope.launch {
                val work = works.get(route.workId)
                existing = work
                if (work != null && !form.restored) {
                    val loaded = WorkForm(
                        name = work.name,
                        type = work.type,
                        budget = Money.toInput(work.budgetPaise),
                        startDay = work.startDate?.toEpochDay(),
                        endDay = work.endDate?.toEpochDay(),
                        status = work.status,
                        notes = work.notes,
                    )
                    form.update { loaded }
                    baseline.update { loaded }
                }
                flags.update { it.copy(loading = false) }
            }
        }
    }

    private fun initialUi() = WorkEditUi(form.value, isNew, !isNew, false, WorkFormErrors(), false, false, false)

    fun onName(value: String) = form.update { it.copy(name = value) }

    /** Picks a type and swaps in its suggested name if the user hasn't typed their own. */
    fun onType(type: WorkType, newDefaultName: String?, oldDefaultName: String?) = form.update {
        val replaceName = it.name.isBlank() || it.name == oldDefaultName
        it.copy(type = type, name = if (replaceName && newDefaultName != null) newDefaultName else it.name)
    }

    fun onBudget(value: String) = form.update { it.copy(budget = value) }
    fun onStart(date: LocalDate?) = form.update { it.copy(startDay = date?.toEpochDay()) }
    fun onEnd(date: LocalDate?) = form.update { it.copy(endDay = date?.toEpochDay()) }
    fun onStatus(status: WorkStatus) = form.update { it.copy(status = status) }
    fun onNotes(value: String) = form.update { it.copy(notes = value) }

    fun save() {
        val f = form.value
        if (validate(f).any) {
            flags.update { it.copy(showErrors = true) }
            return
        }
        flags.update { it.copy(saving = true, saveFailed = false) }
        viewModelScope.launch {
            try {
                val base = existing ?: WorkEntity(projectId = route.projectId, name = "")
                works.save(
                    base.copy(
                        name = f.name.trim(),
                        type = f.type,
                        budgetPaise = Money.parse(f.budget) ?: 0,
                        startDate = f.startDate,
                        endDate = f.endDate,
                        status = f.status,
                        notes = f.notes.trim(),
                    ),
                )
                flags.update { it.copy(saving = false, saved = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                flags.update { it.copy(saving = false, saveFailed = true) }
            }
        }
    }

    private fun validate(f: WorkForm) = WorkFormErrors(
        nameMissing = f.name.isBlank(),
        budgetInvalid = f.budget.isNotBlank() && (Money.parse(f.budget)?.let { it < 0 } ?: true),
        endBeforeStart = f.startDay != null && f.endDay != null && f.endDay < f.startDay,
    )
}
