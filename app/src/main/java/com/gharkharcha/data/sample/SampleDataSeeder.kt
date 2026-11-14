package com.gharkharcha.data.sample

import com.gharkharcha.data.db.GharKharchaDatabase
import com.gharkharcha.data.entity.AttendanceEntity
import com.gharkharcha.data.entity.AttendanceStatus
import com.gharkharcha.data.entity.LabourerEntity
import com.gharkharcha.data.entity.MaterialEntity
import com.gharkharcha.data.entity.MaterialPaymentMode
import com.gharkharcha.data.entity.MaterialUnit
import com.gharkharcha.data.entity.PaymentEntity
import com.gharkharcha.data.entity.PaymentMode
import com.gharkharcha.data.entity.PaymentType
import com.gharkharcha.data.entity.ProjectEntity
import com.gharkharcha.data.entity.RateType
import com.gharkharcha.data.entity.Skill
import com.gharkharcha.data.entity.VendorEntity
import com.gharkharcha.data.entity.WorkEntity
import com.gharkharcha.data.entity.WorkStatus
import com.gharkharcha.data.entity.WorkType
import com.gharkharcha.data.prefs.UserPreferencesRepository
import com.gharkharcha.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SampleDataSeeder @Inject constructor(
    private val db: GharKharchaDatabase,
    private val prefs: UserPreferencesRepository,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) {
    suspend fun seedSampleData() = withContext(io) {
        val today = LocalDate.now()

        // 1. House Project
        val projectId = db.projectDao().insert(
            ProjectEntity(
                name = "Dream Villa (Sector 62)",
                address = "Plot 14, Green Valley Enclave",
                startDate = today.minusDays(30),
                budgetPaise = 2_500_000_00L, // ₹25 Lakh
                notes = "G+1 Residential house construction",
            )
        )
        prefs.setSelectedProject(projectId)

        // 2. Vendors
        val vendor1Id = db.vendorDao().insert(
            VendorEntity(
                name = "Gupta Building Materials",
                phone = "9876543210",
                address = "Shop 12, Sector 18 Market",
                gstin = "07AAAAA0000A1Z5",
            )
        )
        val vendor2Id = db.vendorDao().insert(
            VendorEntity(
                name = "Sharma Electricals & Hardware",
                phone = "9876543211",
                address = "Near Metro Pillar 42",
            )
        )

        // 3. Works
        val work1Id = db.workDao().insert(
            WorkEntity(
                projectId = projectId,
                name = "Brick Masonry & Plaster",
                type = WorkType.BRICK_MASONRY,
                budgetPaise = 350_000_00L,
                startDate = today.minusDays(25),
                status = WorkStatus.IN_PROGRESS,
                notes = "Ground floor outer walls and plastering",
            )
        )
        val work2Id = db.workDao().insert(
            WorkEntity(
                projectId = projectId,
                name = "Electrical Wiring & Conduit",
                type = WorkType.ELECTRICAL,
                budgetPaise = 180_000_00L,
                startDate = today.minusDays(14),
                status = WorkStatus.IN_PROGRESS,
                notes = "Wall conduit chipping and wiring boxes",
            )
        )
        db.workDao().insert(
            WorkEntity(
                projectId = projectId,
                name = "Plumbing & Sanitary",
                type = WorkType.PLUMBING,
                budgetPaise = 150_000_00L,
                startDate = today.plusDays(5),
                status = WorkStatus.PLANNED,
                notes = "Concealed pipe fittings and drainage",
            )
        )

        // 4. Labourers
        val lab1Id = db.labourerDao().insert(
            LabourerEntity(
                name = "Ramesh Kumar (Mason)",
                phone = "9812345678",
                skill = Skill.BRICK_MASON,
                dailyWagePaise = 700_00L,
                hourlyRatePaise = 90_00L,
                address = "Noida Village",
            )
        )
        val lab2Id = db.labourerDao().insert(
            LabourerEntity(
                name = "Suresh Yadav (Helper)",
                phone = "9823456789",
                skill = Skill.HELPER,
                dailyWagePaise = 500_00L,
                hourlyRatePaise = 65_00L,
                address = "Chhajarsi",
            )
        )
        val lab3Id = db.labourerDao().insert(
            LabourerEntity(
                name = "Imran Khan (Electrician)",
                phone = "9834567890",
                skill = Skill.ELECTRICIAN,
                dailyWagePaise = 800_00L,
                hourlyRatePaise = 100_00L,
                address = "Khoda Colony",
            )
        )
        db.labourerDao().insert(
            LabourerEntity(
                name = "Rajesh Verma (Plumber)",
                phone = "9845678901",
                skill = Skill.PLUMBER,
                dailyWagePaise = 750_00L,
                hourlyRatePaise = 95_00L,
                address = "Mamura",
            )
        )

        // 5. Materials
        db.materialDao().insert(
            MaterialEntity(
                workId = work1Id,
                itemName = "UltraTech Cement",
                brand = "UltraTech PPC",
                quantityMilli = 50_000L,
                unit = MaterialUnit.BAG,
                ratePaise = 380_00L,
                totalPaise = 19_000_00L,
                vendorId = vendor1Id,
                shopName = "Gupta Building Materials",
                shopPhone = "9876543210",
                purchaseDate = today.minusDays(20),
                billNumber = "INV-1042",
                paymentMode = MaterialPaymentMode.UPI,
                isPaid = true,
            )
        )
        db.materialDao().insert(
            MaterialEntity(
                workId = work1Id,
                itemName = "Red Bricks 1st Class",
                brand = "Awadh Bhrata",
                quantityMilli = 3000_000L,
                unit = MaterialUnit.PIECE,
                ratePaise = 9_00L,
                totalPaise = 27_000_00L,
                vendorId = vendor1Id,
                shopName = "Gupta Building Materials",
                shopPhone = "9876543210",
                purchaseDate = today.minusDays(18),
                billNumber = "INV-1055",
                paymentMode = MaterialPaymentMode.CASH,
                isPaid = true,
            )
        )
        db.materialDao().insert(
            MaterialEntity(
                workId = work1Id,
                itemName = "River Sand (1 Truck)",
                quantityMilli = 1_000L,
                unit = MaterialUnit.CUSTOM,
                customUnit = "truck",
                ratePaise = 18_000_00L,
                totalPaise = 18_000_00L,
                vendorId = vendor1Id,
                shopName = "Gupta Building Materials",
                purchaseDate = today.minusDays(15),
                paymentMode = MaterialPaymentMode.CREDIT,
                isPaid = false,
            )
        )
        db.materialDao().insert(
            MaterialEntity(
                workId = work2Id,
                itemName = "Polycab Copper Wire 2.5 sq mm",
                brand = "Polycab FRLS",
                quantityMilli = 4_000L,
                unit = MaterialUnit.BOX,
                ratePaise = 1_850_00L,
                totalPaise = 7_400_00L,
                vendorId = vendor2Id,
                shopName = "Sharma Electricals & Hardware",
                purchaseDate = today.minusDays(8),
                billNumber = "SE-882",
                paymentMode = MaterialPaymentMode.UPI,
                isPaid = true,
            )
        )

        // 6. 2 Weeks of Attendance
        for (i in 14 downTo 1) {
            val date = today.minusDays(i.toLong())
            if (date.dayOfWeek.value == 7) continue // Skip Sundays

            db.attendanceDao().insert(
                AttendanceEntity(
                    workId = work1Id,
                    labourerId = lab1Id,
                    date = date,
                    status = AttendanceStatus.FULL_DAY,
                    minutesWorked = 8 * 60,
                    rateType = RateType.DAILY,
                    ratePaise = 700_00L,
                    basePayPaise = 700_00L,
                )
            )

            db.attendanceDao().insert(
                AttendanceEntity(
                    workId = work1Id,
                    labourerId = lab2Id,
                    date = date,
                    status = AttendanceStatus.FULL_DAY,
                    minutesWorked = 8 * 60,
                    rateType = RateType.DAILY,
                    ratePaise = 500_00L,
                    basePayPaise = 500_00L,
                )
            )

            if (i <= 5) {
                db.attendanceDao().insert(
                    AttendanceEntity(
                        workId = work2Id,
                        labourerId = lab3Id,
                        date = date,
                        status = AttendanceStatus.FULL_DAY,
                        minutesWorked = 8 * 60,
                        rateType = RateType.DAILY,
                        ratePaise = 800_00L,
                        basePayPaise = 800_00L,
                    )
                )
            }
        }

        // Today's attendance
        db.attendanceDao().insert(
            AttendanceEntity(
                workId = work1Id,
                labourerId = lab1Id,
                date = today,
                status = AttendanceStatus.FULL_DAY,
                minutesWorked = 8 * 60,
                rateType = RateType.DAILY,
                ratePaise = 700_00L,
                basePayPaise = 700_00L,
            )
        )
        db.attendanceDao().insert(
            AttendanceEntity(
                workId = work1Id,
                labourerId = lab2Id,
                date = today,
                status = AttendanceStatus.FULL_DAY,
                minutesWorked = 8 * 60,
                rateType = RateType.DAILY,
                ratePaise = 500_00L,
                basePayPaise = 500_00L,
            )
        )

        // 7. Payments
        db.paymentDao().insert(
            PaymentEntity(
                labourerId = lab1Id,
                projectId = projectId,
                workId = work1Id,
                date = today.minusDays(7),
                amountPaise = 5_000_00L,
                type = PaymentType.WAGE,
                mode = PaymentMode.CASH,
                notes = "Weekly wage payment",
            )
        )
        db.paymentDao().insert(
            PaymentEntity(
                labourerId = lab2Id,
                projectId = projectId,
                workId = work1Id,
                date = today.minusDays(7),
                amountPaise = 3_000_00L,
                type = PaymentType.WAGE,
                mode = PaymentMode.UPI,
                reference = "UPI/428819201923",
                notes = "Weekly wage payment",
            )
        )
        db.paymentDao().insert(
            PaymentEntity(
                labourerId = lab3Id,
                projectId = projectId,
                workId = work2Id,
                date = today.minusDays(3),
                amountPaise = 1_500_00L,
                type = PaymentType.ADVANCE,
                mode = PaymentMode.UPI,
                reference = "UPI/429910294821",
                notes = "Advance given for conduit tools",
            )
        )
    }
}
