package com.gharkharcha.data.dao

import androidx.room.Embedded
import com.gharkharcha.data.entity.AttendanceEntity
import com.gharkharcha.data.entity.ProjectEntity
import com.gharkharcha.domain.model.AttendanceTotals
import com.gharkharcha.domain.model.PaymentTotals

/** Project with its work count and spend (projects list). */
data class ProjectSummaryRow(
    @Embedded val project: ProjectEntity,
    val workCount: Int,
    val materialPaise: Long,
    val labourPaise: Long,
)

/** Spend per work, used by work list / project totals. */
data class WorkSpendRow(
    val workId: Long,
    val materialPaise: Long,
    val labourPaise: Long,
)

/** Aggregated attendance for one labourer (optionally filtered to a project / work). */
data class LabourerAttendanceRow(
    val labourerId: Long,
    val fullDays: Int,
    val halfDays: Int,
    val hourlyMinutes: Long,
    val totalMinutes: Long,
    val earnedPaise: Long,
    val advancePaise: Long,
) {
    fun toTotals() = AttendanceTotals(
        fullDays = fullDays,
        halfDays = halfDays,
        hourlyMinutes = hourlyMinutes,
        totalMinutes = totalMinutes,
        earnedPaise = earnedPaise,
        advancePaise = advancePaise,
    )
}

/** Aggregated payments for one labourer. */
data class LabourerPaymentRow(
    val labourerId: Long,
    val wagePaise: Long,
    val advancePaise: Long,
) {
    fun toTotals() = PaymentTotals(wagePaise = wagePaise, advancePaise = advancePaise)
}

data class OwnerCount(val ownerId: Long, val count: Int)

/** Attendance entry with its work name, for the labourer ledger. */
data class AttendanceWithWork(
    @Embedded val attendance: AttendanceEntity,
    val workName: String,
)

/** Attendance entry with work and labourer name, for recent activity. */
data class AttendanceActivityRow(
    @Embedded val attendance: AttendanceEntity,
    val workName: String,
    val labourerName: String,
)

data class DailySpendRow(
    val date: java.time.LocalDate,
    val totalPaise: Long,
)

/** Attendance entry with labourer details, for work reports. */
data class AttendanceWithLabourer(
    @Embedded val attendance: AttendanceEntity,
    val labourerName: String,
    val labourerSkill: String,
)

/** Payment entry with labourer details, for work reports. */
data class PaymentWithLabourer(
    @Embedded val payment: com.gharkharcha.data.entity.PaymentEntity,
    val labourerName: String,
)
