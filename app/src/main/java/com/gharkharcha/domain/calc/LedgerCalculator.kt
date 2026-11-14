package com.gharkharcha.domain.calc

import com.gharkharcha.data.entity.AttendanceEntity
import com.gharkharcha.data.entity.AttendanceStatus
import com.gharkharcha.data.entity.PaymentEntity
import com.gharkharcha.data.entity.PaymentType
import com.gharkharcha.domain.model.AttendanceTotals
import com.gharkharcha.domain.model.LabourSummary
import com.gharkharcha.domain.model.PaymentTotals

/** Builds [LabourSummary] from SQL aggregates or from raw rows (same result). */
object LedgerCalculator {

    fun summarize(attendance: AttendanceTotals, payments: PaymentTotals, hoursPerDay: Int): LabourSummary =
        LabourSummary(
            dayHundredths = PayCalculator.dayHundredths(
                attendance.fullDays, attendance.halfDays, attendance.hourlyMinutes, hoursPerDay,
            ),
            totalMinutes = attendance.totalMinutes,
            earnedPaise = attendance.earnedPaise,
            wagesPaidPaise = payments.wagePaise,
            advancesPaise = payments.advancePaise + attendance.advancePaise,
        )

    fun totalsOf(entries: List<AttendanceEntity>): AttendanceTotals {
        var full = 0
        var half = 0
        var hourly = 0L
        var minutes = 0L
        var earned = 0L
        var advance = 0L
        for (e in entries) {
            when (e.status) {
                AttendanceStatus.FULL_DAY, AttendanceStatus.OVERTIME -> full++
                AttendanceStatus.HALF_DAY -> half++
                AttendanceStatus.HOURS -> hourly += e.minutesWorked
                AttendanceStatus.ABSENT -> Unit
            }
            if (e.status != AttendanceStatus.ABSENT) minutes += e.minutesWorked
            minutes += e.overtimeMinutes
            earned += e.totalPayPaise
            advance += e.advancePaise
        }
        return AttendanceTotals(full, half, hourly, minutes, earned, advance)
    }

    fun totalsOfPayments(payments: List<PaymentEntity>): PaymentTotals = PaymentTotals(
        wagePaise = payments.filter { it.type == PaymentType.WAGE }.sumOf { it.amountPaise },
        advancePaise = payments.filter { it.type == PaymentType.ADVANCE }.sumOf { it.amountPaise },
    )

    fun summarize(entries: List<AttendanceEntity>, payments: List<PaymentEntity>, hoursPerDay: Int): LabourSummary =
        summarize(totalsOf(entries), totalsOfPayments(payments), hoursPerDay)
}
