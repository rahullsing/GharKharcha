package com.gharkharcha.data.files

import android.content.Context
import com.gharkharcha.data.entity.OwnerType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-private photo storage: filesDir/attachments/<owner>/<id>/<file>.
 * Paths stored in the DB are relative to filesDir.
 */
@Singleton
class AttachmentFileStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    val root: File get() = context.filesDir

    fun resolve(relativePath: String): File = File(root, relativePath)

    fun relativeDirFor(ownerType: OwnerType, ownerId: Long): String =
        "$ATTACHMENTS_DIR/${ownerType.name.lowercase()}/$ownerId"

    fun dirFor(ownerType: OwnerType, ownerId: Long): File =
        File(root, relativeDirFor(ownerType, ownerId)).apply { mkdirs() }

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

    companion object {
        const val ATTACHMENTS_DIR = "attachments"
    }
}
