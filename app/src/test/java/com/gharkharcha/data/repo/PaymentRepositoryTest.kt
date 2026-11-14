package com.gharkharcha.data.repo

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.gharkharcha.data.dao.DaoTestBase
import com.gharkharcha.data.entity.AttachmentKind
import com.gharkharcha.data.entity.OwnerType
import com.gharkharcha.data.entity.PaymentEntity
import com.gharkharcha.data.entity.PaymentMode
import com.gharkharcha.data.entity.PaymentType
import com.gharkharcha.data.files.AttachmentFileStore
import com.gharkharcha.domain.model.PhotoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class PaymentRepositoryTest : DaoTestBase() {
    private val files = AttachmentFileStore(ApplicationProvider.getApplicationContext<Context>())
    private val attachmentRepo by lazy { AttachmentRepository(db.attachmentDao(), files, Dispatchers.Unconfined) }
    private val paymentRepo by lazy { PaymentRepository(db, db.paymentDao(), db.attachmentDao(), attachmentRepo) }

    private fun pending(name: String, kind: AttachmentKind = AttachmentKind.PHOTO): PhotoItem {
        val f = files.newPendingFile().apply { writeText(name) }
        return PhotoItem(key = name, path = f.absolutePath, kind = kind)
    }

    @Test
    fun `saveWithPhotos creates payment and syncs photos`() = runTest {
        val p = newProject("House A")
        val w = newWork(p)
        val l = newLabourer()

        val payment = PaymentEntity(
            labourerId = l,
            projectId = p,
            workId = w,
            date = LocalDate.of(2026, 5, 10),
            amountPaise = 5000_00,
            type = PaymentType.WAGE,
            mode = PaymentMode.UPI,
            reference = "UPI/2026/05/12345",
            periodFrom = LocalDate.of(2026, 5, 4),
            periodTo = LocalDate.of(2026, 5, 10),
            notes = "Weekly wage settlement",
        )
        val photo1 = pending("receipt_1")
        val photo2 = pending("receipt_2")

        val paymentId = paymentRepo.saveWithPhotos(payment, listOf(photo1, photo2), emptyList())
        assertTrue(paymentId > 0)

        val retrieved = paymentRepo.get(paymentId)
        assertNotNull(retrieved)
        assertEquals("UPI/2026/05/12345", retrieved?.reference)
        assertEquals(LocalDate.of(2026, 5, 4), retrieved?.periodFrom)
        assertEquals(LocalDate.of(2026, 5, 10), retrieved?.periodTo)

        // Verify attachments synced under OwnerType.PAYMENT
        val attachments = attachmentRepo.listFor(OwnerType.PAYMENT, paymentId)
        assertEquals(2, attachments.size)

        // Verify photo counts observed for work and labourer
        val workPhotoCounts = attachmentRepo.observePaymentPhotoCounts(w).first()
        assertEquals(2, workPhotoCounts[paymentId])

        val labourerPhotoCounts = attachmentRepo.observeLabourerPaymentPhotoCounts(l).first()
        assertEquals(2, labourerPhotoCounts[paymentId])
    }

    @Test
    fun `saveWithPhotos updates payment and removes deleted photo`() = runTest {
        val p = newProject("House B")
        val l = newLabourer()

        val initial = PaymentEntity(
            labourerId = l,
            projectId = p,
            date = LocalDate.of(2026, 5, 11),
            amountPaise = 2000_00,
        )
        val p1 = pending("p1")
        val p2 = pending("p2")
        val paymentId = paymentRepo.saveWithPhotos(initial, listOf(p1, p2), emptyList())

        val savedPhotos = attachmentRepo.loadItems(OwnerType.PAYMENT, paymentId)
        assertEquals(2, savedPhotos.size)
        val keptPhoto = savedPhotos[0]
        val removedPhoto = savedPhotos[1]

        val updated = paymentRepo.get(paymentId)!!.copy(amountPaise = 2500_00)
        paymentRepo.saveWithPhotos(
            updated,
            listOf(keptPhoto),
            removedPhotoIds = listOf(removedPhoto.attachmentId!!),
        )

        val afterPhotos = attachmentRepo.loadItems(OwnerType.PAYMENT, paymentId)
        assertEquals(1, afterPhotos.size)
        assertEquals(keptPhoto.attachmentId, afterPhotos[0].attachmentId)

        val labourerPhotoCounts = attachmentRepo.observeLabourerPaymentPhotoCounts(l).first()
        assertEquals(1, labourerPhotoCounts[paymentId])
    }
}
