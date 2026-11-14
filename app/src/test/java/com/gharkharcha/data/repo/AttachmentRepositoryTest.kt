package com.gharkharcha.data.repo

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.gharkharcha.data.dao.DaoTestBase
import com.gharkharcha.data.entity.AttachmentKind
import com.gharkharcha.data.entity.OwnerType
import com.gharkharcha.data.files.AttachmentFileStore
import com.gharkharcha.domain.model.PhotoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class AttachmentRepositoryTest : DaoTestBase() {
    private val files = AttachmentFileStore(ApplicationProvider.getApplicationContext<Context>())
    private fun repo() = AttachmentRepository(db.attachmentDao(), files, Dispatchers.Unconfined)

    private fun pending(name: String, kind: AttachmentKind = AttachmentKind.PHOTO): PhotoItem {
        val f = files.newPendingFile().apply { writeText(name) }
        return PhotoItem(key = name, path = f.absolutePath, kind = kind)
    }

    @Test fun `sync moves pending files into the owner folder in order`() = runTest {
        val repo = repo()
        val a = pending("a", AttachmentKind.BILL)
        val b = pending("b")
        repo.sync(OwnerType.MATERIAL, 7, listOf(a, b), emptyList())

        val saved = repo.listFor(OwnerType.MATERIAL, 7)
        assertEquals(listOf(AttachmentKind.BILL, AttachmentKind.PHOTO), saved.map { it.kind })
        assertEquals(listOf(0, 1), saved.map { it.sortOrder })
        saved.forEach {
            assertTrue(it.filePath.startsWith("attachments/material/7/"))
            assertTrue(files.resolve(it.filePath).exists())
        }
        assertFalse(File(a.path).exists()) // moved, not copied
        assertEquals("a", files.resolve(saved[0].filePath).readText())
    }

    @Test fun `sync reorders kept photos, adds new ones and deletes removed`() = runTest {
        val repo = repo()
        repo.sync(OwnerType.PAYMENT, 3, listOf(pending("x"), pending("y")), emptyList())
        val (x, y) = repo.loadItems(OwnerType.PAYMENT, 3)
        val yFile = files.resolve(repo.listFor(OwnerType.PAYMENT, 3)[1].filePath)

        repo.sync(OwnerType.PAYMENT, 3, listOf(pending("z"), x.copy(caption = "UTR 123")), removedIds = listOf(y.attachmentId!!))

        val after = repo.listFor(OwnerType.PAYMENT, 3)
        assertEquals(2, after.size)
        assertEquals("z", files.resolve(after[0].filePath).readText())
        assertEquals(x.attachmentId, after[1].id)
        assertEquals("UTR 123", after[1].caption)
        assertFalse(yFile.exists())
    }

    @Test fun `replaceSingle swaps a profile photo and clears it`() = runTest {
        val repo = repo()
        repo.replaceSingle(OwnerType.LABOURER, 1, pending("first"), AttachmentKind.PROFILE)
        val first = repo.listFor(OwnerType.LABOURER, 1).single()
        assertEquals(AttachmentKind.PROFILE, first.kind)

        // Saving again with the same stored photo keeps it.
        repo.replaceSingle(OwnerType.LABOURER, 1, repo.loadItems(OwnerType.LABOURER, 1).single(), AttachmentKind.PROFILE)
        assertEquals(first.id, repo.listFor(OwnerType.LABOURER, 1).single().id)

        repo.replaceSingle(OwnerType.LABOURER, 1, pending("second"), AttachmentKind.PROFILE)
        val second = repo.listFor(OwnerType.LABOURER, 1).single()
        assertEquals("second", files.resolve(second.filePath).readText())
        assertFalse(files.resolve(first.filePath).exists())

        repo.replaceSingle(OwnerType.LABOURER, 1, null, AttachmentKind.PROFILE)
        assertTrue(repo.listFor(OwnerType.LABOURER, 1).isEmpty())
    }

    @Test fun `discardPending only touches unsaved files`() = runTest {
        val repo = repo()
        val p = pending("tmp")
        repo.discardPending(p)
        assertFalse(File(p.path).exists())

        val outside = File(files.root, "keep.jpg").apply { writeText("k") }
        repo.discardPending(PhotoItem("k", outside.absolutePath, AttachmentKind.PHOTO))
        assertTrue(outside.exists())
    }
}
