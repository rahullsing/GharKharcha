package com.gharkharcha.data.files

import com.gharkharcha.domain.model.PhotoItem
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory bridge for photos imported from an external share intent ([android.content.Intent.ACTION_SEND]
 * or [android.content.Intent.ACTION_SEND_MULTIPLE]) into a new payment or material form.
 */
@Singleton
class SharedPhotoHolder @Inject constructor() {
    private val pendingPhotos = mutableListOf<PhotoItem>()

    fun set(photos: List<PhotoItem>) {
        pendingPhotos.clear()
        pendingPhotos.addAll(photos)
    }

    /** Returns and clears the pending photos so they are only consumed once. */
    fun consume(): List<PhotoItem> {
        val list = pendingPhotos.toList()
        pendingPhotos.clear()
        return list
    }

    fun hasPending(): Boolean = pendingPhotos.isNotEmpty()
}
