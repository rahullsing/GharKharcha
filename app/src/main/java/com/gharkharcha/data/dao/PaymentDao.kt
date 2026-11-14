package com.gharkharcha.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gharkharcha.data.entity.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query(
        """
        SELECT * FROM payments
        WHERE labourer_id = :labourerId AND (:projectId IS NULL OR project_id = :projectId)
        ORDER BY date DESC, id DESC
        """,
    )
    fun observeForLabourer(labourerId: Long, projectId: Long? = null): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE work_id = :workId ORDER BY date DESC, id DESC")
    fun observeForWork(workId: Long): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE project_id = :projectId ORDER BY date DESC, id DESC")
    fun observeForProject(projectId: Long): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE id = :id")
    suspend fun get(id: Long): PaymentEntity?

    @Query(
        """
        SELECT * FROM payments
        WHERE project_id = :projectId
        ORDER BY date DESC, id DESC
        LIMIT :limit
        """,
    )
    fun observeRecentForProject(projectId: Long, limit: Int = 10): Flow<List<PaymentEntity>>

    @Query(
        """
        SELECT p.* FROM payments p
        INNER JOIN labourers l ON p.labourer_id = l.id
        WHERE p.project_id = :projectId
          AND (l.name LIKE '%' || :query || '%'
               OR p.reference LIKE '%' || :query || '%'
               OR p.notes LIKE '%' || :query || '%')
        ORDER BY p.date DESC
        """,
    )
    fun search(projectId: Long, query: String): Flow<List<PaymentEntity>>

    @Query(
        """
        SELECT p.* FROM payments p
        INNER JOIN labourers l ON p.labourer_id = l.id
        WHERE l.name LIKE '%' || :query || '%'
           OR p.reference LIKE '%' || :query || '%'
           OR p.notes LIKE '%' || :query || '%'
        ORDER BY p.date DESC
        """,
    )
    fun searchAll(query: String): Flow<List<PaymentEntity>>

    /** Per-labourer payment totals; null filters mean "all". */
    @Query(
        """
        SELECT labourer_id AS labourerId,
               SUM(CASE WHEN type = 'WAGE' THEN amount_paise ELSE 0 END) AS wagePaise,
               SUM(CASE WHEN type = 'ADVANCE' THEN amount_paise ELSE 0 END) AS advancePaise
        FROM payments
        WHERE (:projectId IS NULL OR project_id = :projectId)
          AND (:workId IS NULL OR work_id = :workId)
        GROUP BY labourer_id
        """,
    )
    fun observeTotalsByLabourer(projectId: Long? = null, workId: Long? = null): Flow<List<LabourerPaymentRow>>

    @Query(
        """
        SELECT p.*, l.name AS labourerName FROM payments p
        INNER JOIN labourers l ON l.id = p.labourer_id
        WHERE p.work_id = :workId
        ORDER BY p.date ASC, p.id ASC
        """,
    )
    fun observeForWorkWithLabourer(workId: Long): Flow<List<PaymentWithLabourer>>

    @Insert
    suspend fun insert(payment: PaymentEntity): Long

    @Update
    suspend fun update(payment: PaymentEntity)

    @Delete
    suspend fun delete(payment: PaymentEntity)
}
