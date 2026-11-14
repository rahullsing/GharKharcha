package com.gharkharcha.data.repo

import androidx.room.withTransaction
import com.gharkharcha.data.dao.AttachmentDao
import com.gharkharcha.data.dao.ProjectDao
import com.gharkharcha.data.dao.ProjectSummaryRow
import com.gharkharcha.data.db.GharKharchaDatabase
import com.gharkharcha.data.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepository @Inject constructor(
    private val db: GharKharchaDatabase,
    private val dao: ProjectDao,
    private val attachmentDao: AttachmentDao,
    private val attachments: AttachmentRepository,
) {
    fun observeAll(): Flow<List<ProjectEntity>> = dao.observeAll()

    fun observe(id: Long): Flow<ProjectEntity?> = dao.observe(id)

    fun observeSummaries(): Flow<List<ProjectSummaryRow>> = dao.observeSummaries()

    suspend fun get(id: Long): ProjectEntity? = dao.get(id)

    /** Inserts when id == 0, else updates. Returns the id. */
    suspend fun save(project: ProjectEntity): Long =
        if (project.id == 0L) {
            dao.insert(project)
        } else {
            dao.update(project.copy(updatedAt = System.currentTimeMillis()))
            project.id
        }

    /** Deletes the project, its works/materials/attendance (FK cascade) and all their photos. */
    suspend fun delete(project: ProjectEntity) {
        val owned = db.withTransaction {
            val list = attachmentDao.listForProjectTree(project.id)
            attachmentDao.deleteAll(list)
            dao.delete(project)
            list
        }
        attachments.deleteFiles(owned)
    }
}
