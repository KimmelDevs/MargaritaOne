package com.kimmeldevs.margaritaone.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

/**
 * Shrinks a photo to at most [maxDimension] px on its longest side and re-encodes it as JPEG.
 * Re-encoding also drops the original EXIF metadata (including any embedded GPS), which is what we want:
 * the report's location is stored separately and only when the user chooses to attach it.
 */
object ImageCompressor {

    suspend fun compressToJpeg(
        context: Context,
        uri: Uri,
        maxDimension: Int = 1600,
        quality: Int = 80
    ): ByteArray = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Couldn't read the selected image." }

        var sampleSize = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= maxDimension) sampleSize *= 2

        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sampleSize })
        } ?: error("Couldn't read the selected image.")

        val rotation = resolver.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees } ?: 0

        val scale = maxDimension.toFloat() / max(decoded.width, decoded.height)
        val matrix = Matrix().apply {
            if (scale < 1f) postScale(scale, scale)
            if (rotation != 0) postRotate(rotation.toFloat())
        }
        val output = if (matrix.isIdentity) {
            decoded
        } else {
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        }

        ByteArrayOutputStream().use { stream ->
            output.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            if (output !== decoded) output.recycle()
            decoded.recycle()
            stream.toByteArray()
        }
    }
}
