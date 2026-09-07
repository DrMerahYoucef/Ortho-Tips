package com.example.data.repository

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.data.local.model.ClassificationChapter
import com.example.data.local.model.ClassificationIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Reads the 100% static classification reference data bundled under
 * app/src/main/assets/classifications/ — the index, per-chapter JSON files, and their
 * referenced PNG figures. Nothing here ever writes back to assets; this is a pure reader
 * with a small in-memory cache so re-opening the same chapter doesn't re-parse its JSON.
 */
class ClassificationRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private var indexCache: ClassificationIndex? = null
    private val chapterCache = mutableMapOf<String, ClassificationChapter>()

    suspend fun getIndex(): ClassificationIndex = withContext(Dispatchers.IO) {
        indexCache ?: run {
            val text = context.assets.open("classifications/index.json").bufferedReader().use { it.readText() }
            json.decodeFromString<ClassificationIndex>(text).also { indexCache = it }
        }
    }

    /** [fileRelativePath] is the "file" field from [ChapterRef], e.g. "data/epaule.json". */
    suspend fun getChapter(fileRelativePath: String): ClassificationChapter = withContext(Dispatchers.IO) {
        chapterCache[fileRelativePath] ?: run {
            val text = context.assets.open("classifications/$fileRelativePath").bufferedReader().use { it.readText() }
            json.decodeFromString<ClassificationChapter>(text).also { chapterCache[fileRelativePath] = it }
        }
    }

    /** [relativePath] is an entry from [ClassificationEntry.images], e.g. "images/xxx.png". */
    fun loadAssetImage(relativePath: String): ImageBitmap? = try {
        context.assets.open("classifications/$relativePath").use { stream ->
            BitmapFactory.decodeStream(stream)?.asImageBitmap()
        }
    } catch (e: Exception) {
        null
    }
}