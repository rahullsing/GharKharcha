package com.gharkharcha.data.repo

import com.gharkharcha.data.dao.DaoTestBase
import com.gharkharcha.data.entity.AttendanceEntity
import com.gharkharcha.data.entity.AttendanceStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class AttendanceRepositoryTest : DaoTestBase() {
    private val mon = LocalDate.of(2026, 11, 9)

    private fun repo() = AttendanceRepository(db, db.attendanceDao())

    private fun entry(work: Long, labourer: Long, date: LocalDate, status: AttendanceStatus = AttendanceStatus.FULL_DAY) =
        AttendanceEntity(
            workId = work, labourerId = labourer, date = date, status = status,
            minutesWorked = 480, basePayPaise = 700_00, advancePaise = 200_00,
        )

    @Test fun `save updates the same row for the same day`() = runTest {
        val w = newWork(newProject())
        val l = newLabourer()
        val repo = repo()
        val id = repo.save(entry(w, l, mon))
        val again = repo.save(entry(w, l, mon, AttendanceStatus.HALF_DAY).copy(basePayPaise = 350_00))
        assertEquals(id, again)
        val rows = repo.listForWorkOnDate(w, mon)
        assertEquals(1, rows.size)
        assertEquals(AttendanceStatus.HALF_DAY, rows.single().status)
    }

    @Test fun `copy previous day skips already marked labourers and drops advances`() = runTest {
        val w = newWork(newProject())
        val a = newLabourer("A")
        val b = newLabourer("B")
        val repo = repo()
        repo.save(entry(w, a, mon))
        repo.save(entry(w, b, mon))
        repo.save(entry(w, b, mon.plusDays(1), AttendanceStatus.ABSENT))

        assertEquals(1, repo.copyDay(w, mon, mon.plusDays(1)))
        val tue = repo.listForWorkOnDate(w, mon.plusDays(1)).associateBy { it.labourerId }
        assertEquals(AttendanceStatus.FULL_DAY, tue.getValue(a).status)
        assertEquals(0L, tue.getValue(a).advancePaise)
        assertEquals(AttendanceStatus.ABSENT, tue.getValue(b).status) // untouched

        assertEquals(0, repo.copyDay(w, mon, mon.plusDays(1))) // nothing new the second time
    }

    @Test fun `repeat day over a range skips the source day`() = runTest {
        val w = newWork(newProject())
        val a = newLabourer("A")
        val repo = repo()
        repo.save(entry(w, a, mon))
        val week = (0L..5L).map { mon.plusDays(it) }
        assertEquals(5, repo.repeatDay(w, mon, week))
        week.forEach { assertEquals(1, repo.listForWorkOnDate(w, it).size) }
        assertTrue(repo.listForWorkOnDate(w, mon.plusDays(6)).isEmpty())
    }
}
