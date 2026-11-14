package com.gharkharcha.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gharkharcha.data.entity.AttendanceEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance WHERE work_id = :workId AND date = :date")
    fun observeForWorkOnDate(workId: Long, date: LocalDate): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE work_id = :workId AND date = :date")
    suspend fun listForWorkOnDate(workId: Long, date: LocalDate): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE work_id = :workId ORDER BY date DESC, id DESC")
    fun observeForWork(workId: Long): Flow<List<AttendanceEntity>>

    @Query(
        """
        SELECT * FROM attendance
        WHERE work_id = :workId AND labourer_id = :labourerId AND date = :date
        LIMIT 1
        """,
    )
    suspend fun find(workId: Long, labourerId: Long, date: LocalDate): AttendanceEntity?

    /** Ledger rows for a labourer, newest first; [projectId] null = all projects. */
    @Query(
        """
        SELECT a.*, w.name AS workName FROM attendance a
        INNER JOIN works w ON w.id = a.work_id
        WHERE a.labourer_id = :labourerId AND (:projectId IS NULL OR w.project_id = :projectId)
        ORDER BY a.date DESC, a.id DESC
        """,
    )
    fun observeForLabourer(labourerId: Long, projectId: Long? = null): Flow<List<AttendanceWithWork>>

    /** Per-labourer totals; null filters mean "all". */
    @Query(
        """
        SELECT a.labourer_id AS labourerId,
               SUM(CASE WHEN a.status IN ('FULL_DAY', 'OVERTIME') THEN 1 ELSE 0 END) AS fullDays,
               SUM(CASE WHEN a.status = 'HALF_DAY' THEN 1 ELSE 0 END) AS halfDays,
               SUM(CASE WHEN a.status = 'HOURS' THEN a.minutes_worked ELSE 0 END) AS hourlyMinutes,
               SUM(CASE WHEN a.status = 'ABSENT' THEN 0 ELSE a.minutes_worked END + a.overtime_minutes) AS totalMinutes,
               SUM(a.base_pay_paise + a.overtime_pay_paise) AS earnedPaise,
               SUM(a.advance_paise) AS advancePaise
        FROM attendance a
        INNER JOIN works w ON w.id = a.work_id
        WHERE (:projectId IS NULL OR w.project_id = :projectId)
          AND (:workId IS NULL OR a.work_id = :workId)
        GROUP BY a.labourer_id
        """,
    )
    fun observeTotalsByLabourer(projectId: Long? = null, workId: Long? = null): Flow<List<LabourerAttendanceRow>>

    /** Total labour cost on a given day for a project. */
    @Query(
        """
        SELECT COALESCE(SUM(a.base_pay_paise + a.overtime_pay_paise), 0) FROM attendance a
        INNER JOIN works w ON w.id = a.work_id
        WHERE w.project_id = :projectId AND a.date = :date
        """,
    )
    fun observeDayCost(projectId: Long, date: LocalDate): Flow<Long>

    @Query(
        """
        SELECT a.date AS date, SUM(a.base_pay_paise + a.overtime_pay_paise) AS totalPaise
        FROM attendance a
        INNER JOIN works w ON a.work_id = w.id
        WHERE w.project_id = :projectId AND a.date >= :sinceDate
        GROUP BY a.date
        """,
    )
    fun observeDailySpend(projectId: Long, sinceDate: LocalDate): Flow<List<DailySpendRow>>

    @Query(
        """
        SELECT a.date AS date, SUM(a.base_pay_paise + a.overtime_pay_paise) AS totalPaise
        FROM attendance a
        INNER JOIN works w ON a.work_id = w.id
        WHERE w.project_id = :projectId
        GROUP BY a.date
        """,
    )
    fun observeAllDailySpend(projectId: Long): Flow<List<DailySpendRow>>

    @Query(
        """
        SELECT a.*, w.name AS workName, l.name AS labourerName FROM attendance a
        INNER JOIN works w ON w.id = a.work_id
        INNER JOIN labourers l ON l.id = a.labourer_id
        WHERE w.project_id = :projectId
        ORDER BY a.date DESC, a.id DESC
        LIMIT :limit
        """,
    )
    fun observeRecentForProject(projectId: Long, limit: Int = 10): Flow<List<AttendanceActivityRow>>

    @Query(
        """
        SELECT a.*, l.name AS labourerName, l.custom_skill AS labourerSkill FROM attendance a
        INNER JOIN labourers l ON l.id = a.labourer_id
        WHERE a.work_id = :workId
        ORDER BY a.date ASC, a.id ASC
        """,
    )
    fun observeForWorkWithLabourer(workId: Long): Flow<List<AttendanceWithLabourer>>

    @Insert
    suspend fun insert(entry: AttendanceEntity): Long

    @Update
    suspend fun update(entry: AttendanceEntity)

    @Delete
    suspend fun delete(entry: AttendanceEntity)
}
