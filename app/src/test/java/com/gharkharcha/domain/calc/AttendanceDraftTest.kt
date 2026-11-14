package com.gharkharcha.domain.calc

import com.gharkharcha.data.entity.AttendanceStatus
import com.gharkharcha.data.entity.LabourerEntity
import com.gharkharcha.data.entity.RateType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AttendanceDraftTest {
    private val day = LocalDate.of(2026, 11, 14)
    private val mason = LabourerEntity(id = 7, name = "Ramesh", dailyWagePaise = 700_00)
    private val hourly = LabourerEntity(id = 8, name = "Suresh", hourlyRatePaise = 90_00)

    private fun quick(labourer: LabourerEntity, status: AttendanceStatus, existing: com.gharkharcha.data.entity.AttendanceEntity? = null) =
        AttendanceDrafts.quick(existing, labourer, workId = 3, date = day, status = status, hoursPerDay = 8)

    @Test fun `new draft is pre-filled from the labourer`() {
        val d = AttendanceDrafts.from(null, mason, 8)
        assertEquals(AttendanceStatus.FULL_DAY, d.status)
        assertEquals(RateType.DAILY, d.rateType)
        assertEquals("700", d.rate)
        assertEquals("87.5", d.otRate) // 700 ÷ 8
        assertEquals(700_00L, d.totalPay(8))
    }

    @Test fun `one tap full and half day`() {
        val full = quick(mason, AttendanceStatus.FULL_DAY)
        assertEquals(700_00L, full.basePayPaise)
        assertEquals(480, full.minutesWorked)
        assertEquals(7L, full.labourerId)
        assertEquals(3L, full.workId)

        val half = quick(mason, AttendanceStatus.HALF_DAY, existing = full.copy(id = 11))
        assertEquals(11L, half.id)
        assertEquals(350_00L, half.basePayPaise)
        assertEquals(240, half.minutesWorked)
    }

    @Test fun `labourer with only an hourly rate`() {
        val full = quick(hourly, AttendanceStatus.FULL_DAY)
        assertEquals(RateType.HOURLY, full.rateType)
        assertEquals(720_00L, full.basePayPaise)
        assertEquals(90_00L, AttendanceDrafts.defaultOvertimeRate(hourly, 8))
    }

    @Test fun `hours entry`() {
        val d = AttendanceDrafts.from(null, mason.copy(dailyWagePaise = 800_00), 8, AttendanceStatus.HOURS)
        assertTrue(d.hoursMissing)
        val filled = d.copy(hours = "5")
        assertFalse(filled.hoursMissing)
        val e = filled.toEntity(null, 3, 7, day, 8)
        assertEquals(AttendanceStatus.HOURS, e.status)
        assertEquals(300, e.minutesWorked)
        assertEquals(500_00L, e.basePayPaise)
    }

    @Test fun `overtime hours turn a full day into overtime`() {
        val e = AttendanceDrafts.from(null, mason, 8).copy(otHours = "2", otRate = "100")
            .toEntity(null, 3, 7, day, 8)
        assertEquals(AttendanceStatus.OVERTIME, e.status)
        assertEquals(120, e.overtimeMinutes)
        assertEquals(200_00L, e.overtimePayPaise)
        assertEquals(900_00L, e.totalPayPaise)

        // Editing it again shows "Full day" with the overtime kept.
        val again = AttendanceDrafts.from(e, mason, 8)
        assertEquals(AttendanceStatus.FULL_DAY, again.status)
        assertEquals("2", again.otHours)
        assertEquals(e, again.toEntity(e, 3, 7, day, 8))
    }

    @Test fun `manual pay override is kept until status changes`() {
        val e = AttendanceDrafts.from(null, mason, 8).copy(pay = "650", payOverridden = true, advance = "200")
            .toEntity(null, 3, 7, day, 8)
        assertEquals(650_00L, e.basePayPaise)
        assertTrue(e.payOverridden)
        assertEquals(200_00L, e.advancePaise)

        assertTrue(AttendanceDrafts.from(e, mason, 8).payOverridden)
        val half = quick(mason, AttendanceStatus.HALF_DAY, existing = e)
        assertFalse(half.payOverridden)
        assertEquals(350_00L, half.basePayPaise)
        assertEquals(200_00L, half.advancePaise) // advance survives a status change
    }

    @Test fun `absent has no pay or overtime`() {
        val ot = AttendanceDrafts.from(null, mason, 8).copy(otHours = "2").toEntity(null, 3, 7, day, 8)
        val absent = quick(mason, AttendanceStatus.ABSENT, existing = ot)
        assertEquals(AttendanceStatus.ABSENT, absent.status)
        assertEquals(0L, absent.totalPayPaise)
        assertEquals(0, absent.overtimeMinutes)
        assertEquals(0, absent.minutesWorked)
    }

    @Test fun `garbage input is treated as zero`() {
        val d = AttendanceDraft(rate = "abc", otHours = "x", otRate = "-5", advance = "")
        assertEquals(0L, d.totalPay(8))
        assertEquals(0L, d.toEntity(null, 1, 1, day, 8).advancePaise)
    }
}
