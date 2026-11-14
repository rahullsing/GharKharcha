package com.gharkharcha.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class QuantityAndDateTest {

    @Test fun `quantity parse and format`() {
        assertEquals(2_500L, Quantity.parse("2.5"))
        assertEquals(90_000L, Quantity.parse("90"))
        assertEquals(1_235L, Quantity.parse("1.2345")) // rounded to 3 dp
        assertNull(Quantity.parse("x"))
        assertEquals("2.5", Quantity.format(2_500))
        assertEquals("90", Quantity.format(90_000))
        assertEquals("0.125", Quantity.format(125))
    }

    @Test fun `days and hours display`() {
        assertEquals("4.5", Quantity.formatDays(450))
        assertEquals("12", Quantity.formatDays(1_200))
        assertEquals("0.25", Quantity.formatDays(25))
        assertEquals("7.5", Quantity.formatHours(450))
        assertEquals("8", Quantity.formatHours(480))
        assertEquals(450, Quantity.parseHoursToMinutes("7.5"))
        assertNull(Quantity.parseHoursToMinutes("-1"))
    }

    @Test fun `date format dd-MMM-yyyy`() {
        assertEquals("14-Nov-2026", DateFormats.format(LocalDate.of(2026, 11, 14)))
        assertEquals("01-Jan-2027", DateFormats.format(LocalDate.of(2027, 1, 1)))
    }
}
