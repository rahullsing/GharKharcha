package com.gharkharcha.data.repo

import com.gharkharcha.data.dao.VendorDao
import com.gharkharcha.data.entity.VendorEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VendorRepository @Inject constructor(
    private val dao: VendorDao,
) {
    fun observeAll(): Flow<List<VendorEntity>> = dao.observeAll()

    suspend fun suggest(query: String): List<VendorEntity> = dao.suggest(query.trim())

    suspend fun save(vendor: VendorEntity): Long =
        if (vendor.id == 0L) dao.insert(vendor) else vendor.id.also { dao.update(vendor) }

    suspend fun delete(vendor: VendorEntity) = dao.delete(vendor)

    /**
     * Returns the id of the vendor named [name] (case-insensitive), creating it if needed.
     * Fills in a missing phone on an existing vendor. Returns null for a blank name.
     */
    suspend fun ensure(name: String, phone: String = ""): Long? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return null
        val existing = dao.findByName(trimmed)
        if (existing != null) {
            if (existing.phone.isBlank() && phone.isNotBlank()) dao.update(existing.copy(phone = phone.trim()))
            return existing.id
        }
        val id = dao.insert(VendorEntity(name = trimmed, phone = phone.trim()))
        return if (id > 0) id else dao.findByName(trimmed)?.id
    }
}
