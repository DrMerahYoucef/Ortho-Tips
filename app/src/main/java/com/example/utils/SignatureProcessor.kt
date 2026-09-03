package com.example.utils

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

/**
 * Turns a photographed or scanned signature (ink on white/off-white paper) into a
 * clean, transparent-background PNG ready to stamp onto PDF exports:
 *   1. Decodes the source at a bounded resolution.
 *   2. Makes near-white pixels fully transparent, with a soft feathered edge so the
 *      ink strokes don't look jagged.
 *   3. Tightens the crop to the ink's bounding box (plus a small margin).
 *   4. Writes the result as PNG (the only common format here that keeps alpha).
 */
object SignatureProcessor {

    private const val MAX_DIMENSION_PX = 1400
    private const val WHITE_LUMINANCE_THRESHOLD = 205 // 0..255 — at/above this, pixel becomes transparent
    private const val FEATHER_BAND = 40                // luminance band used to soften the cutout edge
    private const val CROP_PADDING_PX = 14

    /** Reads [uri] (gallery pick or scanner output), cleans it, writes a PNG to [destination]. */
    fun processFromUri(contentResolver: ContentResolver, uri: Uri, destination: File) {
        val bitmap = decodeBounded(contentResolver, uri)
        processBitmap(bitmap, destination)
    }

    private fun processBitmap(source: Bitmap, destination: File) {
        val bitmap = source.copy(Bitmap.Config.ARGB_8888, true)
        source.recycle()

        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var minX = width
        var minY = height
        var maxX = 0
        var maxY = 0

        for (y in 0 until height) {
            val rowStart = y * width
            for (x in 0 until width) {
                val idx = rowStart + x
                val pixel = pixels[idx]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val luminance = (0.299 * r + 0.587 * g + 0.114 * b).toInt()

                val alpha = when {
                    luminance >= WHITE_LUMINANCE_THRESHOLD -> 0
                    luminance <= WHITE_LUMINANCE_THRESHOLD - FEATHER_BAND -> 255
                    else -> {
                        val t = (WHITE_LUMINANCE_THRESHOLD - luminance).toFloat() / FEATHER_BAND
                        (t * 255).toInt().coerceIn(0, 255)
                    }
                }

                if (alpha > 0) {
                    // Darken slightly so the ink reads crisp once the paper is gone.
                    val ink = (luminance * 0.6f).toInt().coerceIn(0, 60)
                    pixels[idx] = (alpha shl 24) or (ink shl 16) or (ink shl 8) or ink
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                } else {
                    pixels[idx] = 0
                }
            }
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)

        val hasInk = maxX > minX && maxY > minY
        val cropped = if (hasInk) {
            val left = (minX - CROP_PADDING_PX).coerceAtLeast(0)
            val top = (minY - CROP_PADDING_PX).coerceAtLeast(0)
            val right = (maxX + CROP_PADDING_PX).coerceAtMost(width - 1)
            val bottom = (maxY + CROP_PADDING_PX).coerceAtMost(height - 1)
            Bitmap.createBitmap(
                bitmap,
                left,
                top,
                (right - left).coerceAtLeast(1),
                (bottom - top).coerceAtLeast(1)
            )
        } else {
            bitmap
        }

        destination.parentFile?.mkdirs()
        FileOutputStream(destination).use { out ->
            cropped.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        if (cropped !== bitmap) bitmap.recycle()
        cropped.recycle()
    }

    private fun decodeBounded(contentResolver: ContentResolver, uri: Uri): Bitmap {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, boundsOptions) }

        var sampleSize = 1
        val largestSide = max(boundsOptions.outWidth, boundsOptions.outHeight)
        while (largestSide / sampleSize > MAX_DIMENSION_PX * 2) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        } ?: error("Unable to decode signature image at $uri")
    }
}
