package com.gharkharcha.data.dao

import com.gharkharcha.data.entity.AttachmentEntity
import com.gharkharcha.data.entity.AttendanceEntity
import com.gharkharcha.data.entity.AttendanceStatus
import com.gharkharcha.data.entity.MaterialEntity
import com.gharkharcha.data.entity.OwnerType
import com.gharkharcha.data.entity.PaymentEntity
import com.gharkharcha.data.entity.VendorEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class ProjectWorkMaterialDaoTest : DaoTestBase() {
    private val today = LocalDate.of(2026, 11, 14)

    private fun material(workId: Long, total: Long, brand: String = "", item: String = "Wire") =
        MaterialEntity(workId = workId, itemName = item, brand = brand, totalPaise = total, purchaseDate = today)

    @Test fun `project round trip with LocalDate`() = runTest {
        val id = newProject()
        db.projectDao().update(db.projectDao().get(id)!!.copy(startDate = today))
        assertEquals(today, db.projectDao().get(id)!!.startDate)
        assertEquals(1, db.projectDao().observeAll().first().size)
    }

    @Test fun `spend by work sums materials and labour including zero rows`() = runTest {
        val p = newProject()
        val w1 = newWork(p, "Electrical")
        val w2 = newWork(p, "Plumbing")
        val l = newLabourer()
        db.materialDao().insert(material(w1, 1_000_00))
        db.materialDao().insert(material(w1, 500_50))
        db.attendanceDao().insert(
            AttendanceEntity(
                workId = w1, labourerId = l, date = today, status = AttendanceStatus.OVERTIME,
                basePayPaise = 700_00, overtimePayPaise = 150_00,
            ),
        )

        val rows = db.workDao().observeSpendByWork(p).first().associateBy { it.workId }
        assertEquals(1_500_50L, rows.getValue(w1).materialPaise)
        assertEquals(850_00L, rows.getValue(w1).labourPaise)
        assertEquals(0L, rows.getValue(w2).materialPaise)
        assertEquals(0L, rows.getValue(w2).labourPaise)
    }

    @Test fun `deleting a work cascades materials and attendance but keeps payments`() = runTest {
        val p = newProject()
        val w = newWork(p)
        val l = newLabourer()
        val m = db.materialDao().insert(material(w, 100_00))
        db.attendanceDao().insert(AttendanceEntity(workId = w, labourerId = l, date = today, status = AttendanceStatus.FULL_DAY))
        val pay = db.paymentDao().insert(
            PaymentEntity(labourerId = l, projectId = p, workId = w, date = today, amountPaise = 500_00),
        )

        db.workDao().delete(db.workDao().get(w)!!)

        assertNull(db.materialDao().get(m))
        assertTrue(db.attendanceDao().listForWorkOnDate(w, today).isEmpty())
        val kept = db.paymentDao().get(pay)!!
        assertNull(kept.workId)
        assertEquals(p, kept.projectId)
    }

    @Test fun `deleting a project cascades works`() = runTest {
        val p = newProject()
        val w = newWork(p)
        db.projectDao().delete(db.projectDao().get(p)!!)
        assertNull(db.workDao().get(w))
    }

    @Test fun `brand and item suggestions are distinct and prefix matched`() = runTest {
        val w = newWork(newProject())
        db.materialDao().insert(material(w, 1, brand = "Havells", item = "Wire 2.5 sq mm"))
        db.materialDao().insert(material(w, 1, brand = "Havells", item = "Wire 4 sq mm"))
        db.materialDao().insert(material(w, 1, brand = "Polycab"))
        db.materialDao().insert(material(w, 1, brand = ""))

        assertEquals(listOf("Havells"), db.materialDao().suggestBrands("hav"))
        assertEquals(listOf("Havells", "Polycab"), db.materialDao().suggestBrands(""))
        assertEquals(2, db.materialDao().suggestItems("Wire ").size)
    }

    @Test fun `vendor names are unique case-insensitively and deletion nulls material link`() = runTest {
        val v = db.vendorDao().insert(VendorEntity(name = "Sharma Electricals"))
        assertEquals(-1L, db.vendorDao().insert(VendorEntity(name = "sharma electricals")))
        assertEquals(v, db.vendorDao().findByName("SHARMA ELECTRICALS")!!.id)

        val w = newWork(newProject())
        val m = db.materialDao().insert(material(w, 1).copy(vendorId = v, shopName = "Sharma Electricals"))
        db.vendorDao().delete(db.vendorDao().get(v)!!)
        val after = db.materialDao().get(m)!!
        assertNull(after.vendorId)
        assertEquals("Sharma Electricals", after.shopName)
    }

    @Test fun `attachment tree queries`() = runTest {
        val p = newProject()
        val w = newWork(p)
        val otherWork = newWork(newProject("Other"))
        val m = db.materialDao().insert(material(w, 1))
        fun att(type: OwnerType, id: Long) = AttachmentEntity(ownerType = type, ownerId = id, filePath = "x/$type/$id.jpg")
        db.attachmentDao().insert(att(OwnerType.PROJECT, p))
        db.attachmentDao().insert(att(OwnerType.WORK, w))
        db.attachmentDao().insert(att(OwnerType.MATERIAL, m))
        db.attachmentDao().insert(att(OwnerType.WORK, otherWork))
        db.attachmentDao().insert(att(OwnerType.PAYMENT, 99))

        assertEquals(2, db.attachmentDao().listForWorkTree(w).size)
        assertEquals(3, db.attachmentDao().listForProjectTree(p).size)
        assertEquals(1, db.attachmentDao().listFor(OwnerType.MATERIAL, m).size)
    }

    @Test fun `attachments ordered by sort order`() = runTest {
        fun att(order: Int) = AttachmentEntity(ownerType = OwnerType.PAYMENT, ownerId = 1, filePath = "p$order", sortOrder = order)
        db.attachmentDao().insert(att(2))
        db.attachmentDao().insert(att(0))
        db.attachmentDao().insert(att(1))
        assertEquals(
            listOf("p0", "p1", "p2"),
            db.attachmentDao().observeFor(OwnerType.PAYMENT, 1).first().map { it.filePath },
        )
    }

    @Test fun `daily spend aggregates material and attendance per day`() = runTest {
        val p = newProject()
        val w = newWork(p)
        val l = newLabourer()
        val d1 = today.minusDays(2)
        val d2 = today.minusDays(1)

        db.materialDao().insert(material(w, 100_00).copy(purchaseDate = d1))
        db.materialDao().insert(material(w, 50_00).copy(purchaseDate = d1))
        db.materialDao().insert(material(w, 200_00).copy(purchaseDate = d2))

        val matDaily = db.materialDao().observeAllDailySpend(p).first()
        val d1Row = matDaily.first { it.date == d1 }
        val d2Row = matDaily.first { it.date == d2 }
        assertEquals(150_00, d1Row.totalPaise)
        assertEquals(200_00, d2Row.totalPaise)

        db.attendanceDao().insert(
            AttendanceEntity(workId = w, labourerId = l, date = d1, status = AttendanceStatus.FULL_DAY, basePayPaise = 500_00),
        )
        val attDaily = db.attendanceDao().observeAllDailySpend(p).first()
        assertEquals(500_00, attDaily.single { it.date == d1 }.totalPaise)
    }

    @Test fun `search queries across materials and works`() = runTest {
        val p = newProject()
        val w1 = newWork(p, "Bathroom Plumbing")
        val w2 = newWork(p, "Kitchen Electrical")

        db.materialDao().insert(material(w1, 50_00, brand = "Finolex", item = "PVC Pipe"))
        db.materialDao().insert(material(w2, 100_00, brand = "Havells", item = "Switchboard"))

        val pipes = db.materialDao().searchAll("PVC").first()
        assertEquals(1, pipes.size)
        assertEquals("PVC Pipe", pipes[0].itemName)

        val havells = db.materialDao().searchAll("havells").first()
        assertEquals(1, havells.size)
        assertEquals("Switchboard", havells[0].itemName)

        val plumbWorks = db.workDao().search(p, "plumb").first()
        assertEquals(1, plumbWorks.size)
        assertEquals("Bathroom Plumbing", plumbWorks[0].name)
    }
}
