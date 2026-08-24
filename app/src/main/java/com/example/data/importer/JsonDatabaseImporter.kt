package com.example.data.importer

import android.content.Context
import android.util.Base64
import com.example.data.local.OrthoDatabase
import com.example.data.local.entity.BoneEntity
import com.example.data.local.entity.ChapterEntity
import com.example.data.local.entity.ImageEntity
import com.example.data.local.entity.SectionEntity
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Result of a successful [JsonDatabaseImporter.importOrthoJson] call.
 */
data class ImportSummary(
    val chapterCount: Int,
    val boneCount: Int,
    val pageCount: Int,
    val figureCount: Int
)

/**
 * Imports the clinical/bone-fracture reference JSON database into the local Room database.
 *
 * Expected schema:
 * ```
 * {
 *   "bones": [
 *     {
 *       "bone": "NAME_OF_BONE",
 *       "region": "ANATOMICAL_REGION",
 *       "notes": "GENERAL_NOTES_ABOUT_THIS_BONE",
 *       "pages": [
 *         {
 *           "page": "PDF_PAGE_NUMBER",
 *           "chapter": "CHAPTER_NUMBER",
 *           "chapterTitle": "FRACTURE_CHAPTER_TITLE",
 *           "section": "BODY_REGION_SECTION",
 *           "figureLabels": ["FIGURE_NUMBER_1"],
 *           "text": "FULL_TEXT_CONTENT_OF_THE_PAGE",
 *           "figures": [
 *             { "index": 0, "label": "FIGURE_LABEL", "width": 0, "height": 0, "mime": "image/jpeg", "image": "BASE64..." }
 *           ]
 *         }
 *       ]
 *     }
 *   ]
 * }
 * ```
 * Chapters (top-level categories) are derived from each bone's distinct "region".
 * Every "page" becomes a section on its bone, and every "figure" is decoded from
 * base64 and written to local app storage under [IMAGES_DIR_NAME].
 */
