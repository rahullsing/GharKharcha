package com.gharkharcha.data.reports

import com.gharkharcha.data.dao.AttendanceWithLabourer
import com.gharkharcha.data.dao.AttendanceWithWork
import com.gharkharcha.data.dao.PaymentWithLabourer
import com.gharkharcha.data.entity.AttendanceEntity
import com.gharkharcha.data.entity.AttendanceStatus
import com.gharkharcha.data.entity.LabourerEntity
import com.gharkharcha.data.entity.MaterialEntity
import com.gharkharcha.data.entity.MaterialUnit
import com.gharkharcha.data.entity.PaymentEntity
import com.gharkharcha.data.entity.PaymentMode
import com.gharkharcha.data.entity.PaymentType
import com.gharkharcha.data.entity.ProjectEntity
import com.gharkharcha.data.entity.WorkEntity
import com.gharkharcha.data.entity.WorkStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class ReportsGeneratorTest {

    @Test
    fun testDateFilterPresets() {
        val today = LocalDate.now()
        val allTime = DateFilter(DateRangePreset.ALL_TIME)
        assertTrue(allTime.matches(today))
        assertTrue(allTime.matches(today.minusYears(2)))

        val thisMonth = DateFilter(DateRangePreset.THIS_MONTH)
        assertTrue(thisMonth.matches(today))
        assertFalse(thisMonth.matches(today.minusMonths(2)))

        val lastMonth = DateFilter(DateRangePreset.LAST_MONTH)
        assertTrue(lastMonth.matches(today.minusMonths(1).withDayOfMonth(15)))
        assertFalse(lastMonth.matches(today))

        val custom = DateFilter(
            DateRangePreset.CUSTOM,
            startDate = today.minusDays(5),
            endDate = today.plusDays(5),
        )
        assertTrue(custom.matches(today))
        assertTrue(custom.matches(today.minusDays(5)))
        assertTrue(custom.matches(today.plusDays(5)))
        assertFalse(custom.matches(today.minusDays(6)))
        assertFalse(custom.matches(today.plusDays(6)))
    }

    @Test
    fun testCsvHouseSummaryGeneration() {
        val project = ProjectEntity(id = 1, name = "Shanti Nivas", address = "Plot 42, Green Park", budgetPaise = 25_00_000_00)
        val work = WorkEntity(id = 1, projectId = 1, name = "Brick Masonry", budgetPaise = 5_00_000_00, status = WorkStatus.IN_PROGRESS)
        val workRow = HouseWorkRow(
            work = work,
            materialPaise = 1_50_000_00,
            labourPaise = 80_000_00,
            totalPaise = 2_30_000_00,
            budgetPaise = 5_00_000_00,
            percentUsed = 46,
        )
        val monthRow = MonthTotalRow(
            yearMonth = "Nov 2026",
            materialPaise = 1_50_000_00,
            labourPaise = 80_000_00,
            totalPaise = 2_30_000_00,
        )
        val data = HouseSummaryReportData(
            project = project,
            dateFilterText = "All time",
            works = listOf(workRow),
            totalMaterialPaise = 1_50_000_00,
            totalLabourPaise = 80_000_00,
            grandTotalPaise = 2_30_000_00,
            totalBudgetPaise = 25_00_000_00,
            balancePaise = 22_70_000_00,
            unpaidShopPaise = 20_000_00,
            monthlyTotals = listOf(monthRow),
        )

        val out = ByteArrayOutputStream()
        CsvReportGenerator.generateHouseSummary(data, out)
        val csv = out.toString(StandardCharsets.UTF_8.name())

        // Check UTF-8 BOM
        assertTrue(csv.startsWith("\uFEFF"))
        assertTrue(csv.contains("GHARKHARCHA - HOUSE SUMMARY REPORT"))
        assertTrue(csv.contains("Shanti Nivas"))
        assertTrue(csv.contains("Plot 42, Green Park"))
        assertTrue(csv.contains("Brick Masonry"))
        assertTrue(csv.contains("₹2,30,000"))
        assertTrue(csv.contains("₹22,70,000"))
        assertTrue(csv.contains("Nov 2026"))
    }

    @Test
    fun testCsvWorkReportGeneration() {
        val project = ProjectEntity(id = 1, name = "Shanti Nivas")
        val work = WorkEntity(id = 10, projectId = 1, name = "Foundation, Columns & Slabs", budgetPaise = 10_00_000_00)
        val material = MaterialEntity(
            id = 1,
            workId = 10,
            itemName = "River Sand, Grade-A",
            brand = "UltraTech \"Special\"",
            quantityMilli = 5_000,
            unit = MaterialUnit.BAG,
            ratePaise = 400_00,
            totalPaise = 2_000_00,
            shopName = "Gupta & Sons, Builders",
            purchaseDate = LocalDate.of(2026, 11, 10),
            isPaid = true,
        )
        val attendance = AttendanceWithLabourer(
            attendance = AttendanceEntity(
                id = 1,
                workId = 10,
                labourerId = 5,
                date = LocalDate.of(2026, 11, 10),
                status = AttendanceStatus.FULL_DAY,
                minutesWorked = 480,
                basePayPaise = 700_00,
                overtimeMinutes = 60,
                overtimePayPaise = 150_00,
                advancePaise = 200_00,
            ),
            labourerName = "Ramesh Kumar",
            labourerSkill = "Mason",
        )
        val payment = PaymentWithLabourer(
            payment = PaymentEntity(
                id = 1,
                labourerId = 5,
                workId = 10,
                date = LocalDate.of(2026, 11, 11),
                amountPaise = 1_000_00,
                type = PaymentType.WAGE,
                mode = PaymentMode.UPI,
                reference = "UPI12345678",
                notes = "Weekly settlement",
            ),
            labourerName = "Ramesh Kumar",
        )

        val data = WorkReportData(
            project = project,
            work = work,
            dateFilterText = "Nov 2026",
            materials = listOf(material),
            attendance = listOf(attendance),
            payments = listOf(payment),
            totalMaterialPaise = 2_000_00,
            totalLabourPaise = 850_00,
            grandTotalPaise = 2_850_00,
            budgetPaise = 10_00_000_00,
            variancePaise = 9_97_150_00,
        )

        val out = ByteArrayOutputStream()
        CsvReportGenerator.generateWorkReport(data, out)
        val csv = out.toString(StandardCharsets.UTF_8.name())

        assertTrue(csv.startsWith("\uFEFF"))
        assertTrue(csv.contains("GHARKHARCHA - WORK DETAILED REPORT"))
        // Check RFC 4180 escaping on material name with comma
        assertTrue(csv.contains("\"River Sand, Grade-A\""))
        // Check RFC 4180 escaping on quotes
        assertTrue(csv.contains("\"UltraTech \"\"Special\"\"\""))
        assertTrue(csv.contains("Ramesh Kumar"))
        assertTrue(csv.contains("Mason"))
        assertTrue(csv.contains("₹850"))
        assertTrue(csv.contains("UPI12345678"))
        assertTrue(csv.contains("Weekly settlement"))
    }

    @Test
    fun testCsvLabourerStatementGeneration() {
        val project = ProjectEntity(id = 1, name = "Shanti Nivas")
        val labourer = LabourerEntity(
            id = 5,
            name = "Ramesh Kumar",
            phone = "9876543210",
            customSkill = "Senior Mason",
            dailyWagePaise = 800_00,
        )
        val att = AttendanceWithWork(
            attendance = AttendanceEntity(
                id = 1,
                workId = 10,
                labourerId = 5,
                date = LocalDate.of(2026, 11, 12),
                status = AttendanceStatus.FULL_DAY,
                minutesWorked = 480,
                basePayPaise = 800_00,
            ),
            workName = "Brickwork",
        )
        val payment = PaymentEntity(
            id = 1,
            labourerId = 5,
            date = LocalDate.of(2026, 11, 12),
            amountPaise = 500_00,
            mode = PaymentMode.CASH,
            notes = "Daily advance",
        )

        val data = LabourerStatementData(
            project = project,
            labourer = labourer,
            photoPath = null,
            dateFilterText = "This month",
            attendance = listOf(att),
            payments = listOf(payment),
            totalDays = 1.0,
            totalMinutes = 480,
            totalEarnedPaise = 800_00,
            totalPaidPaise = 500_00,
            netDuesPaise = 300_00,
        )

        val out = ByteArrayOutputStream()
        CsvReportGenerator.generateLabourerStatement(data, out)
        val csv = out.toString(StandardCharsets.UTF_8.name())

        assertTrue(csv.startsWith("\uFEFF"))
        assertTrue(csv.contains("GHARKHARCHA - LABOURER STATEMENT"))
        assertTrue(csv.contains("Ramesh Kumar"))
        assertTrue(csv.contains("9876543210"))
        assertTrue(csv.contains("Senior Mason"))
        assertTrue(csv.contains("Pending Due to Labourer"))
        assertTrue(csv.contains("₹300"))
    }
}
