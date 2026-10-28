package com.alona.imageloader.internal

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

/** Decodes images downsampled to the target size to avoid loading full-resolution bitmaps. */
internal object BitmapDecoder {

    fun decode(file: File, reqWidth: Int, reqHeight: Int): Bitmap? =
        decode(reqWidth, reqHeight) { options -> BitmapFactory.decodeFile(file.path, options) }

    fun decode(bytes: ByteArray, reqWidth: Int, reqHeight: Int): Bitmap? =
        decode(reqWidth, reqHeight) { options -> BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) }

    /**
     * Largest power of two that keeps both decoded dimensions at least the requested size,
     * so the image still fills the view (e.g. with centerCrop) without visible upscaling.
     */
    fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
        if (reqWidth <= 0 || reqHeight <= 0) return 1
        var sampleSize = 1
        while (width / (sampleSize * 2) >= reqWidth && height / (sampleSize * 2) >= reqHeight) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private inline fun decode(
        reqWidth: Int,
        reqHeight: Int,
        decoder: (BitmapFactory.Options) -> Bitmap?,
    ): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        decoder(bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, reqWidth, reqHeight)
        }
        return decoder(options)
    }
}
