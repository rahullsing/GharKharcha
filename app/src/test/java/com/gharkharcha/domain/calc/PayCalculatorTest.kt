package com.gharkharcha.domain.calc

import com.gharkharcha.data.entity.AttendanceStatus.ABSENT
import com.gharkharcha.data.entity.AttendanceStatus.FULL_DAY
import com.gharkharcha.data.entity.AttendanceStatus.HALF_DAY
import com.gharkharcha.data.entity.AttendanceStatus.HOURS
import com.gharkharcha.data.entity.AttendanceStatus.OVERTIME
import com.gharkharcha.data.entity.RateType.DAILY
import com.gharkharcha.data.entity.RateType.HOURLY
import org.junit.Assert.assertEquals
import org.junit.Test

class PayCalculatorTest {
    private val hpd = 8

    @Test fun `daily rate`() {
        assertEquals(700_00L, PayCalculator.basePay(FULL_DAY, DAILY, 700_00, 480, hpd))
        assertEquals(350_00L, PayCalculator.basePay(HALF_DAY, DAILY, 700_00, 240, hpd))
        assertEquals(0L, PayCalculator.basePay(ABSENT, DAILY, 700_00, 0, hpd))
        assertEquals(700_00L, PayCalculator.basePay(OVERTIME, DAILY, 700_00, 480, hpd))
    }

    @Test fun `half day of odd paise rounds half up`() {
        assertEquals(50L, PayCalculator.basePay(HALF_DAY, DAILY, 99, 240, hpd)) // 49.5 → 50
    }

    @Test fun `hours on daily rate is pro-rata of hours per day`() {
        // 6h of an 8h ₹800 day = ₹600
        assertEquals(600_00L, PayCalculator.basePay(HOURS, DAILY, 800_00, 360, hpd))
        // 5h of a 9h ₹700 day = 388.888… → ₹388.89
        assertEquals(388_89L, PayCalculator.basePay(HOURS, DAILY, 700_00, 300, 9))
    }

    @Test fun `hourly rate`() {
        assertEquals(800_00L, PayCalculator.basePay(FULL_DAY, HOURLY, 100_00, 480, hpd))
        assertEquals(400_00L, PayCalculator.basePay(HALF_DAY, HOURLY, 100_00, 240, hpd))
        // 7h30m at ₹90/h = ₹675
        assertEquals(675_00L, PayCalculator.basePay(HOURS, HOURLY, 90_00, 450, hpd))
    }

    @Test fun `overtime pay`() {
        assertEquals(300_00L, PayCalculator.overtimePay(120, 150_00))
        assertEquals(37_50L, PayCalculator.overtimePay(15, 150_00))
        assertEquals(0L, PayCalculator.overtimePay(-30, 150_00))
    }

    @Test fun `minutes credited`() {
        assertEquals(480, PayCalculator.minutesFor(FULL_DAY, 0, 8))
        assertEquals(480, PayCalculator.minutesFor(OVERTIME, 0, 8))
        assertEquals(270, PayCalculator.minutesFor(HALF_DAY, 0, 9))
        assertEquals(330, PayCalculator.minutesFor(HOURS, 330, 8))
        assertEquals(0, PayCalculator.minutesFor(ABSENT, 300, 8))
    }

    @Test fun `day hundredths`() {
        // 3 full + 2 half + 4h (of 8h) = 3 + 1 + 0.5 = 4.5 days
        assertEquals(450L, PayCalculator.dayHundredths(3, 2, 240, 8))
        // hours ÷ configurable hours per day: 5h of 10h = 0.5
        assertEquals(50L, PayCalculator.dayHundredths(0, 0, 300, 10))
        assertEquals(0L, PayCalculator.dayHundredths(0, 0, 0, 8))
    }

    @Test fun `divRound handles negatives symmetrically`() {
        assertEquals(2L, PayCalculator.divRound(3, 2))
        assertEquals(-2L, PayCalculator.divRound(-3, 2))
        assertEquals(1L, PayCalculator.divRound(4, 3))
    }

    @Test fun `material total`() {
        // 2.5 bags × ₹420 = ₹1,050
        assertEquals(1_050_00L, MaterialCalculator.total(2_500, 420_00))
        // 90 m × ₹18.75 = ₹1,687.50
        assertEquals(1_687_50L, MaterialCalculator.total(90_000, 18_75))
        // 0.333 kg × ₹10 = 3.33
        assertEquals(3_33L, MaterialCalculator.total(333, 10_00))
    }
}
