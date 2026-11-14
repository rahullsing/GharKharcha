package com.gharkharcha.data.repo

import android.content.Context
import android.net.Uri
import com.gharkharcha.data.db.GharKharchaDatabase
import com.gharkharcha.data.files.AttachmentFileStore
import com.gharkharcha.data.prefs.UserPreferencesRepository
import com.gharkharcha.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class BackupMetadata(
    val timestamp: Long,
    val photoCount: Int,
    val totalBytes: Long,
)

sealed interface RestoreResult {
    data class Success(val photoCount: Int) : RestoreResult
    data class Failure(val message: String) : RestoreResult
}

@Singleton
class BackupRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val db: GharKharchaDatabase,
    private val prefs: UserPreferencesRepository,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) {

    suspend fun createBackup(targetUri: Uri): BackupMetadata = withContext(io) {
        val outStream = context.contentResolver.openOutputStream(targetUri)
            ?: throw IllegalStateException("Could not open destination for backup")
        outStream.use { out ->
            createBackupToStream(out)
        }
    }

    suspend fun createBackupToStream(out: OutputStream): BackupMetadata = withContext(io) {
        // 1. Checkpoint WAL so all pending DB transactions are flushed to disk
        try {
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { cursor ->
                cursor.moveToFirst()
            }
        } catch (_: Exception) {
            // Checkpoint best-effort
        }

        val dbFile = context.getDatabasePath(GharKharchaDatabase.NAME)
        val attachmentsDir = File(context.filesDir, AttachmentFileStore.ATTACHMENTS_DIR)

        var photoCount = 0
        var totalBytes = 0L

        ZipOutputStream(BufferedOutputStream(out)).use { zipOut ->
            // 2. Add database file
            if (dbFile.exists()) {
                val dbEntry = ZipEntry("database/${GharKharchaDatabase.NAME}")
                zipOut.putNextEntry(dbEntry)
                FileInputStream(dbFile).use { inStream ->
                    inStream.copyTo(zipOut)
                }
                zipOut.closeEntry()
                totalBytes += dbFile.length()
            }

            // 3. Add attachments
            if (attachmentsDir.exists()) {
                attachmentsDir.walkTopDown().filter { it.isFile }.forEach { file ->
                    val relativePath = file.relativeTo(context.filesDir).path.replace('\\', '/')
                    val entry = ZipEntry(relativePath)
                    zipOut.putNextEntry(entry)
                    FileInputStream(file).use { inStream ->
                        inStream.copyTo(zipOut)
                    }
                    zipOut.closeEntry()
                    photoCount++
                    totalBytes += file.length()
                }
            }

            // 4. Add manifest
            val timestamp = System.currentTimeMillis()
            val manifest = JSONObject().apply {
                put("app", "GharKharcha")
                put("version", 1)
                put("createdAt", timestamp)
                put("photoCount", photoCount)
            }
            val manifestBytes = manifest.toString().toByteArray(Charsets.UTF_8)
            val manifestEntry = ZipEntry("manifest.json")
            zipOut.putNextEntry(manifestEntry)
            zipOut.write(manifestBytes)
            zipOut.closeEntry()
            totalBytes += manifestBytes.size

            prefs.recordBackupCompleted(timestamp)

            BackupMetadata(
                timestamp = timestamp,
                photoCount = photoCount,
                totalBytes = totalBytes,
            )
        }
    }

    suspend fun restoreBackup(sourceUri: Uri): RestoreResult = withContext(io) {
        val inStream = context.contentResolver.openInputStream(sourceUri)
            ?: return@withContext RestoreResult.Failure("Cannot read selected file")

        inStream.use { stream ->
            restoreFromStream(stream)
        }
    }

    suspend fun restoreFromStream(stream: InputStream): RestoreResult = withContext(io) {
        val tempRestoreDir = File(context.cacheDir, "temp_restore_${System.currentTimeMillis()}").apply { mkdirs() }
        val tempDbDir = File(tempRestoreDir, "database").apply { mkdirs() }
        val tempAttachmentsDir = File(tempRestoreDir, AttachmentFileStore.ATTACHMENTS_DIR).apply { mkdirs() }

        var foundDb = false
        var restoredPhotos = 0

        try {
            ZipInputStream(BufferedInputStream(stream)).use { zipIn ->
                var entry: ZipEntry? = zipIn.nextEntry
                while (entry != null) {
                    val entryName = entry.name.replace('\\', '/')
                    if (!entry.isDirectory) {
                        if (entryName.startsWith("database/") && entryName.endsWith(".db")) {
                            val targetFile = File(tempDbDir, GharKharchaDatabase.NAME)
                            FileOutputStream(targetFile).use { out -> zipIn.copyTo(out) }
                            foundDb = true
                        } else if (entryName.startsWith("${AttachmentFileStore.ATTACHMENTS_DIR}/")) {
                            val subPath = entryName.removePrefix("${AttachmentFileStore.ATTACHMENTS_DIR}/")
                            val targetFile = File(tempAttachmentsDir, subPath)
                            targetFile.parentFile?.mkdirs()
                            FileOutputStream(targetFile).use { out -> zipIn.copyTo(out) }
                            restoredPhotos++
                        }
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }

            if (!foundDb) {
                return@withContext RestoreResult.Failure("Invalid backup archive: Database file missing")
            }

            // 1. Close current DB
            try {
                db.close()
            } catch (_: Exception) {}

            // 2. Overwrite database
            val activeDbFile = context.getDatabasePath(GharKharchaDatabase.NAME)
            activeDbFile.parentFile?.mkdirs()
            File(tempDbDir, GharKharchaDatabase.NAME).copyTo(activeDbFile, overwrite = true)

            // Remove any stale WAL/SHM files
            File(activeDbFile.path + "-wal").delete()
            File(activeDbFile.path + "-shm").delete()

            // 3. Overwrite attachments
            val activeAttachmentsDir = File(context.filesDir, AttachmentFileStore.ATTACHMENTS_DIR).apply { mkdirs() }
            tempAttachmentsDir.walkTopDown().filter { it.isFile }.forEach { file ->
                val relativePath = file.relativeTo(tempAttachmentsDir).path
                val targetFile = File(activeAttachmentsDir, relativePath)
                targetFile.parentFile?.mkdirs()
                file.copyTo(targetFile, overwrite = true)
            }

            RestoreResult.Success(restoredPhotos)
        } catch (e: Exception) {
            RestoreResult.Failure(e.message ?: "Failed to extract backup archive")
        } finally {
            tempRestoreDir.deleteRecursively()
        }
    }
}
