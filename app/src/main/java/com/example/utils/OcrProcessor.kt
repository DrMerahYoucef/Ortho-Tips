package com.example.utils

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Runs Google ML Kit's on-device Text Recognition against a saved patient image.
 * On-device (not cloud) so it works fully offline, matching the rest of the module.
 */
object OcrProcessor {

    /** Returns the recognized text, or an empty string if nothing was found or recognition failed. */
    suspend fun extractText(context: Context, imageFile: File): String = withContext(Dispatchers.IO) {
        if (!imageFile.exists()) return@withContext ""
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val inputImage = InputImage.fromFilePath(context, android.net.Uri.fromFile(imageFile))
            val result = Tasks.await(recognizer.process(inputImage))
            result.text
        } catch (_: Exception) {
            ""
        } finally {
            recognizer.close()
        }
    }
}
