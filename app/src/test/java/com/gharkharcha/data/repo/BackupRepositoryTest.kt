package com.gharkharcha.data.repo

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import com.gharkharcha.data.dao.DaoTestBase
import com.gharkharcha.data.files.AttachmentFileStore
import com.gharkharcha.data.prefs.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
class BackupRepositoryTest : DaoTestBase() {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val prefsRepo by lazy {
        val dataStore = PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("test_settings_${System.nanoTime()}")
        }
        UserPreferencesRepository(dataStore)
    }

    private fun backupRepo(): BackupRepository {
        return BackupRepository(
            context = context,
            db = db,
            prefs = prefsRepo,
            io = Dispatchers.Unconfined,
        )
    }

    @Test
    fun `backup archives database, attachments, and manifest into zip`() = runTest {
        val repo = backupRepo()

        // Create a dummy attachment file
        val attachmentsDir = File(context.filesDir, AttachmentFileStore.ATTACHMENTS_DIR)
        val testFile = File(attachmentsDir, "material/1/sample.jpg").apply {
            parentFile?.mkdirs()
            writeText("dummy image content")
        }

        val out = ByteArrayOutputStream()
        val metadata = repo.createBackupToStream(out)

        assertTrue(metadata.photoCount >= 1)
        assertTrue(metadata.totalBytes > 0)

        // Read zip entries
        val entries = mutableListOf<String>()
        ZipInputStream(ByteArrayInputStream(out.toByteArray())).use { zipIn ->
            var entry: ZipEntry? = zipIn.nextEntry
            while (entry != null) {
                entries.add(entry.name.replace('\\', '/'))
                entry = zipIn.nextEntry
            }
        }

        assertTrue(entries.contains("manifest.json"))
        assertTrue(entries.any { it.startsWith("attachments/") })

        val lastBackup = prefsRepo.preferences.first().lastBackupTimestamp
        assertTrue(lastBackup > 0)
    }

    @Test
    fun `restore rejects archive missing database file`() = runTest {
        val repo = backupRepo()

        // Create a zip without database
        val badZipOut = ByteArrayOutputStream()
        ZipOutputStream(badZipOut).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write("{}".toByteArray())
            zip.closeEntry()
        }

        val result = repo.restoreFromStream(ByteArrayInputStream(badZipOut.toByteArray()))
        assertTrue(result is RestoreResult.Failure)
        assertEquals("Invalid backup archive: Database file missing", (result as RestoreResult.Failure).message)
    }

    @Test
    fun `roundtrip backup and restore extracts attachments successfully`() = runTest {
        val repo = backupRepo()

        // Ensure database file exists on disk for test
        val dbFile = context.getDatabasePath(com.gharkharcha.data.db.GharKharchaDatabase.NAME)
        dbFile.parentFile?.mkdirs()
        dbFile.writeText("SQLite format 3 test header")

        // Set up initial attachment
        val attachmentsDir = File(context.filesDir, AttachmentFileStore.ATTACHMENTS_DIR)
        val initialFile = File(attachmentsDir, "works/42/photo.jpg").apply {
            parentFile?.mkdirs()
            writeText("house foundation photo")
        }

        val backupOut = ByteArrayOutputStream()
        val metadata = repo.createBackupToStream(backupOut)
        assertEquals(1, metadata.photoCount)

        // Delete the original attachment to simulate clean state
        initialFile.delete()

        // Restore from backup
        val restoreResult = repo.restoreFromStream(ByteArrayInputStream(backupOut.toByteArray()))
        assertTrue(restoreResult is RestoreResult.Success)
        assertEquals(1, (restoreResult as RestoreResult.Success).photoCount)

        // Verify file is restored
        val restoredFile = File(attachmentsDir, "works/42/photo.jpg")
        assertTrue(restoredFile.exists())
        assertEquals("house foundation photo", restoredFile.readText())
    }
}
