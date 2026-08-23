package com.example.data.importer

import com.example.data.local.OrthoDatabase
import com.example.data.local.entity.BoneEntity
import com.example.data.local.entity.ChapterEntity
import com.example.data.local.entity.SectionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class JsonDatabaseImporter(private val database: OrthoDatabase) {

    suspend fun importOrthoJson(jsonContent: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonContent)
            var totalCount = 0

            // If JSON contains chapters
            if (root.has("chapters")) {
                val chaptersArray = root.getJSONArray("chapters")
                val chapterList = mutableListOf<ChapterEntity>()
                for (i in 0 until chaptersArray.length()) {
                    val obj = chaptersArray.getJSONObject(i)
                    chapterList.add(
                        ChapterEntity(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            description = obj.optString("description", ""),
                            icon = obj.optString("icon", "medical_services"),
                            orderIndex = obj.optInt("orderIndex", i + 1),
                            regionTag = obj.optString("regionTag", "General")
                        )
                    )
                }
                database.chapterDao().insertChapters(chapterList)
                totalCount += chapterList.size
            }

            // If JSON contains bones / techniques
            if (root.has("bones")) {
                val bonesArray = root.getJSONArray("bones")
                val boneList = mutableListOf<BoneEntity>()
                val sectionList = mutableListOf<SectionEntity>()

                for (i in 0 until bonesArray.length()) {
                    val obj = bonesArray.getJSONObject(i)
                    val boneId = obj.getString("id")
                    boneList.add(
                        BoneEntity(
                            id = boneId,
                            chapterId = obj.optString("chapterId", "chap_upper_limb"),
                            name = obj.getString("name"),
                            subtitle = obj.optString("subtitle", ""),
                            region = obj.optString("region", "General"),
                            traumaType = obj.optString("traumaType", ""),
                            classificationSummary = obj.optString("classificationSummary", ""),
                            orderIndex = obj.optInt("orderIndex", i + 1),
                            isHighYield = obj.optBoolean("isHighYield", true)
                        )
                    )

                    if (obj.has("sections")) {
                        val secArray = obj.getJSONArray("sections")
                        for (s in 0 until secArray.length()) {
                            val secObj = secArray.getJSONObject(s)
                            sectionList.add(
                                SectionEntity(
                                    id = secObj.optString("id", "${boneId}_sec_$s"),
                                    boneId = boneId,
                                    sectionType = secObj.optString("sectionType", "PEARLS"),
                                    title = secObj.getString("title"),
                                    content = secObj.getString("content"),
                                    pearls = secObj.optString("pearls", ""),
                                    warnings = secObj.optString("warnings", ""),
                                    orderIndex = secObj.optInt("orderIndex", s)
                                )
                            )
                        }
                    }
                }

                database.boneDao().insertBones(boneList)
                if (sectionList.isNotEmpty()) {
                    database.contentDao().insertSections(sectionList)
                }
                totalCount += boneList.size
            }

            Result.success(totalCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
