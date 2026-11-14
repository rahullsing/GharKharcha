package com.gharkharcha.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gharkharcha.data.entity.LabourerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LabourerDao {
    @Query("SELECT * FROM labourers ORDER BY is_active DESC, name COLLATE NOCASE")
    fun observeAll(): Flow<List<LabourerEntity>>

    @Query("SELECT * FROM labourers WHERE is_active = 1 ORDER BY name COLLATE NOCASE")
    fun observeActive(): Flow<List<LabourerEntity>>

    @Query("SELECT * FROM labourers WHERE id = :id")
    fun observe(id: Long): Flow<LabourerEntity?>

    @Query("SELECT * FROM labourers WHERE id = :id")
    suspend fun get(id: Long): LabourerEntity?

    /** Attendance or payment rows that block hard-deleting this labourer. */
    @Query(
        """
        SELECT (SELECT COUNT(*) FROM attendance WHERE labourer_id = :id) +
               (SELECT COUNT(*) FROM payments WHERE labourer_id = :id)
        """,
    )
    suspend fun countReferences(id: Long): Int

    @Query(
        """
        SELECT * FROM labourers
        WHERE name LIKE '%' || :query || '%'
           OR phone LIKE '%' || :query || '%'
           OR custom_skill LIKE '%' || :query || '%'
        ORDER BY is_active DESC, name COLLATE NOCASE
        """,
    )
    fun search(query: String): Flow<List<LabourerEntity>>

    @Insert
    suspend fun insert(labourer: LabourerEntity): Long

    @Update
    suspend fun update(labourer: LabourerEntity)

    @Delete
    suspend fun delete(labourer: LabourerEntity)
}
