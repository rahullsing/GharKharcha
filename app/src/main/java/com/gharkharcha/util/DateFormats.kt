package com.gharkharcha.util

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** App-wide date formats (en-IN). Display: 14-Nov-2026. */
object DateFormats {
    val LOCALE: Locale = Locale.forLanguageTag("en-IN")

    private val display = DateTimeFormatter.ofPattern("dd-MMM-yyyy", LOCALE)
    private val month = DateTimeFormatter.ofPattern("MMM yyyy", LOCALE)
    private val dayShort = DateTimeFormatter.ofPattern("dd MMM", LOCALE)
    private val compact = DateTimeFormatter.ofPattern("dd MMM yy", LOCALE)
    private val fileStamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm", LOCALE)

    fun format(date: LocalDate): String = display.format(date)
    fun format(month: YearMonth): String = this.month.format(month)
    fun formatShort(date: LocalDate): String = dayShort.format(date)

    /** For narrow table columns: 14 Nov 26. */
    fun formatCompact(date: LocalDate): String = compact.format(date)

    fun formatEpochMillis(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        display.format(Instant.ofEpochMilli(millis).atZone(zone).toLocalDate())

    /** Timestamp for exported file names, e.g. 20261114-1830. */
    fun fileStamp(millis: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): String =
        fileStamp.format(Instant.ofEpochMilli(millis).atZone(zone))
}
