package com.gharkharcha.data.dao

import android.database.sqlite.SQLiteConstraintException
import com.gharkharcha.data.entity.AttendanceEntity
import com.gharkharcha.data.entity.AttendanceStatus
import com.gharkharcha.data.entity.PaymentEntity
import com.gharkharcha.data.entity.PaymentType
import com.gharkharcha.domain.calc.LedgerCalculator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class LabourDaoTest : DaoTestBase() {
    private val d1 = LocalDate.of(2026, 11, 2)

    private fun att(
        workId: Long,
        labourerId: Long,
        date: LocalDate,
        status: AttendanceStatus,
        minutes: Int,
        base: Long,
        otMin: Int = 0,
        otPay: Long = 0,
        advance: Long = 0,
    ) = AttendanceEntity(
        workId = workId, labourerId = labourerId, date = date, status = status, minutesWorked = minutes,
        basePayPaise = base, overtimeMinutes = otMin, overtimePayPaise = otPay, advancePaise = advance,
    )

    @Test(expected = SQLiteConstraintException::class)
    fun `one entry per work labourer and day`() = runTest {
        val w = newWork(newProject())
        val l = newLabourer()
        db.attendanceDao().insert(att(w, l, d1, AttendanceStatus.FULL_DAY, 480, 700_00))
        db.attendanceDao().insert(att(w, l, d1, AttendanceStatus.HALF_DAY, 240, 350_00))
    }

    @Test fun `same labourer can split a day across two works`() = runTest {
        val p = newProject()
        val w1 = newWork(p, "A")
        val w2 = newWork(p, "B")
        val l = newLabourer()
        db.attendanceDao().insert(att(w1, l, d1, AttendanceStatus.HALF_DAY, 240, 350_00))
        db.attendanceDao().insert(att(w2, l, d1, AttendanceStatus.HALF_DAY, 240, 350_00))
        assertEquals(2, db.attendanceDao().observeForLabourer(l).first().size)
    }

    @Test(expected = SQLiteConstraintException::class)
    fun `labourer with history cannot be deleted`() = runTest {
        val w = newWork(newProject())
        val l = newLabourer()
        db.attendanceDao().insert(att(w, l, d1, AttendanceStatus.FULL_DAY, 480, 700_00))
        assertEquals(1, db.labourerDao().countReferences(l))
        db.labourerDao().delete(db.labourerDao().get(l)!!)
    }

    @Test fun `SQL totals match the Kotlin calculator`() = runTest {
        val p = newProject()
        val w1 = newWork(p, "A")
        val w2 = newWork(p, "B")
        val ramesh = newLabourer("Ramesh")
        val suresh = newLabourer("Suresh")
        val entries = listOf(
            att(w1, ramesh, d1, AttendanceStatus.FULL_DAY, 480, 700_00, advance = 100_00),
            att(w1, ramesh, d1.plusDays(1), AttendanceStatus.OVERTIME, 480, 700_00, otMin = 120, otPay = 200_00),
            att(w2, ramesh, d1.plusDays(2), AttendanceStatus.HOURS, 300, 437_50),
            att(w1, ramesh, d1.plusDays(3), AttendanceStatus.ABSENT, 0, 0),
            att(w1, suresh, d1, AttendanceStatus.HALF_DAY, 240, 250_00),
        )
        entries.forEach { db.attendanceDao().insert(it) }
        val payments = listOf(
            PaymentEntity(labourerId = ramesh, projectId = p, workId = w1, date = d1, amountPaise = 1_000_00),
            PaymentEntity(labourerId = ramesh, projectId = p, date = d1, amountPaise = 300_00, type = PaymentType.ADVANCE),
        )
        payments.forEach { db.paymentDao().insert(it) }

        val attRow = db.attendanceDao().observeTotalsByLabourer().first().single { it.labourerId == ramesh }
        val payRow = db.paymentDao().observeTotalsByLabourer().first().single { it.labourerId == ramesh }
        val fromSql = LedgerCalculator.summarize(attRow.toTotals(), payRow.toTotals(), 8)
        val fromKotlin = LedgerCalculator.summarize(
            entries.filter { it.labourerId == ramesh }, payments, 8,
        )
        assertEquals(fromKotlin, fromSql)
        // 2 full + 5h/8h = 2.63 days (rounded), earned 700+900+437.5
        assertEquals(263L, fromSql.dayHundredths)
        assertEquals(2_037_50L, fromSql.earnedPaise)
        assertEquals(400_00L, fromSql.advancesPaise)
        assertEquals(637_50L, fromSql.pendingPaise)

        // Per-work filter
        val w1Att = db.attendanceDao().observeTotalsByLabourer(workId = w1).first().single { it.labourerId == ramesh }
        assertEquals(1_600_00L, w1Att.earnedPaise)
        val w1Pay = db.paymentDao().observeTotalsByLabourer(workId = w1).first().single { it.labourerId == ramesh }
        assertEquals(1_000_00L, w1Pay.wagePaise)
        assertEquals(0L, w1Pay.advancePaise)
    }

    @Test fun `project filter and day cost`() = runTest {
        val p1 = newProject("One")
        val p2 = newProject("Two")
        val w1 = newWork(p1)
        val w2 = newWork(p2)
        val l = newLabourer()
        db.attendanceDao().insert(att(w1, l, d1, AttendanceStatus.FULL_DAY, 480, 700_00))
        db.attendanceDao().insert(att(w2, l, d1, AttendanceStatus.FULL_DAY, 480, 800_00))

        assertEquals(700_00L, db.attendanceDao().observeDayCost(p1, d1).first())
        assertEquals(0L, db.attendanceDao().observeDayCost(p1, d1.plusDays(1)).first())
        val rows = db.attendanceDao().observeTotalsByLabourer(projectId = p2).first()
        assertEquals(800_00L, rows.single().earnedPaise)
        assertEquals(1, db.attendanceDao().observeForLabourer(l, p1).first().size)
        assertTrue(db.attendanceDao().observeForLabourer(l, p1).first().all { it.workName == "Electrical" })
    }
}
