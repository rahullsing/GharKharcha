package com.gharkharcha.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gharkharcha.data.entity.VendorEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VendorDao {
    @Query("SELECT * FROM vendors ORDER BY name")
    fun observeAll(): Flow<List<VendorEntity>>

    @Query("SELECT * FROM vendors WHERE name LIKE :query || '%' ORDER BY name LIMIT :limit")
    suspend fun suggest(query: String, limit: Int = 8): List<VendorEntity>

    @Query("SELECT * FROM vendors WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): VendorEntity?

    @Query("SELECT * FROM vendors WHERE id = :id")
    suspend fun get(id: Long): VendorEntity?

    /** Returns -1 when a vendor with the same name (case-insensitive) already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(vendor: VendorEntity): Long

    @Update
    suspend fun update(vendor: VendorEntity)

    @Delete
    suspend fun delete(vendor: VendorEntity)
}
