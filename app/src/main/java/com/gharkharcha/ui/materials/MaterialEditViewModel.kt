package com.gharkharcha.ui.materials

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gharkharcha.R
import com.gharkharcha.data.entity.AttachmentKind
import com.gharkharcha.data.entity.MaterialEntity
import com.gharkharcha.data.entity.MaterialPaymentMode
import com.gharkharcha.data.entity.MaterialUnit
import com.gharkharcha.data.entity.OwnerType
import com.gharkharcha.data.entity.VendorEntity
import com.gharkharcha.data.files.ImageImporter
import com.gharkharcha.data.repo.AttachmentRepository
import com.gharkharcha.data.repo.MaterialRepository
import com.gharkharcha.data.repo.VendorRepository
import com.gharkharcha.domain.calc.MaterialCalculator
import com.gharkharcha.domain.model.PhotoItem
import com.gharkharcha.ui.common.SavedForm
import com.gharkharcha.ui.common.UiText
import com.gharkharcha.ui.navigation.MaterialEditRoute
import com.gharkharcha.util.Money
import com.gharkharcha.util.Quantity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.LocalDate
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@Serializable
data class MaterialForm(
    /** False = quick "total only" entry. */
    val itemised: Boolean = true,
    val item: String = "",
    val brand: String = "",
    val qty: String = "",
    val unit: MaterialUnit = MaterialUnit.PIECE,
    val customUnit: String = "",
    val rate: String = "",
    val total: String = "",
    val totalOverridden: Boolean = false,
    val shop: String = "",
    val shopPhone: String = "",
    val day: Long = LocalDate.now().toEpochDay(),
    val billNo: String = "",
    val mode: MaterialPaymentMode = MaterialPaymentMode.CASH,
    val isPaid: Boolean = true,
    val notes: String = "",
    val photos: List<PhotoItem> = emptyList(),
    val removedPhotoIds: List<Long> = emptyList(),
) {
    val date: LocalDate get() = LocalDate.ofEpochDay(day)

    /** qty × rate, or null until both are valid. */
    val autoTotalPaise: Long?
        get() {
            val q = Quantity.parse(qty) ?: return null
            val r = Money.parse(rate) ?: return null
            return MaterialCalculator.total(q, r)
        }

    val totalPaise: Long?
        get() = if (!itemised || totalOverridden) Money.parse(total) else autoTotalPaise
}

data class MaterialErrors(
    val itemMissing: Boolean = false,
    val qtyInvalid: Boolean = false,
    val rateInvalid: Boolean = false,
    val totalMissing: Boolean = false,
) {
    val any get() = itemMissing || qtyInvalid || rateInvalid || totalMissing
}

