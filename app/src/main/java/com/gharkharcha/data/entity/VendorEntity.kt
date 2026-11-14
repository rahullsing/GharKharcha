package com.gharkharcha.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Optional shop / supplier master used for auto-suggest on materials. */
@Entity(
    tableName = "vendors",
    indices = [Index(value = ["name"], unique = true)],
)
data class VendorEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val name: String,
    val phone: String = "",
    val address: String = "",
    val gstin: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
)
