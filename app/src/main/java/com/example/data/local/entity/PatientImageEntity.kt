package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ImageCategory {
    RADIOLOGY, CLINICAL_PHOTO, DOCUMENT_SCAN, OTHER
}

/**
 * An image attached to a patient. [timelineEventId] and [surgeryId] are optional
 * links so the same image row can surface both on the patient's media grid and,
 * when relevant, inline in a specific timeline entry or surgery record.
 *
 * [filePath]/[thumbnailPath] are absolute paths under the app's private storage
 * (never MediaStore/shared storage) so patient media stays sandboxed and is
 * included automatically by the ZIP export/import flow.
 */
@Entity(
    tableName = "patient_images",
    foreignKeys = [
        ForeignKey(
            entity = PatientEntity::class,
            parentColumns = ["id"],
            childColumns = ["patientId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TimelineEventEntity::class,
            parentColumns = ["id"],
            childColumns = ["timelineEventId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = SurgeryEntity::class,
            parentColumns = ["id"],
            childColumns = ["surgeryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("patientId"),
        Index("timelineEventId"),
        Index("surgeryId"),
        Index("category")
    ]
)
data class PatientImageEntity(
    @PrimaryKey val id: String,
    val patientId: String,
    val timelineEventId: String? = null,
    val surgeryId: String? = null,
    val category: String = ImageCategory.OTHER.name,
    val filePath: String,
    val thumbnailPath: String? = null,
    val ocrText: String = "",
    /** Serialized (JSON) list of annotation overlays: arrows, text labels, shapes. */
    val annotationsJson: String = "",
    val notes: String = "",
    val capturedAt: Long = System.currentTimeMillis()
)
