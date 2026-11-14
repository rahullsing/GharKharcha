package com.gharkharcha.data.files

import android.content.Context
import com.gharkharcha.data.entity.OwnerType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-private photo storage: filesDir/attachments/<owner>/<id>/<file>.
 * Paths stored in the DB are relative to filesDir.
 *
 * Photos picked in a form that hasn't been saved yet live in cacheDir/pending_photos and are moved
 * into place on save, so abandoned forms never leave files in the attachments folder.
 */
@Singleton
class AttachmentFileStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    val root: File get() = context.filesDir

    /** Compressed photos not yet attached to a saved record. */
    val pendingDir: File get() = File(context.cacheDir, PENDING_DIR).apply { mkdirs() }

    /** Raw camera captures before compression. */
    val cameraDir: File get() = File(context.cacheDir, CAMERA_DIR).apply { mkdirs() }

    /** Copies made for sharing via FileProvider (cache-path "shared"). */
    val shareDir: File get() = File(context.cacheDir, SHARE_DIR).apply { mkdirs() }

    fun resolve(relativePath: String): File = File(root, relativePath)

    fun relativeDirFor(ownerType: OwnerType, ownerId: Long): String =
        "$ATTACHMENTS_DIR/${ownerType.name.lowercase()}/$ownerId"

    fun dirFor(ownerType: OwnerType, ownerId: Long): File =
        File(root, relativeDirFor(ownerType, ownerId)).apply { mkdirs() }

    fun newPendingFile(): File = File(pendingDir, "${UUID.randomUUID()}.jpg")

    fun newCameraFile(): File = File(cameraDir, "${UUID.randomUUID()}.jpg")

    /** Moves a pending file under the owner's folder; returns the relative path to store. */
    fun moveIntoOwner(file: File, ownerType: OwnerType, ownerId: Long): String {
        val target = File(dirFor(ownerType, ownerId), file.name)
        if (!file.renameTo(target)) {
            file.copyTo(target, overwrite = true)
            file.delete()
        }
        return "${relativeDirFor(ownerType, ownerId)}/${target.name}"
    }

    /** Deletes a file and prunes now-empty parent dirs up to attachments/. Returns true if gone. */
    fun delete(relativePath: String): Boolean {
        val file = resolve(relativePath)
        val deleted = !file.exists() || file.delete()
        var parent = file.parentFile
        val stop = File(root, ATTACHMENTS_DIR)
        while (parent != null && parent != stop && parent.isDirectory && parent.list()?.isEmpty() == true) {
            parent.delete()
            parent = parent.parentFile
        }
        return deleted
    }

    /** Removes pending/camera/share leftovers older than [maxAgeMillis] (abandoned forms). */
    fun cleanCache(maxAgeMillis: Long = DAY_MILLIS, now: Long = System.currentTimeMillis()) {
        listOf(pendingDir, cameraDir, shareDir).forEach { dir ->
            dir.listFiles()?.filter { now - it.lastModified() > maxAgeMillis }?.forEach { it.delete() }
        }
    }

    companion object {
        const val ATTACHMENTS_DIR = "attachments"
        private const val PENDING_DIR = "pending_photos"
        private const val CAMERA_DIR = "camera"
        private const val SHARE_DIR = "shared"
        private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}
