package com.gharkharcha.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gharkharcha.data.entity.MaterialEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MaterialDao {
    @Query("SELECT * FROM materials WHERE work_id = :workId ORDER BY purchase_date DESC, id DESC")
    fun observeByWork(workId: Long): Flow<List<MaterialEntity>>

    @Query("SELECT * FROM materials WHERE id = :id")
    fun observe(id: Long): Flow<MaterialEntity?>

    @Query("SELECT * FROM materials WHERE id = :id")
    suspend fun get(id: Long): MaterialEntity?

    @Query(
        """
        SELECT DISTINCT brand FROM materials
        WHERE brand != '' AND brand LIKE :query || '%'
        ORDER BY brand LIMIT :limit
        """,
    )
    suspend fun suggestBrands(query: String, limit: Int = 8): List<String>

    @Query(
        """
        SELECT DISTINCT item_name FROM materials
        WHERE item_name != '' AND item_name LIKE :query || '%'
        ORDER BY item_name LIMIT :limit
        """,
    )
    suspend fun suggestItems(query: String, limit: Int = 8): List<String>

    @Query(
        """
        SELECT m.* FROM materials m
        INNER JOIN works w ON m.work_id = w.id
        WHERE w.project_id = :projectId
        ORDER BY m.purchase_date DESC, m.id DESC
        """,
    )
    fun observeByProject(projectId: Long): Flow<List<MaterialEntity>>

    @Query(
        """
        SELECT m.* FROM materials m
        INNER JOIN works w ON m.work_id = w.id
        WHERE w.project_id = :projectId
        ORDER BY m.purchase_date DESC, m.id DESC
        LIMIT :limit
        """,
    )
    fun observeRecentByProject(projectId: Long, limit: Int = 10): Flow<List<MaterialEntity>>

    @Query(
        """
        SELECT m.purchase_date AS date, SUM(m.total_paise) AS totalPaise
        FROM materials m
        INNER JOIN works w ON m.work_id = w.id
        WHERE w.project_id = :projectId AND m.purchase_date >= :sinceDate
        GROUP BY m.purchase_date
        """,
    )
    fun observeDailySpend(projectId: Long, sinceDate: java.time.LocalDate): Flow<List<DailySpendRow>>

    @Query(
        """
        SELECT m.purchase_date AS date, SUM(m.total_paise) AS totalPaise
        FROM materials m
        INNER JOIN works w ON m.work_id = w.id
        WHERE w.project_id = :projectId
        GROUP BY m.purchase_date
        """,
    )
    fun observeAllDailySpend(projectId: Long): Flow<List<DailySpendRow>>

    @Query(
        """
        SELECT m.* FROM materials m
        INNER JOIN works w ON m.work_id = w.id
        WHERE w.project_id = :projectId
          AND (m.item_name LIKE '%' || :query || '%'
               OR m.brand LIKE '%' || :query || '%'
               OR m.shop_name LIKE '%' || :query || '%'
               OR m.notes LIKE '%' || :query || '%')
        ORDER BY m.purchase_date DESC
        """,
    )
    fun search(projectId: Long, query: String): Flow<List<MaterialEntity>>

    @Query(
        """
        SELECT * FROM materials
        WHERE item_name LIKE '%' || :query || '%'
           OR brand LIKE '%' || :query || '%'
           OR shop_name LIKE '%' || :query || '%'
           OR notes LIKE '%' || :query || '%'
        ORDER BY purchase_date DESC
        """,
    )
    fun searchAll(query: String): Flow<List<MaterialEntity>>

    @Insert
    suspend fun insert(material: MaterialEntity): Long

    @Update
    suspend fun update(material: MaterialEntity)

    @Delete
    suspend fun delete(material: MaterialEntity)
}