class JsonDatabaseImporter(
    private val context: Context,
    private val database: OrthoDatabase
) {

    private var idCounter = 0

    suspend fun importOrthoJson(jsonContent: String): Result<ImportSummary> = withContext(Dispatchers.IO) {
        try {
            val root: JSONObject
            try {
                root = JSONObject(jsonContent)
            } catch (e: Exception) {
                return@withContext Result.failure(
                    IllegalArgumentException("The selected file is not valid JSON.")
                )
            }

            if (!root.has("bones")) {
                return@withContext Result.failure(
                    IllegalArgumentException("Invalid database file: missing required \"bones\" array.")
                )
            }

            val bonesArray = try {
                root.getJSONArray("bones")
            } catch (e: Exception) {
                return@withContext Result.failure(
                    IllegalArgumentException("Invalid database file: \"bones\" must be an array.")
                )
            }

            val chaptersByRegion = LinkedHashMap<String, ChapterEntity>()
            val bones = mutableListOf<BoneEntity>()
            val sections = mutableListOf<SectionEntity>()
            val images = mutableListOf<ImageEntity>()

            val imagesDir = File(context.filesDir, IMAGES_DIR_NAME)
            imagesDir.mkdirs()

            var pageCount = 0
            var figureCount = 0

            for (b in 0 until bonesArray.length()) {
                val boneObj = bonesArray.optJSONObject(b) ?: continue
                val boneName = boneObj.optString("bone").ifBlank { "Untitled Bone ${b + 1}" }
                val region = boneObj.optString("region").ifBlank { "General" }
                val notes = boneObj.optString("notes", "")

                val chapter = chaptersByRegion.getOrPut(region) {
                    ChapterEntity(
                        id = slug("chap_$region"),
                        title = region,
                        description = "Imported clinical reference data",
                        icon = "medical_services",
                        orderIndex = chaptersByRegion.size + 1,
                        regionTag = region
                    )
                }

                val boneId = uniqueId("bone_${boneName}")
                bones.add(
                    BoneEntity(
                        id = boneId,
                        chapterId = chapter.id,
                        name = boneName,
                        subtitle = notes.take(140),
                        region = region,
                        traumaType = "",
                        classificationSummary = notes,
                        orderIndex = b + 1,
                        isHighYield = false
                    )
                )

                val pagesArray = boneObj.optJSONArray("pages") ?: JSONArray()
                for (p in 0 until pagesArray.length()) {
                    val pageObj = pagesArray.optJSONObject(p) ?: continue
                    val pageNumber = pageObj.optString("page", "${p + 1}")
                    val chapterNumber = pageObj.optString("chapter", "")
                    val chapterTitle = pageObj.optString("chapterTitle", "")
                    val sectionName = pageObj.optString("section", "")
                    val text = pageObj.optString("text", "")

                    val sectionId = uniqueId("${boneId}_page_$pageNumber")
                    val sectionTitle = chapterTitle.ifBlank { sectionName.ifBlank { "Page $pageNumber" } }
                    val sectionType = when {
                        chapterNumber.isNotBlank() -> "Ch. $chapterNumber"
                        sectionName.isNotBlank() -> sectionName
                        else -> "REFERENCE"
                    }

                    sections.add(
                        SectionEntity(
                            id = sectionId,
                            boneId = boneId,
                            sectionType = sectionType,
                            title = sectionTitle,
                            content = text,
                            pearls = "",
                            warnings = "",
                            orderIndex = p + 1
                        )
                    )
                    pageCount++

                    val figuresArray = pageObj.optJSONArray("figures") ?: JSONArray()
                    for (f in 0 until figuresArray.length()) {
                        val figObj = figuresArray.optJSONObject(f) ?: continue
                        val base64Data = figObj.optString("image", "")
                        if (base64Data.isBlank()) continue

                        val label = figObj.optString("label", "Fig ${f + 1}")
                        val mime = figObj.optString("mime", "image/jpeg")
                        val imageId = uniqueId("${sectionId}_fig_$f")
                        val imageFile = File(imagesDir, "$imageId.${mimeToExtension(mime)}")

                        val decoded = try {
                            Base64.decode(base64Data, Base64.DEFAULT)
                        } catch (e: Exception) {
                            null
                        }
                        if (decoded == null) continue

                        try {
                            FileOutputStream(imageFile).use { it.write(decoded) }
                        } catch (e: Exception) {
                            continue
                        }

                        images.add(
                            ImageEntity(
                                id = imageId,
                                boneId = boneId,
                                sectionId = sectionId,
                                title = label,
                                description = "$boneName • $sectionTitle",
                                drawableResName = "",
                                imagePath = imageFile.absolutePath,
                                figureNumber = label
                            )
                        )
                        figureCount++
                    }
                }
            }

            if (bones.isEmpty()) {
                return@withContext Result.failure(
                    IllegalArgumentException("No bones found in the selected file.")
                )
            }

            database.chapterDao().insertChapters(chaptersByRegion.values.toList())
            database.boneDao().insertBones(bones)
            if (sections.isNotEmpty()) database.contentDao().insertSections(sections)
            if (images.isNotEmpty()) database.imageDao().insertImages(images)

            Result.success(
                ImportSummary(
                    chapterCount = chaptersByRegion.size,
                    boneCount = bones.size,
                    pageCount = pageCount,
                    figureCount = figureCount
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mimeToExtension(mime: String): String = when {
        mime.contains("png", ignoreCase = true) -> "png"
        mime.contains("webp", ignoreCase = true) -> "webp"
        else -> "jpg"
    }

    private fun slug(input: String): String =
        input.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "item" }

    private fun uniqueId(base: String): String {
        idCounter++
        return "${slug(base)}_$idCounter"
    }

    companion object {
        const val IMAGES_DIR_NAME = "imported_figures"
    }
}
