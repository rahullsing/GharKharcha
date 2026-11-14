package com.gharkharcha.util

import java.math.BigDecimal
import java.math.RoundingMode

/** Fixed-point quantity helpers (×1000, matches MaterialCalculator.QTY_SCALE), plus day/hour display. */
object Quantity {

    /** "2.5" → 2500; null if invalid. */
    fun parse(input: String): Long? {
        val cleaned = input.trim().replace(",", "")
        if (cleaned.isEmpty()) return null
        return try {
            BigDecimal(cleaned).setScale(3, RoundingMode.HALF_UP).movePointRight(3).longValueExact()
        } catch (_: NumberFormatException) {
            null
        } catch (_: ArithmeticException) {
            null
        }
    }

    /** 2500 → "2.5"; 3000 → "3". */
    fun format(milli: Long): String = BigDecimal.valueOf(milli, 3).stripTrailingZeros().toPlainString()

    /** Day hundredths → "4.5" / "12" / "0.25". */
    fun formatDays(hundredths: Long): String =
        BigDecimal.valueOf(hundredths, 2).stripTrailingZeros().toPlainString()

    /** Minutes → "7.5" hours (2 decimals max). */
    fun formatHours(minutes: Long): String =
        BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP)
            .stripTrailingZeros().toPlainString()

    /** "7.5" hours → 450 minutes. */
    fun parseHoursToMinutes(input: String): Int? {
        val cleaned = input.trim()
        if (cleaned.isEmpty()) return null
        return try {
            BigDecimal(cleaned).multiply(BigDecimal.valueOf(60)).setScale(0, RoundingMode.HALF_UP).intValueExact()
                .takeIf { it >= 0 }
        } catch (_: NumberFormatException) {
            null
        } catch (_: ArithmeticException) {
            null
        }
    }
}
