package com.gharkharcha.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A photo/file belonging to any entity. Polymorphic ([ownerType], [ownerId]) so there is no
 * FK; repositories delete attachment rows and files when the owner is deleted.
 *
 * [filePath] is relative to `filesDir` (e.g. `attachments/payment/12/abc.jpg`) so a backup
 * restored on another device still resolves.
 */
@Entity(
    tableName = "attachments",
    indices = [Index(value = ["owner_type", "owner_id", "sort_order"])],
)
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "owner_type") val ownerType: OwnerType,
    @ColumnInfo(name = "owner_id") val ownerId: Long,
    val kind: AttachmentKind = AttachmentKind.PHOTO,
    @ColumnInfo(name = "file_path") val filePath: String,
    @ColumnInfo(name = "mime_type") val mimeType: String = "image/jpeg",
    val caption: String = "",
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0,
    /** EXIF capture time if known. */
    @ColumnInfo(name = "taken_at") val takenAt: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
)
