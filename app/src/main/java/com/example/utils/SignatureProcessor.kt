package com.example.utils

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

/** Outcome of a signature cleanup pass — lets the UI tell the surgeon when a retake would help. */
sealed class SignatureResult {
    data class Success(val file: File) : SignatureResult()

    /** Cleaned up and saved, but the source photo made it hard to be sure — still worth a look. */
    data class Warning(val file: File, val message: String) : SignatureResult()

    /** Nothing usable was written. */
    data class Failure(val message: String) : SignatureResult()
}

/**
 * Turns a photographed or scanned signature (ink on white/off-white paper) into a
 * clean, transparent-background PNG ready to stamp onto PDF exports.
 *
 * Uses **local, block-based adaptive thresholding** rather than one fixed
 * brightness cutoff: a phone photo of a page is rarely evenly lit — shadows,
 * glare, and warm indoor light all shift what "white" looks like across the
 * frame. A single global threshold either keeps shadowed paper as a solid dark
 * block, or erases faint ink — so instead each pixel is compared against the
 * *local* background estimate around it (a coarse blurred luminance map).
 */
object SignatureProcessor {

    private const val MAX_DIMENSION_PX = 1400
    private const val BLOCK_GRID = 14          // background is estimated on a ~14x14 grid, then interpolated
    private const val MIN_BLOCK_PX = 20
    private const val LOW_MARGIN = 16f          // luminance drop below local background still counts as paper
    private const val HIGH_MARGIN = 55f         // luminance drop at/above this is unambiguous ink
    private const val CROP_PADDING_PX = 14

    fun processFromUri(contentResolver: ContentResolver, uri: Uri, destination: File): SignatureResult {
        val source = runCatching { decodeBounded(contentResolver, uri) }.getOrNull()
            ?: return SignatureResult.Failure("Couldn't read that image.")
        return processBitmap(source, destination)
    }

    private fun processBitmap(source: Bitmap, destination: File): SignatureResult {
        val bitmap = source.copy(Bitmap.Config.ARGB_8888, true)
        source.recycle()

        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // --- 1. Luminance pass ---
        val luminance = FloatArray(width * height)
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            luminance[i] = 0.299f * r + 0.587f * g + 0.114f * b
        }

        // --- 2. Coarse local-background grid (mean luminance per block) ---
        val blockW = max(MIN_BLOCK_PX, width / BLOCK_GRID)
        val blockH = max(MIN_BLOCK_PX, height / BLOCK_GRID)
        val cols = (width + blockW - 1) / blockW
        val rows = (height + blockH - 1) / blockH
        val blockSum = FloatArray(cols * rows)
        val blockCount = IntArray(cols * rows)

        for (y in 0 until height) {
            val by = (y / blockH).coerceAtMost(rows - 1)
            for (x in 0 until width) {
                val bx = (x / blockW).coerceAtMost(cols - 1)
                val idx = by * cols + bx
                blockSum[idx] += luminance[y * width + x]
                blockCount[idx] += 1
            }
        }
        val blockAvg = FloatArray(cols * rows) { i -> if (blockCount[i] > 0) blockSum[i] / blockCount[i] else 255f }

        // --- 3. Per-pixel alpha via bilinear-interpolated local background ---
        var minX = width
        var minY = height
        var maxX = 0
        var maxY = 0
        var inkPixelCount = 0L

        for (y in 0 until height) {
            // Block-center coordinates this row sits between, for vertical interpolation.
            val fby = (y.toFloat() / blockH) - 0.5f
            val by0 = fby.toInt().coerceIn(0, rows - 1)
            val by1 = (by0 + 1).coerceIn(0, rows - 1)
            val ty = (fby - by0).coerceIn(0f, 1f)

            for (x in 0 until width) {
                val fbx = (x.toFloat() / blockW) - 0.5f
                val bx0 = fbx.toInt().coerceIn(0, cols - 1)
                val bx1 = (bx0 + 1).coerceIn(0, cols - 1)
                val tx = (fbx - bx0).coerceIn(0f, 1f)

                val top = blockAvg[by0 * cols + bx0] * (1 - tx) + blockAvg[by0 * cols + bx1] * tx
                val bottom = blockAvg[by1 * cols + bx0] * (1 - tx) + blockAvg[by1 * cols + bx1] * tx
                val localBackground = top * (1 - ty) + bottom * ty

                val idx = y * width + x
                val diff = localBackground - luminance[idx]

                val alpha = when {
                    diff <= LOW_MARGIN -> 0
                    diff >= HIGH_MARGIN -> 255
                    else -> (((diff - LOW_MARGIN) / (HIGH_MARGIN - LOW_MARGIN)) * 255f).toInt().coerceIn(0, 255)
                }

                if (alpha > 0) {
                    val p = pixels[idx]
                    val r = (p shr 16) and 0xFF
                    val g = (p shr 8) and 0xFF
                    val b = p and 0xFF
                    val srcLum = (0.299f * r + 0.587f * g + 0.114f * b)
                    val ink = (srcLum * 0.45f).toInt().coerceIn(0, 55)
                    pixels[idx] = (alpha shl 24) or (ink shl 16) or (ink shl 8) or ink
                    if (alpha > 60) inkPixelCount++
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

        val totalPixels = (width * height).toLong()
        val inkFraction = inkPixelCount.toDouble() / totalPixels.toDouble()
        val hasInk = maxX > minX && maxY > minY

        if (!hasInk || inkFraction < 0.0015) {
            bitmap.recycle()
            return SignatureResult.Failure(
                "Couldn't find a clear signature in that photo. Try again with the pen strokes " +
                    "on a plain, well-lit, uncreased background."
            )
        }

        val cropped = run {
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
        }

        destination.parentFile?.mkdirs()
        FileOutputStream(destination).use { out ->
            cropped.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        if (cropped !== bitmap) bitmap.recycle()
        cropped.recycle()

        // Too much of the frame reads as "ink" — usually heavy shadow/glare rather than a clean
        // background removal. Still saved (better than nothing) but flagged so the surgeon can retake.
        return if (inkFraction > 0.35) {
            SignatureResult.Warning(
                destination,
                "The lighting made it hard to separate the signature from the page cleanly. " +
                    "If it doesn't look right below, try retaking it in more even light."
            )
        } else {
            SignatureResult.Success(destination)
        }
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
