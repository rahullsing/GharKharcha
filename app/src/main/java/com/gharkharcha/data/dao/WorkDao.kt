package com.gharkharcha.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gharkharcha.data.entity.WorkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkDao {
    @Query("SELECT * FROM works WHERE project_id = :projectId ORDER BY created_at ASC")
    fun observeByProject(projectId: Long): Flow<List<WorkEntity>>

    @Query("SELECT * FROM works WHERE id = :id")
    fun observe(id: Long): Flow<WorkEntity?>

    @Query("SELECT * FROM works WHERE id = :id")
    suspend fun get(id: Long): WorkEntity?

    /** Material and labour spend per work of a project (one row per work, zeros included). */
    @Query(
        """
        SELECT w.id AS workId,
               COALESCE((SELECT SUM(m.total_paise) FROM materials m WHERE m.work_id = w.id), 0) AS materialPaise,
               COALESCE((SELECT SUM(a.base_pay_paise + a.overtime_pay_paise) FROM attendance a WHERE a.work_id = w.id), 0) AS labourPaise
        FROM works w
        WHERE w.project_id = :projectId
        """,
    )
    fun observeSpendByWork(projectId: Long): Flow<List<WorkSpendRow>>

    @Query(
        """
        SELECT :workId AS workId,
               COALESCE((SELECT SUM(m.total_paise) FROM materials m WHERE m.work_id = :workId), 0) AS materialPaise,
               COALESCE((SELECT SUM(a.base_pay_paise + a.overtime_pay_paise) FROM attendance a WHERE a.work_id = :workId), 0) AS labourPaise
        """,
    )
    fun observeSpend(workId: Long): Flow<WorkSpendRow>

    @Query(
        """
        SELECT * FROM works
        WHERE (:projectId IS NULL OR project_id = :projectId)
          AND (name LIKE '%' || :query || '%'
               OR notes LIKE '%' || :query || '%')
        ORDER BY created_at ASC
        """,
    )
    fun search(projectId: Long? = null, query: String): Flow<List<WorkEntity>>

    @Insert
    suspend fun insert(work: WorkEntity): Long

    @Update
    suspend fun update(work: WorkEntity)

    @Delete
    suspend fun delete(work: WorkEntity)
}
