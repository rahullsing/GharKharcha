package com.gharkharcha.util

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ShareIntentParserTest {

    @Test
    fun `extractImageUris returns empty list for null intent`() {
        val result = ShareIntentParser.extractImageUris(null)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `extractImageUris returns empty list for non-image intent`() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Hello")
        }
        val result = ShareIntentParser.extractImageUris(intent)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `extractImageUris parses ACTION_SEND with EXTRA_STREAM`() {
        val uri = Uri.parse("content://media/external/images/media/1")
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
        }
        val result = ShareIntentParser.extractImageUris(intent)
        assertEquals(listOf(uri), result)
    }

    @Test
    fun `extractImageUris parses ACTION_SEND_MULTIPLE with EXTRA_STREAM list`() {
        val uri1 = Uri.parse("content://media/external/images/media/1")
        val uri2 = Uri.parse("content://media/external/images/media/2")
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/jpeg"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(uri1, uri2))
        }
        val result = ShareIntentParser.extractImageUris(intent)
        assertEquals(listOf(uri1, uri2), result)
    }

    @Test
    fun `extractImageUris parses ClipData when extra stream is null`() {
        val uri = Uri.parse("content://media/external/images/media/3")
        val item = ClipData.Item(uri)
        val clip = ClipData("images", arrayOf("image/*"), item)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            clipData = clip
        }
        val result = ShareIntentParser.extractImageUris(intent)
        assertEquals(listOf(uri), result)
    }
}
