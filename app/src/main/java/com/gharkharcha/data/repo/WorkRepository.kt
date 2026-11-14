package com.gharkharcha.data.repo

import androidx.room.withTransaction
import com.gharkharcha.data.dao.AttachmentDao
import com.gharkharcha.data.dao.WorkDao
import com.gharkharcha.data.dao.WorkSpendRow
import com.gharkharcha.data.db.GharKharchaDatabase
import com.gharkharcha.data.entity.WorkEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkRepository @Inject constructor(
    private val db: GharKharchaDatabase,
    private val dao: WorkDao,
    private val attachmentDao: AttachmentDao,
    private val attachments: AttachmentRepository,
) {
    fun observeByProject(projectId: Long): Flow<List<WorkEntity>> = dao.observeByProject(projectId)

    fun observe(id: Long): Flow<WorkEntity?> = dao.observe(id)

    fun observeSpendByWork(projectId: Long): Flow<List<WorkSpendRow>> = dao.observeSpendByWork(projectId)

    suspend fun get(id: Long): WorkEntity? = dao.get(id)

    suspend fun save(work: WorkEntity): Long =
        if (work.id == 0L) {
            dao.insert(work)
        } else {
            dao.update(work.copy(updatedAt = System.currentTimeMillis()))
            work.id
        }

    /** Materials and attendance cascade; payments keep the labourer but lose the work link. */
    suspend fun delete(work: WorkEntity) {
        val owned = db.withTransaction {
            val list = attachmentDao.listForWorkTree(work.id)
            attachmentDao.deleteAll(list)
            dao.delete(work)
            list
        }
        attachments.deleteFiles(owned)
    }
}
