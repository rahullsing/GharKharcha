package com.gharkharcha.util

import android.content.Intent
import android.net.Uri

object ShareIntentParser {
    /**
     * Extracts all incoming image URIs from an [Intent.ACTION_SEND] or [Intent.ACTION_SEND_MULTIPLE]
     * intent. Returns an empty list if the intent is not an image share.
     */
    fun extractImageUris(intent: Intent?): List<Uri> {
        if (intent == null) return emptyList()
        val action = intent.action ?: return emptyList()
        val type = intent.type ?: return emptyList()
        if (!type.startsWith("image/")) return emptyList()

        val uris = mutableListOf<Uri>()
        when (action) {
            Intent.ACTION_SEND -> {
                val uri = intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                if (uri != null) {
                    uris += uri
                } else {
                    val clip = intent.clipData
                    if (clip != null) {
                        for (i in 0 until clip.itemCount) {
                            clip.getItemAt(i).uri?.let { uris += it }
                        }
                    }
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val list = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                if (list != null) {
                    uris += list
                } else {
                    val clip = intent.clipData
                    if (clip != null) {
                        for (i in 0 until clip.itemCount) {
                            clip.getItemAt(i).uri?.let { uris += it }
                        }
                    }
                }
            }
        }
        return uris.distinct()
    }
}
