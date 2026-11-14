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

    @Insert
    suspend fun insert(payment: PaymentEntity): Long

    @Update
    suspend fun update(payment: PaymentEntity)

    @Delete
    suspend fun delete(payment: PaymentEntity)
}
