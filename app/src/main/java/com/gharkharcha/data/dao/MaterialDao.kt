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

    @Insert
    suspend fun insert(material: MaterialEntity): Long

    @Update
    suspend fun update(material: MaterialEntity)

    @Delete
    suspend fun delete(material: MaterialEntity)
}
