package com.gharkharcha.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/** Money given to a labourer, optionally attributed to a project / work. */
@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = LabourerEntity::class,
            parentColumns = ["id"],
            childColumns = ["labourer_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = WorkEntity::class,
            parentColumns = ["id"],
            childColumns = ["work_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("labourer_id"), Index("project_id"), Index("work_id"), Index("date")],
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "labourer_id") val labourerId: Long,
    @ColumnInfo(name = "project_id") val projectId: Long? = null,
    @ColumnInfo(name = "work_id") val workId: Long? = null,
    val date: LocalDate,
    @ColumnInfo(name = "amount_paise") val amountPaise: Long,
    val type: PaymentType = PaymentType.WAGE,
    val mode: PaymentMode = PaymentMode.CASH,
    /** UTR / cheque number. */
    val reference: String = "",
    @ColumnInfo(name = "period_from") val periodFrom: LocalDate? = null,
    @ColumnInfo(name = "period_to") val periodTo: LocalDate? = null,
    val notes: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
)
