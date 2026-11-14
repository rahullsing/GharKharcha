package com.gharkharcha.data.repo

import com.gharkharcha.data.dao.AttachmentDao
import com.gharkharcha.data.entity.AttachmentEntity
import com.gharkharcha.data.entity.OwnerType
import com.gharkharcha.data.files.AttachmentFileStore
import com.gharkharcha.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttachmentRepository @Inject constructor(
    private val dao: AttachmentDao,
    private val files: AttachmentFileStore,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) {
    fun observeFor(ownerType: OwnerType, ownerId: Long): Flow<List<AttachmentEntity>> =
        dao.observeFor(ownerType, ownerId)

    suspend fun listFor(ownerType: OwnerType, ownerId: Long): List<AttachmentEntity> =
        dao.listFor(ownerType, ownerId)

    suspend fun add(attachment: AttachmentEntity): Long = dao.insert(attachment)

    suspend fun updateCaption(attachment: AttachmentEntity, caption: String) =
        dao.update(attachment.copy(caption = caption.trim()))

    /** Persists the given order (index becomes sort_order). */
    suspend fun reorder(ordered: List<AttachmentEntity>) =
        dao.updateAll(ordered.mapIndexed { index, a -> a.copy(sortOrder = index) })

    suspend fun delete(attachment: AttachmentEntity) {
        dao.delete(attachment)
        deleteFiles(listOf(attachment))
    }

    suspend fun deleteForOwner(ownerType: OwnerType, ownerId: Long) {
        val list = dao.listFor(ownerType, ownerId)
        dao.deleteAll(list)
        deleteFiles(list)
    }

    /** Call after the owning rows are deleted (outside the DB transaction). */
    suspend fun deleteFiles(attachments: List<AttachmentEntity>) = withContext(io) {
        attachments.forEach { files.delete(it.filePath) }
    }
}
