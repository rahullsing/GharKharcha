package com.gharkharcha.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gharkharcha.data.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY created_at DESC")
    fun observeAll(): Flow<List<ProjectEntity>>

    /** Every project with its work count and spend. */
    @Query(
        """
        SELECT p.*,
               (SELECT COUNT(*) FROM works w WHERE w.project_id = p.id) AS workCount,
               COALESCE((SELECT SUM(m.total_paise) FROM materials m
                         INNER JOIN works w ON w.id = m.work_id WHERE w.project_id = p.id), 0) AS materialPaise,
               COALESCE((SELECT SUM(a.base_pay_paise + a.overtime_pay_paise) FROM attendance a
                         INNER JOIN works w ON w.id = a.work_id WHERE w.project_id = p.id), 0) AS labourPaise
        FROM projects p
        ORDER BY p.created_at DESC
        """,
    )
    fun observeSummaries(): Flow<List<ProjectSummaryRow>>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun observe(id: Long): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun get(id: Long): ProjectEntity?

    @Query("SELECT COUNT(*) FROM projects")
    suspend fun count(): Int

    @Insert
    suspend fun insert(project: ProjectEntity): Long

    @Update
    suspend fun update(project: ProjectEntity)

    @Delete
    suspend fun delete(project: ProjectEntity)
}
