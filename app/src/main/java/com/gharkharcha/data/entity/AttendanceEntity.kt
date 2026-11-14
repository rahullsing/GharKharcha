package com.gharkharcha.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * One labourer, one work, one day. Rates are snapshotted so editing the labourer's
 * default wage never rewrites history.
 *
 * [minutesWorked] is always filled (full day = hours-per-day × 60 at save time) so
 * hour totals can be summed in SQL. [basePayPaise] may be a user override.
 */
@Entity(
    tableName = "attendance",
    foreignKeys = [
        ForeignKey(
            entity = WorkEntity::class,
            parentColumns = ["id"],
            childColumns = ["work_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = LabourerEntity::class,
            parentColumns = ["id"],
            childColumns = ["labourer_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["work_id", "labourer_id", "date"], unique = true),
        Index("labourer_id"),
        Index("date"),
    ],
)
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "work_id") val workId: Long,
    @ColumnInfo(name = "labourer_id") val labourerId: Long,
    val date: LocalDate,
    val status: AttendanceStatus,
    @ColumnInfo(name = "minutes_worked") val minutesWorked: Int = 0,
    @ColumnInfo(name = "rate_type") val rateType: RateType = RateType.DAILY,
    @ColumnInfo(name = "rate_paise") val ratePaise: Long = 0,
    @ColumnInfo(name = "base_pay_paise") val basePayPaise: Long = 0,
    @ColumnInfo(name = "pay_overridden") val payOverridden: Boolean = false,
    @ColumnInfo(name = "overtime_minutes") val overtimeMinutes: Int = 0,
    /** Overtime rate per hour. */
    @ColumnInfo(name = "overtime_rate_paise") val overtimeRatePaise: Long = 0,
    @ColumnInfo(name = "overtime_pay_paise") val overtimePayPaise: Long = 0,
    @ColumnInfo(name = "advance_paise") val advancePaise: Long = 0,
    val notes: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
) {
    val totalPayPaise: Long get() = basePayPaise + overtimePayPaise
}
