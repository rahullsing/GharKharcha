package com.gharkharcha.data.repo

import androidx.room.withTransaction
import com.gharkharcha.data.dao.AttachmentDao
import com.gharkharcha.data.dao.LabourerDao
import com.gharkharcha.data.db.GharKharchaDatabase
import com.gharkharcha.data.entity.LabourerEntity
import com.gharkharcha.data.entity.OwnerType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LabourerRepository @Inject constructor(
    private val db: GharKharchaDatabase,
    private val dao: LabourerDao,
    private val attachmentDao: AttachmentDao,
    private val attachments: AttachmentRepository,
) {
    fun observeAll(): Flow<List<LabourerEntity>> = dao.observeAll()

    fun observeActive(): Flow<List<LabourerEntity>> = dao.observeActive()

    fun observe(id: Long): Flow<LabourerEntity?> = dao.observe(id)

    suspend fun get(id: Long): LabourerEntity? = dao.get(id)

    suspend fun save(labourer: LabourerEntity): Long =
        if (labourer.id == 0L) {
            dao.insert(labourer)
        } else {
            dao.update(labourer.copy(updatedAt = System.currentTimeMillis()))
            labourer.id
        }

    suspend fun setActive(labourer: LabourerEntity, active: Boolean) =
        dao.update(labourer.copy(isActive = active, updatedAt = System.currentTimeMillis()))

    /** True when the labourer has attendance/payments and can only be deactivated. */
    suspend fun hasHistory(id: Long): Boolean = dao.countReferences(id) > 0

    sealed interface DeleteResult {
        data object Deleted : DeleteResult
        data object HasHistory : DeleteResult
    }

    suspend fun delete(labourer: LabourerEntity): DeleteResult {
        val owned = db.withTransaction {
            if (dao.countReferences(labourer.id) > 0) return@withTransaction null
            val list = attachmentDao.listFor(OwnerType.LABOURER, labourer.id)
            attachmentDao.deleteAll(list)
            dao.delete(labourer)
            list
        } ?: return DeleteResult.HasHistory
        attachments.deleteFiles(owned)
        return DeleteResult.Deleted
    }
}
