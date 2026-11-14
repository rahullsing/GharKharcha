package com.gharkharcha.domain.model

import com.gharkharcha.data.entity.AttachmentKind
import kotlinx.serialization.Serializable

/**
 * A photo shown in an edit form. New photos ([attachmentId] == null) point at a compressed file in
 * the pending folder until the record is saved; existing ones point at their stored file.
 * Serializable so a half-filled form keeps its photos across process death.
 */
@Serializable
data class PhotoItem(
    val key: String,
    val path: String,
    val kind: AttachmentKind,
    val attachmentId: Long? = null,
    val caption: String = "",
    val takenAt: Long? = null,
)
