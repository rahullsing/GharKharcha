package com.gharkharcha.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gharkharcha.data.dao.LabourerDao
import com.gharkharcha.data.dao.MaterialDao
import com.gharkharcha.data.dao.PaymentDao
import com.gharkharcha.data.dao.VendorDao
import com.gharkharcha.data.dao.WorkDao
import com.gharkharcha.data.entity.LabourerEntity
import com.gharkharcha.data.entity.MaterialEntity
import com.gharkharcha.data.entity.PaymentEntity
import com.gharkharcha.data.entity.VendorEntity
import com.gharkharcha.data.entity.WorkEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SearchResults(
    val works: List<WorkEntity> = emptyList(),
    val labourers: List<LabourerEntity> = emptyList(),
    val materials: List<MaterialEntity> = emptyList(),
    val payments: List<PaymentEntity> = emptyList(),
    val vendors: List<VendorEntity> = emptyList(),
) {
    val isEmpty: Boolean
        get() = works.isEmpty() && labourers.isEmpty() && materials.isEmpty() && payments.isEmpty() && vendors.isEmpty()
    val totalCount: Int
        get() = works.size + labourers.size + materials.size + payments.size + vendors.size
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val workDao: WorkDao,
    private val labourerDao: LabourerDao,
    private val materialDao: MaterialDao,
    private val paymentDao: PaymentDao,
    private val vendorDao: VendorDao,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun clearQuery() {
        _query.value = ""
    }

    val results: StateFlow<SearchResults> = _query
        .debounce(150)
        .distinctUntilChanged()
        .flatMapLatest { q ->
            val trimmed = q.trim()
            if (trimmed.isEmpty()) {
                flowOf(SearchResults())
            } else {
                combine(
                    workDao.search(null, trimmed),
                    labourerDao.search(trimmed),
                    materialDao.searchAll(trimmed),
                    paymentDao.searchAll(trimmed),
                    vendorDao.search(trimmed),
                ) { works, labourers, materials, payments, vendors ->
                    SearchResults(
                        works = works,
                        labourers = labourers,
                        materials = materials,
                        payments = payments,
                        vendors = vendors,
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchResults())
}
