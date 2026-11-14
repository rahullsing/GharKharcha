package com.gharkharcha.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gharkharcha.data.entity.AttachmentEntity
import com.gharkharcha.data.entity.OwnerType
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {
    @Query(
        """
        SELECT * FROM attachments WHERE owner_type = :ownerType AND owner_id = :ownerId
        ORDER BY sort_order, id
        """,
    )
    fun observeFor(ownerType: OwnerType, ownerId: Long): Flow<List<AttachmentEntity>>

    @Query(
        """
        SELECT * FROM attachments WHERE owner_type = :ownerType AND owner_id = :ownerId
        ORDER BY sort_order, id
        """,
    )
    suspend fun listFor(ownerType: OwnerType, ownerId: Long): List<AttachmentEntity>

    @Query("SELECT * FROM attachments WHERE id = :id")
    suspend fun get(id: Long): AttachmentEntity?

    /** Attachments of a work and of every material under it (payments survive work deletion). */
    @Query(
        """
        SELECT * FROM attachments
        WHERE (owner_type = 'WORK' AND owner_id = :workId)
           OR (owner_type = 'MATERIAL' AND owner_id IN (SELECT id FROM materials WHERE work_id = :workId))
        """,
    )
    suspend fun listForWorkTree(workId: Long): List<AttachmentEntity>

    /** Attachments of a project, its works and their materials. */
    @Query(
        """
        SELECT * FROM attachments
        WHERE (owner_type = 'PROJECT' AND owner_id = :projectId)
           OR (owner_type = 'WORK' AND owner_id IN (SELECT id FROM works WHERE project_id = :projectId))
           OR (owner_type = 'MATERIAL' AND owner_id IN (
                SELECT m.id FROM materials m INNER JOIN works w ON w.id = m.work_id
                WHERE w.project_id = :projectId))
        """,
    )
    suspend fun listForProjectTree(projectId: Long): List<AttachmentEntity>

    @Query("SELECT * FROM attachments")
    suspend fun listAll(): List<AttachmentEntity>

    @Insert
    suspend fun insert(attachment: AttachmentEntity): Long

    @Update
    suspend fun update(attachment: AttachmentEntity)

    @Update
    suspend fun updateAll(attachments: List<AttachmentEntity>)

    @Delete
    suspend fun delete(attachment: AttachmentEntity)

    @Delete
    suspend fun deleteAll(attachments: List<AttachmentEntity>)
}
