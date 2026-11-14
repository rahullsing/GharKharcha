package com.gharkharcha.data.repo

import androidx.room.withTransaction
import com.gharkharcha.data.dao.AttachmentDao
import com.gharkharcha.data.dao.MaterialDao
import com.gharkharcha.data.db.GharKharchaDatabase
import com.gharkharcha.data.entity.MaterialEntity
import com.gharkharcha.data.entity.OwnerType
import com.gharkharcha.domain.model.PhotoItem
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MaterialRepository @Inject constructor(
    private val db: GharKharchaDatabase,
    private val dao: MaterialDao,
    private val vendors: VendorRepository,
    private val attachmentDao: AttachmentDao,
    private val attachments: AttachmentRepository,
) {
    fun observeByWork(workId: Long): Flow<List<MaterialEntity>> = dao.observeByWork(workId)

    fun observe(id: Long): Flow<MaterialEntity?> = dao.observe(id)

    suspend fun get(id: Long): MaterialEntity? = dao.get(id)

    suspend fun suggestBrands(query: String): List<String> = dao.suggestBrands(query.trim())

    suspend fun suggestItems(query: String): List<String> = dao.suggestItems(query.trim())

    /** Saves the material and links / creates its vendor from the shop name. */
    suspend fun save(material: MaterialEntity): Long = db.withTransaction {
        val vendorId = vendors.ensure(material.shopName, material.shopPhone)
        val toSave = material.copy(vendorId = vendorId, updatedAt = System.currentTimeMillis())
        if (toSave.id == 0L) dao.insert(toSave) else toSave.id.also { dao.update(toSave) }
    }

    /** Saves the material, then brings its photos in line with [photos] (removing [removedPhotoIds]). */
    suspend fun saveWithPhotos(material: MaterialEntity, photos: List<PhotoItem>, removedPhotoIds: Collection<Long>): Long {
        val id = save(material)
        attachments.sync(OwnerType.MATERIAL, id, photos, removedPhotoIds)
        return id
    }

    /** Copies a material as a new entry dated [onDate] (photos are not copied). */
    suspend fun duplicate(material: MaterialEntity, onDate: LocalDate = material.purchaseDate): Long {
        val now = System.currentTimeMillis()
        return dao.insert(material.copy(id = 0, purchaseDate = onDate, createdAt = now, updatedAt = now))
    }

    suspend fun delete(material: MaterialEntity) {
        val owned = db.withTransaction {
            val list = attachmentDao.listFor(OwnerType.MATERIAL, material.id)
            attachmentDao.deleteAll(list)
            dao.delete(material)
            list
        }
        attachments.deleteFiles(owned)
    }
}
