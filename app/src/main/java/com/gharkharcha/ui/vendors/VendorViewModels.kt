package com.gharkharcha.ui.vendors

import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gharkharcha.data.entity.VendorEntity
import com.gharkharcha.data.repo.VendorRepository
import com.gharkharcha.ui.common.LoadState
import com.gharkharcha.ui.common.SavedForm
import com.gharkharcha.ui.common.retryableLoad
import com.gharkharcha.ui.navigation.VendorEditRoute
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

@HiltViewModel
class VendorsViewModel @Inject constructor(vendors: VendorRepository) : ViewModel() {
    private val retry = MutableStateFlow(0)

    val state: StateFlow<LoadState<List<VendorEntity>>> = retryableLoad(retry) { vendors.observeAll() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoadState.Loading)

    fun retry() = retry.update { it + 1 }
}

@Serializable
data class VendorForm(
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val gstin: String = "",
)

data class VendorEditUi(
    val form: VendorForm,
    val isNew: Boolean,
    val loading: Boolean,
    val isDirty: Boolean,
    val nameMissing: Boolean,
    val nameTaken: Boolean,
    val saving: Boolean,
    val done: Boolean,
)

@HiltViewModel
class VendorEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vendors: VendorRepository,
) : ViewModel() {
    private val vendorId = savedStateHandle.toRoute<VendorEditRoute>().vendorId
    private val isNew = vendorId == 0L
    private val form = SavedForm(savedStateHandle, "vendor_form", VendorForm.serializer()) { VendorForm() }
    private val baseline = SavedForm(savedStateHandle, "vendor_baseline", VendorForm.serializer()) { form.value }
    private var existing: VendorEntity? = null

    private data class Flags(
        val loading: Boolean,
        val showErrors: Boolean = false,
        val nameTaken: Boolean = false,
        val saving: Boolean = false,
        val done: Boolean = false,
    )

    private val flags = MutableStateFlow(Flags(loading = !isNew))

    val state: StateFlow<VendorEditUi> = combine(form.state, baseline.state, flags) { f, b, fl ->
        VendorEditUi(f, isNew, fl.loading, f != b, fl.showErrors && f.name.isBlank(), fl.nameTaken, fl.saving, fl.done)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        VendorEditUi(form.value, isNew, !isNew, false, false, false, false, false),
    )

    init {
        if (!isNew) {
            viewModelScope.launch {
                val v = vendors.get(vendorId)
                existing = v
                if (v != null && !form.restored) {
                    val loaded = VendorForm(v.name, v.phone, v.address, v.gstin)
                    form.update { loaded }
                    baseline.update { loaded }
                }
                flags.update { it.copy(loading = false) }
            }
        }
    }

    fun onName(v: String) {
        form.update { it.copy(name = v) }
        flags.update { it.copy(nameTaken = false) }
    }
    fun onPhone(v: String) = form.update { it.copy(phone = v.filter { c -> c.isDigit() || c == '+' || c == ' ' }) }
    fun onAddress(v: String) = form.update { it.copy(address = v) }
    fun onGstin(v: String) = form.update { it.copy(gstin = v.uppercase().filter(Char::isLetterOrDigit).take(GSTIN_LENGTH)) }

    fun save() {
        val f = form.value
        if (f.name.isBlank()) {
            flags.update { it.copy(showErrors = true) }
            return
        }
        if (flags.value.saving) return
        flags.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                val base = existing ?: VendorEntity(name = "")
                val id = vendors.save(base.copy(name = f.name.trim(), phone = f.phone.trim(), address = f.address.trim(), gstin = f.gstin))
                if (id < 0) {
                    flags.update { it.copy(saving = false, nameTaken = true) }
                } else {
                    flags.update { it.copy(done = true) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: SQLiteConstraintException) {
                flags.update { it.copy(saving = false, nameTaken = true) }
            }
        }
    }

    fun delete() {
        val v = existing ?: return
        viewModelScope.launch {
            vendors.delete(v)
            flags.update { it.copy(done = true) }
        }
    }

    private companion object {
        const val GSTIN_LENGTH = 15
    }
}
