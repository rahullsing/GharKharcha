package com.gharkharcha.ui.projects

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gharkharcha.data.entity.ProjectEntity
import com.gharkharcha.data.repo.ProjectRepository
import com.gharkharcha.domain.usecase.CurrentProjectUseCase
import com.gharkharcha.ui.common.SavedForm
import com.gharkharcha.ui.navigation.ProjectEditRoute
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
data class ProjectForm(
    val name: String = "",
    val address: String = "",
    val startDay: Long? = null,
    val budget: String = "",
    val notes: String = "",
) {
    val startDate: LocalDate? get() = startDay?.let(LocalDate::ofEpochDay)
}

data class ProjectEditUi(
    val form: ProjectForm,
    val isNew: Boolean,
    val loading: Boolean,
    val isDirty: Boolean,
    val nameMissing: Boolean,
    val budgetInvalid: Boolean,
    val saving: Boolean,
    val saved: Boolean,
    val saveFailed: Boolean,
)

@HiltViewModel
class ProjectEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val projects: ProjectRepository,
    private val currentProject: CurrentProjectUseCase,
) : ViewModel() {
    private val projectId = savedStateHandle.toRoute<ProjectEditRoute>().projectId
    private val isNew = projectId == 0L

    private val form = SavedForm(savedStateHandle, "project_form", ProjectForm.serializer()) {
        ProjectForm(startDay = if (isNew) LocalDate.now().toEpochDay() else null)
    }
    private val baseline = SavedForm(savedStateHandle, "project_baseline", ProjectForm.serializer()) { form.value }
    private var existing: ProjectEntity? = null

    private data class Flags(
        val loading: Boolean,
        val showErrors: Boolean = false,
        val saving: Boolean = false,
        val saved: Boolean = false,
        val saveFailed: Boolean = false,
    )

    private val flags = MutableStateFlow(Flags(loading = !isNew))

    val state: StateFlow<ProjectEditUi> = combine(form.state, baseline.state, flags) { f, base, fl ->
        toUi(f, base, fl)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), toUi(form.value, baseline.value, flags.value))

    init {
        if (!isNew) {
            viewModelScope.launch {
                val project = projects.get(projectId)
                existing = project
                if (project != null && !form.restored) {
                    val loaded = ProjectForm(
                        name = project.name,
                        address = project.address,
                        startDay = project.startDate?.toEpochDay(),
                        budget = Money.toInput(project.budgetPaise),
                        notes = project.notes,
                    )
                    form.update { loaded }
                    baseline.update { loaded }
                }
                flags.update { it.copy(loading = false) }
            }
        }
    }

    private fun toUi(f: ProjectForm, base: ProjectForm, fl: Flags) = ProjectEditUi(
        form = f,
        isNew = isNew,
        loading = fl.loading,
        isDirty = f != base,
        nameMissing = fl.showErrors && f.name.isBlank(),
        budgetInvalid = fl.showErrors && budgetInvalid(f),
        saving = fl.saving,
        saved = fl.saved,
        saveFailed = fl.saveFailed,
    )

    private fun budgetInvalid(f: ProjectForm) = f.budget.isNotBlank() && (Money.parse(f.budget)?.let { it < 0 } ?: true)

    fun onName(v: String) = form.update { it.copy(name = v) }
    fun onAddress(v: String) = form.update { it.copy(address = v) }
    fun onStart(d: LocalDate?) = form.update { it.copy(startDay = d?.toEpochDay()) }
    fun onBudget(v: String) = form.update { it.copy(budget = v) }
    fun onNotes(v: String) = form.update { it.copy(notes = v) }

    fun save() {
        val f = form.value
        if (f.name.isBlank() || budgetInvalid(f)) {
            flags.update { it.copy(showErrors = true) }
            return
        }
        flags.update { it.copy(saving = true, saveFailed = false) }
        viewModelScope.launch {
            try {
                val base = existing ?: ProjectEntity(name = "")
                val id = projects.save(
                    base.copy(
                        name = f.name.trim(),
                        address = f.address.trim(),
                        startDate = f.startDate,
                        budgetPaise = Money.parse(f.budget) ?: 0,
                        notes = f.notes.trim(),
                    ),
                )
                // A newly added house becomes the one you're looking at.
                if (isNew) currentProject.select(id)
                flags.update { it.copy(saving = false, saved = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                flags.update { it.copy(saving = false, saveFailed = true) }
            }
        }
    }
}
