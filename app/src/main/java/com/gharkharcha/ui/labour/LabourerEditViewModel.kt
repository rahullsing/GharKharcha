package com.gharkharcha.ui.labour

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gharkharcha.data.entity.LabourerEntity
import com.gharkharcha.data.entity.Skill
import com.gharkharcha.data.repo.LabourerRepository
import com.gharkharcha.ui.common.SavedForm
import com.gharkharcha.ui.navigation.LabourerEditRoute
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
import javax.inject.Inject

@Serializable
data class LabourerForm(
    val name: String = "",
    val phone: String = "",
    val skill: Skill = Skill.HELPER,
    val customSkill: String = "",
    val dailyWage: String = "",
    val hourlyRate: String = "",
    val address: String = "",
    val idNotes: String = "",
    val isActive: Boolean = true,
)

data class LabourerFormErrors(
    val nameMissing: Boolean = false,
    val dailyInvalid: Boolean = false,
    val hourlyInvalid: Boolean = false,
) {
    val any get() = nameMissing || dailyInvalid || hourlyInvalid
}

data class LabourerEditUi(
    val form: LabourerForm,
    val isNew: Boolean,
    val loading: Boolean,
    val isDirty: Boolean,
    val errors: LabourerFormErrors,
    val saving: Boolean,
    /** Id of the saved labourer once saving finished. */
    val savedId: Long?,
    val saveFailed: Boolean,
)

@HiltViewModel
class LabourerEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val labourers: LabourerRepository,
) : ViewModel() {
    private val labourerId = savedStateHandle.toRoute<LabourerEditRoute>().labourerId
    private val isNew = labourerId == 0L

    private val form = SavedForm(savedStateHandle, "labourer_form", LabourerForm.serializer()) { LabourerForm() }
    private val baseline = SavedForm(savedStateHandle, "labourer_baseline", LabourerForm.serializer()) { form.value }
    private var existing: LabourerEntity? = null

    private data class Flags(
        val loading: Boolean,
        val showErrors: Boolean = false,
        val saving: Boolean = false,
        val savedId: Long? = null,
        val saveFailed: Boolean = false,
    )

    private val flags = MutableStateFlow(Flags(loading = !isNew))

    val state: StateFlow<LabourerEditUi> = combine(form.state, baseline.state, flags, ::toUi)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), toUi(form.value, baseline.value, flags.value))

    init {
        if (!isNew) {
            viewModelScope.launch {
                val l = labourers.get(labourerId)
                existing = l
                if (l != null && !form.restored) {
                    val loaded = LabourerForm(
                        name = l.name,
                        phone = l.phone,
                        skill = l.skill,
                        customSkill = l.customSkill,
                        dailyWage = Money.toInput(l.dailyWagePaise),
                        hourlyRate = Money.toInput(l.hourlyRatePaise),
                        address = l.address,
                        idNotes = l.idNotes,
                        isActive = l.isActive,
                    )
                    form.update { loaded }
                    baseline.update { loaded }
                }
                flags.update { it.copy(loading = false) }
            }
        }
    }

    private fun toUi(f: LabourerForm, base: LabourerForm, fl: Flags) = LabourerEditUi(
        form = f,
        isNew = isNew,
        loading = fl.loading,
        isDirty = f != base,
        errors = if (fl.showErrors) validate(f) else LabourerFormErrors(),
        saving = fl.saving,
        savedId = fl.savedId,
        saveFailed = fl.saveFailed,
    )

    fun onName(v: String) = form.update { it.copy(name = v) }
    fun onPhone(v: String) = form.update { it.copy(phone = v.filter { c -> c.isDigit() || c == '+' || c == ' ' }) }
    fun onSkill(v: Skill) = form.update { it.copy(skill = v) }
    fun onCustomSkill(v: String) = form.update { it.copy(customSkill = v) }
    fun onDailyWage(v: String) = form.update { it.copy(dailyWage = v) }
    fun onHourlyRate(v: String) = form.update { it.copy(hourlyRate = v) }
    fun onAddress(v: String) = form.update { it.copy(address = v) }
    fun onIdNotes(v: String) = form.update { it.copy(idNotes = v) }
    fun onActive(v: Boolean) = form.update { it.copy(isActive = v) }

    fun save() {
        val f = form.value
        if (validate(f).any) {
            flags.update { it.copy(showErrors = true) }
            return
        }
        if (flags.value.saving) return // ignore double taps
        flags.update { it.copy(saving = true, saveFailed = false) }
        viewModelScope.launch {
            try {
                val base = existing ?: LabourerEntity(name = "")
                val id = labourers.save(
                    base.copy(
                        name = f.name.trim(),
                        phone = f.phone.trim(),
                        skill = f.skill,
                        customSkill = if (f.skill == Skill.CUSTOM) f.customSkill.trim() else "",
                        dailyWagePaise = Money.parse(f.dailyWage) ?: 0,
                        hourlyRatePaise = Money.parse(f.hourlyRate) ?: 0,
                        address = f.address.trim(),
                        idNotes = f.idNotes.trim(),
                        isActive = f.isActive,
                    ),
                )
                // Stay "saving" so the button can't be pressed again before the screen closes.
                flags.update { it.copy(savedId = id) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                flags.update { it.copy(saving = false, saveFailed = true) }
            }
        }
    }

    private fun invalidAmount(text: String) = text.isNotBlank() && (Money.parse(text)?.let { it < 0 } ?: true)

    private fun validate(f: LabourerForm) = LabourerFormErrors(
        nameMissing = f.name.isBlank(),
        dailyInvalid = invalidAmount(f.dailyWage),
        hourlyInvalid = invalidAmount(f.hourlyRate),
    )
}
