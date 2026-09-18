package com.example.data.local.model

import kotlinx.serialization.Serializable

/**
 * Models for the statically-bundled classification reference data shipped under
 * app/src/main/assets/classifications/. These files are never modified by the app —
 * they are read-only reference content parsed at runtime by [com.example.data.repository.ClassificationRepository].
 *
 * Schema (do not rename fields; must match the asset JSON exactly):
 *
 * index.json:
 * { "chapters": [ { "id": String, "title": String, "file": String, "sectionCount": Int, "classificationCount": Int } ] }
 *
 * data/<chapitre>.json:
 * { "id": String, "title": String, "sections": [ { "id", "title", "classifications": [ ... ] } ] }
 */

@Serializable
data class ClassificationIndex(
    val chapters: List<ChapterRef> = emptyList()
)

@Serializable
data class ChapterRef(
    val id: String,
    val title: String,
    val file: String,
    val sectionCount: Int = 0,
    val classificationCount: Int = 0
)

@Serializable
data class ClassificationChapter(
    val id: String,
    val title: String,
    val sections: List<ClassificationSection> = emptyList()
)

@Serializable
data class ClassificationSection(
    val id: String,
    val title: String,
    val classifications: List<ClassificationEntry> = emptyList()
)

@Serializable
data class ClassificationEntry(
    val id: String,
    val title: String,
    val images: List<String> = emptyList(),
    val blocks: List<ClassificationBlock> = emptyList()
)

/**
 * A single content block within a classification entry. [type] is one of "text", "item",
 * or "table" — fields not relevant to that type are simply left at their defaults.
 */
@Serializable
data class ClassificationBlock(
    val type: String,
    val lines: List<String> = emptyList(),
    val label: String? = null,
    val text: String? = null,
    val bullets: List<String> = emptyList(),
    val sub: List<ClassificationSubGroup> = emptyList(),
    val headers: List<String> = emptyList(),
    val rows: List<List<String>> = emptyList()
)

@Serializable
data class ClassificationSubGroup(
    val label: String,
    val bullets: List<String> = emptyList()
)