data class MaterialEditUi(
    val form: MaterialForm,
    val isNew: Boolean,
    val loading: Boolean,
    val isDirty: Boolean,
    val errors: MaterialErrors,
    val saving: Boolean,
    val done: Boolean,
    val saveFailed: Boolean,
    val importing: Boolean,
    val itemSuggestions: List<String>,
    val brandSuggestions: List<String>,
    val shopSuggestions: List<VendorEntity>,
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class MaterialEditViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val materials: MaterialRepository,
    private val vendors: VendorRepository,
    private val attachments: AttachmentRepository,
    private val importer: ImageImporter,
    sharedPhotoHolder: com.gharkharcha.data.files.SharedPhotoHolder,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<MaterialEditRoute>()
    private val isNew = route.materialId == 0L

    private val form = SavedForm(savedStateHandle, "material_form", MaterialForm.serializer()) { MaterialForm() }
    private val baseline = SavedForm(savedStateHandle, "material_baseline", MaterialForm.serializer()) { form.value }
    private var existing: MaterialEntity? = null

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

    private fun <T> suggestions(field: (MaterialForm) -> String, load: suspend (String) -> List<T>, label: (T) -> String) =
        form.state.map(field).distinctUntilChanged().debounce(SUGGEST_DEBOUNCE).mapLatest { text ->
            if (text.isBlank()) emptyList() else load(text).filterNot { label(it).equals(text.trim(), ignoreCase = true) }
        }

    private val itemSuggestions = suggestions({ it.item }, materials::suggestItems) { it }
    private val brandSuggestions = suggestions({ it.brand }, materials::suggestBrands) { it }
    private val shopSuggestions = suggestions({ it.shop }, vendors::suggest) { it.name }

    val state: StateFlow<MaterialEditUi> = combine(
        combine(form.state, baseline.state, flags, ::Triple),
        itemSuggestions,
        brandSuggestions,
        shopSuggestions,
    ) { (f, base, fl), items, brands, shops ->
        MaterialEditUi(
            form = f,
            isNew = isNew,
            loading = fl.loading,
            isDirty = f != base,
            errors = if (fl.showErrors) validate(f) else MaterialErrors(),
            saving = fl.saving,
            done = fl.done,
            saveFailed = fl.saveFailed,
            importing = fl.importing,
            itemSuggestions = items,
            brandSuggestions = brands,
            shopSuggestions = shops,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialUi())

    init {
        if (!isNew) {
            viewModelScope.launch {
                val m = materials.get(route.materialId)
                existing = m
                if (m != null && !form.restored) {
                    val loaded = m.toForm(attachments.loadItems(OwnerType.MATERIAL, m.id))
                    form.update { loaded }
                    baseline.update { loaded }
                }
                flags.update { it.copy(loading = false) }
            }
        } else {
            val shared = sharedPhotoHolder.consume()
            if (shared.isNotEmpty()) {
                form.update { it.copy(photos = it.photos + shared) }
            }
        }
    }

    private fun initialUi() = MaterialEditUi(
        form.value, isNew, !isNew, false, MaterialErrors(), false, false, false, false, emptyList(), emptyList(), emptyList(),
    )

    private fun MaterialEntity.toForm(photos: List<PhotoItem>) = MaterialForm(
        itemised = quantityMilli != null || ratePaise != null || brand.isNotBlank(),
        item = itemName,
        brand = brand,
        qty = quantityMilli?.let(Quantity::format).orEmpty(),
        unit = unit ?: MaterialUnit.PIECE,
        customUnit = customUnit,
        rate = ratePaise?.let(Money::toInput).orEmpty(),
        total = Money.toInput(totalPaise),
        totalOverridden = totalOverridden,
        shop = shopName,
        shopPhone = shopPhone,
        day = purchaseDate.toEpochDay(),
        billNo = billNumber,
        mode = paymentMode,
        isPaid = isPaid,
        notes = notes,
        photos = photos,
    )

    // --- Field updates ---------------------------------------------------------------------------

    fun onItemised(v: Boolean) = form.update { it.copy(itemised = v) }
    fun onItem(v: String) = form.update { it.copy(item = v) }
    fun onBrand(v: String) = form.update { it.copy(brand = v) }
    fun onQty(v: String) = form.update { it.copy(qty = v) }
    fun onUnit(v: MaterialUnit) = form.update { it.copy(unit = v) }
    fun onCustomUnit(v: String) = form.update { it.copy(customUnit = v) }
    fun onRate(v: String) = form.update { it.copy(rate = v) }

    /** Typing a total in itemised mode overrides qty × rate. */
    fun onTotal(v: String) = form.update { it.copy(total = v, totalOverridden = it.itemised || it.totalOverridden) }

    fun resetTotal() = form.update { it.copy(totalOverridden = false, total = "") }
    fun onShop(v: String) = form.update { it.copy(shop = v) }

    fun onShopPicked(vendor: VendorEntity) = form.update {
        it.copy(shop = vendor.name, shopPhone = it.shopPhone.ifBlank { vendor.phone })
    }

    fun onShopPhone(v: String) = form.update { it.copy(shopPhone = v.filter { c -> c.isDigit() || c == '+' || c == ' ' }) }
    fun onDate(v: LocalDate?) {
        if (v != null) form.update { it.copy(day = v.toEpochDay()) }
    }
    fun onBillNo(v: String) = form.update { it.copy(billNo = v) }

    /** Credit purchases are unpaid by default; any other mode means it was paid. */
    fun onMode(v: MaterialPaymentMode) = form.update { it.copy(mode = v, isPaid = v != MaterialPaymentMode.CREDIT) }
    fun onPaid(v: Boolean) = form.update { it.copy(isPaid = v) }
    fun onNotes(v: String) = form.update { it.copy(notes = v) }

    // --- Photos ----------------------------------------------------------------------------------

    /** Remembers which strip the camera was opened for (survives the trip to the camera screen). */
    fun startCamera(kind: AttachmentKind) {
        savedStateHandle[KEY_CAMERA_KIND] = kind.name
    }

    fun onCaptured(path: String) {
        val kind = savedStateHandle.get<String>(KEY_CAMERA_KIND)?.let(AttachmentKind::valueOf) ?: AttachmentKind.PHOTO
        importPhotos { importer.importCapture(path, kind)?.let { listOf(it) to 0 } ?: (emptyList<PhotoItem>() to 1) }
    }

    fun onPicked(uris: List<Uri>, kind: AttachmentKind) = importPhotos { importer.importAll(uris, kind) }

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

    // --- Save ------------------------------------------------------------------------------------

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
                val base = existing ?: MaterialEntity(workId = route.workId, itemName = "", totalPaise = 0, purchaseDate = f.date)
                val entity = base.copy(
                    itemName = f.item.trim(),
                    brand = if (f.itemised) f.brand.trim() else "",
                    quantityMilli = if (f.itemised) Quantity.parse(f.qty) else null,
                    unit = if (f.itemised && f.qty.isNotBlank()) f.unit else null,
                    customUnit = if (f.itemised && f.unit == MaterialUnit.CUSTOM) f.customUnit.trim() else "",
                    ratePaise = if (f.itemised) Money.parse(f.rate) else null,
                    totalPaise = f.totalPaise ?: 0,
                    totalOverridden = !f.itemised || f.totalOverridden,
                    shopName = f.shop.trim(),
                    shopPhone = f.shopPhone.trim(),
                    purchaseDate = f.date,
                    billNumber = f.billNo.trim(),
                    paymentMode = f.mode,
                    isPaid = f.isPaid,
                    notes = f.notes.trim(),
                )
                materials.saveWithPhotos(entity, f.photos, f.removedPhotoIds)
                // Stay "saving" so the button can't be pressed again before the screen closes.
                flags.update { it.copy(done = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                flags.update { it.copy(saving = false, saveFailed = true) }
            }
        }
    }

    private fun validate(f: MaterialForm) = MaterialErrors(
        itemMissing = f.item.isBlank(),
        qtyInvalid = f.itemised && f.qty.isNotBlank() && Quantity.parse(f.qty) == null,
        rateInvalid = f.itemised && f.rate.isNotBlank() && Money.parse(f.rate) == null,
        totalMissing = (f.totalPaise ?: 0) <= 0,
    )

    private companion object {
        const val KEY_CAMERA_KIND = "camera_kind"
        val SUGGEST_DEBOUNCE = 150.milliseconds
    }
}
