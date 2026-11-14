package com.gharkharcha.domain.calc

import com.gharkharcha.data.entity.AttendanceStatus
import com.gharkharcha.data.entity.RateType

/**
 * Pure pay arithmetic. All money in paise, time in minutes; rounding is half-up to the paisa.
 */
object PayCalculator {

    const val DEFAULT_HOURS_PER_DAY = 8

    /** Minutes credited for an entry (stored in AttendanceEntity.minutesWorked). */
    fun minutesFor(status: AttendanceStatus, enteredMinutes: Int, hoursPerDay: Int): Int {
        val fullDay = hoursPerDay * 60
        return when (status) {
            AttendanceStatus.FULL_DAY, AttendanceStatus.OVERTIME -> fullDay
            AttendanceStatus.HALF_DAY -> fullDay / 2
            AttendanceStatus.HOURS -> enteredMinutes.coerceAtLeast(0)
            AttendanceStatus.ABSENT -> 0
        }
    }

    /** Base pay excluding overtime. */
    fun basePay(
        status: AttendanceStatus,
        rateType: RateType,
        ratePaise: Long,
        minutesWorked: Int,
        hoursPerDay: Int,
    ): Long {
        require(hoursPerDay > 0) { "hoursPerDay must be positive" }
        val dayRate = when (rateType) {
            RateType.DAILY -> ratePaise
            RateType.HOURLY -> ratePaise * hoursPerDay
        }
        return when (status) {
            AttendanceStatus.ABSENT -> 0
            AttendanceStatus.FULL_DAY, AttendanceStatus.OVERTIME -> dayRate
            AttendanceStatus.HALF_DAY -> divRound(dayRate, 2)
            AttendanceStatus.HOURS -> when (rateType) {
                RateType.HOURLY -> divRound(ratePaise * minutesWorked, 60)
                RateType.DAILY -> divRound(ratePaise * minutesWorked, hoursPerDay * 60L)
            }
        }
    }

    /** Overtime pay at an hourly [overtimeRatePaise]. */
    fun overtimePay(overtimeMinutes: Int, overtimeRatePaise: Long): Long =
        divRound(overtimeRatePaise * overtimeMinutes.coerceAtLeast(0), 60)

    /** Day credit in hundredths: full = 100, half = 50, hours = minutes / (hoursPerDay·60). */
    fun dayHundredths(fullDays: Int, halfDays: Int, hourlyMinutes: Long, hoursPerDay: Int): Long {
        require(hoursPerDay > 0) { "hoursPerDay must be positive" }
        return fullDays * 100L + halfDays * 50L + divRound(hourlyMinutes * 100, hoursPerDay * 60L)
    }

    /** Integer division rounding half away from zero. */
    internal fun divRound(numerator: Long, denominator: Long): Long {
        require(denominator > 0)
        val half = denominator / 2
        return if (numerator >= 0) (numerator + half) / denominator else -((-numerator + half) / denominator)
    }

    private fun divRound(numerator: Long, denominator: Int): Long = divRound(numerator, denominator.toLong())
}
