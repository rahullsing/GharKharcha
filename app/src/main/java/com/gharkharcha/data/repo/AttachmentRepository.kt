package com.gharkharcha.data.repo

import com.gharkharcha.data.dao.AttachmentDao
import com.gharkharcha.data.entity.AttachmentEntity
import com.gharkharcha.data.entity.AttachmentKind
import com.gharkharcha.data.entity.OwnerType
import com.gharkharcha.data.files.AttachmentFileStore
import com.gharkharcha.di.IoDispatcher
import com.gharkharcha.domain.model.PhotoItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
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

    fun observeForWorkTree(workId: Long): Flow<List<AttachmentEntity>> = dao.observeForWorkTree(workId)

    fun observeMaterialPhotoCounts(workId: Long): Flow<Map<Long, Int>> =
        dao.observeMaterialPhotoCounts(workId).map { rows -> rows.associate { it.ownerId to it.count } }

    fun observePaymentPhotoCounts(workId: Long): Flow<Map<Long, Int>> =
        dao.observePaymentPhotoCounts(workId).map { rows -> rows.associate { it.ownerId to it.count } }

    fun observeLabourerPaymentPhotoCounts(labourerId: Long): Flow<Map<Long, Int>> =
        dao.observeLabourerPaymentPhotoCounts(labourerId).map { rows -> rows.associate { it.ownerId to it.count } }

    /** First photo of each owner of [ownerType] (labourer profile, house cover), as absolute paths. */
    fun observeFirstPhotos(ownerType: OwnerType): Flow<Map<Long, String>> =
        dao.observeByOwnerType(ownerType).map { list ->
            list.groupBy { it.ownerId }.mapValues { (_, items) -> files.resolve(items.first().filePath).absolutePath }
        }

    fun fileOf(attachment: AttachmentEntity): File = files.resolve(attachment.filePath)

    suspend fun listFor(ownerType: OwnerType, ownerId: Long): List<AttachmentEntity> =
        dao.listFor(ownerType, ownerId)

    /** Existing photos of an owner as form items. */
    suspend fun loadItems(ownerType: OwnerType, ownerId: Long): List<PhotoItem> =
        dao.listFor(ownerType, ownerId).map {
            PhotoItem(
                key = "a${it.id}",
                path = files.resolve(it.filePath).absolutePath,
                kind = it.kind,
                attachmentId = it.id,
                caption = it.caption,
                takenAt = it.takenAt,
            )
        }

    /**
     * Makes the owner's photos match [items] (in this order): moves new pending files into place,
     * inserts their rows, updates order/caption of kept ones and deletes [removedIds].
     */
    suspend fun sync(ownerType: OwnerType, ownerId: Long, items: List<PhotoItem>, removedIds: Collection<Long>) {
        removedIds.forEach { id -> dao.get(id)?.let { delete(it) } }
        val existing = dao.listFor(ownerType, ownerId).associateBy { it.id }
        items.forEachIndexed { index, item ->
            val current = item.attachmentId?.let(existing::get)
            if (current != null) {
                if (current.sortOrder != index || current.caption != item.caption || current.kind != item.kind) {
                    dao.update(current.copy(sortOrder = index, caption = item.caption, kind = item.kind))
                }
            } else if (item.attachmentId == null) {
                val pending = File(item.path)
                if (!pending.exists()) return@forEachIndexed
                val relative = withContext(io) { files.moveIntoOwner(pending, ownerType, ownerId) }
                dao.insert(
                    AttachmentEntity(
                        ownerType = ownerType,
                        ownerId = ownerId,
                        kind = item.kind,
                        filePath = relative,
                        caption = item.caption,
                        sortOrder = index,
                        takenAt = item.takenAt,
                    ),
                )
            }
        }
    }

    /** Replaces an owner's single photo (profile / cover). Null clears it. */
    suspend fun replaceSingle(ownerType: OwnerType, ownerId: Long, item: PhotoItem?, kind: AttachmentKind) {
        val current = dao.listFor(ownerType, ownerId)
        if (item?.attachmentId != null && current.any { it.id == item.attachmentId }) return
        sync(ownerType, ownerId, listOfNotNull(item?.copy(kind = kind)), current.map { it.id })
    }

    suspend fun updateCaption(attachment: AttachmentEntity, caption: String) =
        dao.update(attachment.copy(caption = caption.trim()))

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

    /** Deletes the pending file of an unsaved photo the user removed. */
    suspend fun discardPending(item: PhotoItem) = withContext(io) {
        if (item.attachmentId == null) File(item.path).takeIf { it.startsWith(files.pendingDir) }?.delete()
    }
}
