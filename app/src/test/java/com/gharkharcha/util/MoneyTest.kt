package com.gharkharcha.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {

    @Test fun `indian grouping`() {
        assertEquals("0", Money.groupIndian(0))
        assertEquals("999", Money.groupIndian(999))
        assertEquals("1,000", Money.groupIndian(1_000))
        assertEquals("10,000", Money.groupIndian(10_000))
        assertEquals("1,00,000", Money.groupIndian(1_00_000))
        assertEquals("12,34,567", Money.groupIndian(12_34_567))
        assertEquals("1,23,45,678", Money.groupIndian(1_23_45_678))
        assertEquals("10,00,00,000", Money.groupIndian(10_00_00_000))
    }

    @Test fun `format whole rupees hides paise`() {
        assertEquals("₹0", Money.format(0))
        assertEquals("₹4,500", Money.format(4_500_00))
        assertEquals("₹1,00,000", Money.format(1_00_000_00))
    }

    @Test fun `format shows paise when present or forced`() {
        assertEquals("₹1,234.50", Money.format(1_234_50))
        assertEquals("₹0.05", Money.format(5))
        assertEquals("₹700.00", Money.format(700_00, alwaysShowPaise = true))
        assertEquals("1,234.50", Money.format(1_234_50, withSymbol = false))
    }

    @Test fun `format negative`() {
        assertEquals("-₹2,500", Money.format(-2_500_00))
        assertEquals("-₹0.50", Money.format(-50))
    }

    @Test fun `compact format`() {
        assertEquals("₹950", Money.formatCompact(950_00))
        assertEquals("₹12.5K", Money.formatCompact(12_500_00))
        assertEquals("₹3L", Money.formatCompact(3_00_000_00))
        assertEquals("₹3.2L", Money.formatCompact(3_20_000_00))
        assertEquals("₹1.1Cr", Money.formatCompact(1_10_00_000_00))
    }

    @Test fun `parse valid input`() {
        assertEquals(4_500_00L, Money.parse("4500"))
        assertEquals(1_23_456_50L, Money.parse("1,23,456.5"))
        assertEquals(450_00L, Money.parse(" ₹ 450 "))
        assertEquals(10L, Money.parse("0.1"))
        assertEquals(45_026L, Money.parse("450.255")) // half-up
        assertEquals(-200_00L, Money.parse("-200"))
    }

    @Test fun `parse invalid input`() {
        assertNull(Money.parse(""))
        assertNull(Money.parse("   "))
        assertNull(Money.parse("abc"))
        assertNull(Money.parse("12.3.4"))
        assertNull(Money.parse("99999999999999999999999"))
    }

    @Test fun `to input round trips`() {
        assertEquals("", Money.toInput(0))
        assertEquals("700", Money.toInput(700_00))
        assertEquals("1250.5", Money.toInput(1_250_50))
        assertEquals(1_250_50L, Money.parse(Money.toInput(1_250_50)))
    }
}
