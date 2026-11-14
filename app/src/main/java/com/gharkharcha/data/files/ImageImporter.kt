package com.gharkharcha.data.files

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.gharkharcha.data.entity.AttachmentKind
import com.gharkharcha.di.IoDispatcher
import com.gharkharcha.domain.model.PhotoItem
import kotlinx.coroutines.CancellationException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.roundToInt

/** A compressed photo ready to be attached. [takenAt] is the EXIF capture time when known. */
data class ImportedImage(val file: File, val takenAt: Long?)

/**
 * Turns a picked/captured image into a small JPEG: longest side ≤ 1600 px, quality 80,
 * orientation applied, original EXIF capture date kept.
 */
@Singleton
class ImageImporter @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val files: AttachmentFileStore,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) {
    suspend fun import(uri: Uri): ImportedImage = withContext(io) {
        val dates = context.contentResolver.openInputStream(uri)?.use(::readDates)
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        compress(source, dates)
    }

    /** For CameraX captures; the raw file is deleted afterwards. */
    suspend fun import(file: File): ImportedImage = withContext(io) {
        try {
            val dates = file.inputStream().use(::readDates)
            compress(ImageDecoder.createSource(file), dates)
        } finally {
            file.delete()
        }
    }

    /** Imports each picked image; ones that fail to decode are skipped (count returned separately). */
    suspend fun importAll(uris: List<Uri>, kind: AttachmentKind): Pair<List<PhotoItem>, Int> {
        val items = mutableListOf<PhotoItem>()
        var failed = 0
        for (uri in uris) {
            try {
                items += import(uri).toItem(kind)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                failed++
            }
        }
        return items to failed
    }

    /** Imports a camera capture; null if it couldn't be read. */
    suspend fun importCapture(path: String, kind: AttachmentKind): PhotoItem? = try {
        import(File(path)).toItem(kind)
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    private fun ImportedImage.toItem(kind: AttachmentKind) =
        PhotoItem(key = UUID.randomUUID().toString(), path = file.absolutePath, kind = kind, takenAt = takenAt)

    private data class ExifDates(val original: String?, val millis: Long?)

    private fun readDates(input: InputStream): ExifDates = runCatching {
        val exif = ExifInterface(input)
        ExifDates(
            original = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL) ?: exif.getAttribute(ExifInterface.TAG_DATETIME),
            millis = exif.dateTimeOriginal ?: exif.dateTime,
        )
    }.getOrDefault(ExifDates(null, null))

    private fun compress(source: ImageDecoder.Source, dates: ExifDates?): ImportedImage {
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val longest = max(info.size.width, info.size.height)
            if (longest > MAX_SIDE) {
                val scale = MAX_SIDE.toFloat() / longest
                decoder.setTargetSize(
                    (info.size.width * scale).roundToInt().coerceAtLeast(1),
                    (info.size.height * scale).roundToInt().coerceAtLeast(1),
                )
            }
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        val out = files.newPendingFile()
        try {
            FileOutputStream(out).use { bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, it) }
        } finally {
            bitmap.recycle()
        }
        dates?.original?.let { date ->
            runCatching {
                ExifInterface(out).apply {
                    setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, date)
                    setAttribute(ExifInterface.TAG_DATETIME, date)
                    saveAttributes()
                }
            }
        }
        return ImportedImage(out, dates?.millis)
    }

    private companion object {
        const val MAX_SIDE = 1600
        const val QUALITY = 80
    }
}
