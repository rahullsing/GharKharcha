package com.gharkharcha.domain.calc

import com.gharkharcha.data.entity.AttendanceEntity
import com.gharkharcha.data.entity.AttendanceStatus
import com.gharkharcha.data.entity.LabourerEntity
import com.gharkharcha.data.entity.RateType
import com.gharkharcha.util.Money
import com.gharkharcha.util.Quantity
import kotlinx.serialization.Serializable
import java.time.LocalDate

/**
 * Editable form of one attendance entry (text fields as typed). [status] is never OVERTIME here:
 * overtime is simply "full day + overtime hours" and is derived when saving.
 */
@Serializable
data class AttendanceDraft(
    val status: AttendanceStatus = AttendanceStatus.FULL_DAY,
    val hours: String = "",
    val rateType: RateType = RateType.DAILY,
    val rate: String = "",
    /** Only meaningful when [payOverridden]. */
    val pay: String = "",
    val payOverridden: Boolean = false,
    val otHours: String = "",
    val otRate: String = "",
    val advance: String = "",
    val notes: String = "",
) {
    private val ratePaise: Long get() = amount(rate)

    val overtimeMinutes: Int
        get() = if (status == AttendanceStatus.ABSENT) 0 else Quantity.parseHoursToMinutes(otHours) ?: 0

    /** True when "Hours" is chosen but no hours are entered. */
    val hoursMissing: Boolean
        get() = status == AttendanceStatus.HOURS && (Quantity.parseHoursToMinutes(hours) ?: 0) <= 0

    fun minutes(hoursPerDay: Int): Int =
        PayCalculator.minutesFor(status, Quantity.parseHoursToMinutes(hours) ?: 0, hoursPerDay)

    fun autoBasePay(hoursPerDay: Int): Long =
        PayCalculator.basePay(status, rateType, ratePaise, minutes(hoursPerDay), hoursPerDay)

    fun basePay(hoursPerDay: Int): Long {
        val typed = if (payOverridden) Money.parse(pay)?.coerceAtLeast(0) else null
        return typed ?: autoBasePay(hoursPerDay)
    }

    fun overtimePay(): Long = PayCalculator.overtimePay(overtimeMinutes, amount(otRate))

    fun totalPay(hoursPerDay: Int): Long = basePay(hoursPerDay) + overtimePay()

    fun toEntity(
        existing: AttendanceEntity?,
        workId: Long,
        labourerId: Long,
        date: LocalDate,
        hoursPerDay: Int,
    ): AttendanceEntity {
        val finalStatus =
            if (status == AttendanceStatus.FULL_DAY && overtimeMinutes > 0) AttendanceStatus.OVERTIME else status
        val base = existing ?: AttendanceEntity(workId = workId, labourerId = labourerId, date = date, status = finalStatus)
        return base.copy(
            workId = workId,
            labourerId = labourerId,
            date = date,
            status = finalStatus,
            minutesWorked = minutes(hoursPerDay),
            rateType = rateType,
            ratePaise = ratePaise,
            basePayPaise = basePay(hoursPerDay),
            payOverridden = payOverridden && Money.parse(pay) != null,
            overtimeMinutes = overtimeMinutes,
            overtimeRatePaise = amount(otRate),
            overtimePayPaise = overtimePay(),
            advancePaise = amount(advance),
            notes = notes.trim(),
        )
    }

    private fun amount(text: String): Long = Money.parse(text)?.coerceAtLeast(0) ?: 0
}

object AttendanceDrafts {

    /** Daily wage if set, else hourly rate. */
    fun defaultRate(labourer: LabourerEntity): Pair<RateType, Long> = when {
        labourer.dailyWagePaise > 0 -> RateType.DAILY to labourer.dailyWagePaise
        labourer.hourlyRatePaise > 0 -> RateType.HOURLY to labourer.hourlyRatePaise
        else -> RateType.DAILY to 0L
    }

    /** Hourly rate if set, else daily wage ÷ hours per day. */
    fun defaultOvertimeRate(labourer: LabourerEntity, hoursPerDay: Int): Long =
        if (labourer.hourlyRatePaise > 0) {
            labourer.hourlyRatePaise
        } else {
            PayCalculator.divRound(labourer.dailyWagePaise, hoursPerDay.toLong())
        }

    /**
     * Draft for the editor. With no [entry] it is pre-filled from the labourer's default rates.
     * [presetStatus] switches status (and drops any manual pay override, since pay must be recalculated).
     */
    fun from(
        entry: AttendanceEntity?,
        labourer: LabourerEntity,
        hoursPerDay: Int,
        presetStatus: AttendanceStatus? = null,
    ): AttendanceDraft {
        val defaultOt = Money.toInput(defaultOvertimeRate(labourer, hoursPerDay))
        if (entry == null) {
            val (type, rate) = defaultRate(labourer)
            return AttendanceDraft(
                status = presetStatus ?: AttendanceStatus.FULL_DAY,
                rateType = type,
                rate = Money.toInput(rate),
                otRate = defaultOt,
            )
        }
        val entryStatus = if (entry.status == AttendanceStatus.OVERTIME) AttendanceStatus.FULL_DAY else entry.status
        return AttendanceDraft(
            status = presetStatus ?: entryStatus,
            hours = if (entry.status == AttendanceStatus.HOURS) Quantity.formatHours(entry.minutesWorked.toLong()) else "",
            rateType = entry.rateType,
            rate = Money.toInput(entry.ratePaise),
            pay = Money.toInput(entry.basePayPaise),
            payOverridden = entry.payOverridden && (presetStatus == null || presetStatus == entryStatus),
            otHours = if (entry.overtimeMinutes > 0) Quantity.formatHours(entry.overtimeMinutes.toLong()) else "",
            otRate = if (entry.overtimeRatePaise > 0) Money.toInput(entry.overtimeRatePaise) else defaultOt,
            advance = Money.toInput(entry.advancePaise),
            notes = entry.notes,
        )
    }

    /** One-tap marking: keeps rate, overtime, advance and notes of an existing entry; recalculates pay. */
    fun quick(
        existing: AttendanceEntity?,
        labourer: LabourerEntity,
        workId: Long,
        date: LocalDate,
        status: AttendanceStatus,
        hoursPerDay: Int,
    ): AttendanceEntity =
        from(existing, labourer, hoursPerDay, presetStatus = status)
            .toEntity(existing, workId, labourer.id, date, hoursPerDay)
}
