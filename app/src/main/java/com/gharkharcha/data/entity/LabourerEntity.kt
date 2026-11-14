package com.gharkharcha.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Global labourer master, reusable across works and projects. */
@Entity(
    tableName = "labourers",
    indices = [Index("is_active")],
)
data class LabourerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val skill: Skill = Skill.HELPER,
    @ColumnInfo(name = "custom_skill") val customSkill: String = "",
    @ColumnInfo(name = "daily_wage_paise") val dailyWagePaise: Long = 0,
    @ColumnInfo(name = "hourly_rate_paise") val hourlyRatePaise: Long = 0,
    val address: String = "",
    @ColumnInfo(name = "id_notes") val idNotes: String = "",
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
)
