package com.gharkharcha.util

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * INR formatting/parsing on Long paise. Indian digit grouping (1,00,000) is done by hand so output
 * is identical on every device / JVM regardless of ICU data. Never uses Double.
 */
object Money {
    const val SYMBOL = "₹"

    /**
     * 12345600 → "₹1,23,456"; 12345650 → "₹1,23,456.50".
     * @param alwaysShowPaise force ".00" (used in PDFs / tables for alignment).
     */
    fun format(paise: Long, alwaysShowPaise: Boolean = false, withSymbol: Boolean = true): String {
        val negative = paise < 0
        // Long.MIN_VALUE can't be negated; it is not a realistic amount, clamp it.
        val abs = if (paise == Long.MIN_VALUE) Long.MAX_VALUE else kotlin.math.abs(paise)
        val rupees = abs / 100
        val fraction = (abs % 100).toInt()
        val sb = StringBuilder()
        if (negative) sb.append('-')
        if (withSymbol) sb.append(SYMBOL)
        sb.append(groupIndian(rupees))
        if (alwaysShowPaise || fraction != 0) {
            sb.append('.')
            if (fraction < 10) sb.append('0')
            sb.append(fraction)
        }
        return sb.toString()
    }

    /** Compact for chart labels: ₹950, ₹12.5K, ₹3.2L, ₹1.1Cr. */
    fun formatCompact(paise: Long): String {
        val rupees = paise / 100
        val abs = kotlin.math.abs(rupees)
        val sign = if (rupees < 0) "-" else ""
        fun oneDecimal(value: Long, unit: Long): String {
            val tenths = (value * 10 + unit / 2) / unit
            return if (tenths % 10 == 0L) "${tenths / 10}" else "${tenths / 10}.${tenths % 10}"
        }
        return when {
            abs >= 1_00_00_000 -> "$sign$SYMBOL${oneDecimal(abs, 1_00_00_000)}Cr"
            abs >= 1_00_000 -> "$sign$SYMBOL${oneDecimal(abs, 1_00_000)}L"
            abs >= 1_000 -> "$sign$SYMBOL${oneDecimal(abs, 1_000)}K"
            else -> "$sign$SYMBOL$abs"
        }
    }

    /** "1,23,456" grouping: last 3 digits, then pairs. */
    fun groupIndian(value: Long): String {
        val digits = value.toString()
        if (digits.length <= 3) return digits
        val last3 = digits.takeLast(3)
        val rest = digits.dropLast(3)
        val sb = StringBuilder()
        val firstGroupLen = rest.length % 2
        if (firstGroupLen == 1) sb.append(rest[0])
        var i = firstGroupLen
        while (i < rest.length) {
            if (sb.isNotEmpty()) sb.append(',')
            sb.append(rest, i, i + 2)
            i += 2
        }
        return sb.append(',').append(last3).toString()
    }

    /**
     * Parses user input like "1,23,456.5", "₹ 450", "450.255" (rounded half-up) into paise.
     * Returns null for blank/invalid input.
     */
    fun parse(input: String): Long? {
        val cleaned = input.trim().replace(SYMBOL, "").replace(",", "").replace(" ", "")
        if (cleaned.isEmpty()) return null
        return try {
            BigDecimal(cleaned).setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()
        } catch (_: NumberFormatException) {
            null
        } catch (_: ArithmeticException) {
            null
        }
    }

    /** Paise → editable text for input fields ("1250.5" style, no grouping, no symbol). */
    fun toInput(paise: Long): String {
        if (paise == 0L) return ""
        return BigDecimal.valueOf(paise, 2).stripTrailingZeros().toPlainString()
    }
}
