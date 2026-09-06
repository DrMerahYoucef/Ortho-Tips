package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.BoneDao
import com.example.data.local.dao.ChapterDao
import com.example.data.local.dao.ContentDao
import com.example.data.local.dao.ImageDao
import com.example.data.local.dao.PatientDao
import com.example.data.local.dao.PatientDocumentDao
import com.example.data.local.dao.PatientImageDao
import com.example.data.local.dao.SearchDao
import com.example.data.local.dao.SuggestionDao
import com.example.data.local.dao.SurgeryDao
import com.example.data.local.dao.TimelineEventDao
import com.example.data.local.dao.UserDataDao
import com.example.data.local.entity.BookmarkEntity
import com.example.data.local.entity.BoneEntity
import com.example.data.local.entity.ChapterEntity
import com.example.data.local.entity.FieldSuggestionEntity
import com.example.data.local.entity.ImageEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.PatientDocumentEntity
import com.example.data.local.entity.PatientEntity
import com.example.data.local.entity.PatientImageEntity
import com.example.data.local.entity.SectionEntity
import com.example.data.local.entity.SurgeryEntity
import com.example.data.local.entity.TimelineEventEntity

@Database(
    entities = [
        ChapterEntity::class,
        BoneEntity::class,
        SectionEntity::class,
        ImageEntity::class,
        BookmarkEntity::class,
        NoteEntity::class,
        PatientEntity::class,
        TimelineEventEntity::class,
        SurgeryEntity::class,
        PatientImageEntity::class,
        PatientDocumentEntity::class,
        FieldSuggestionEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class OrthoDatabase : RoomDatabase() {
    abstract fun chapterDao(): ChapterDao
    abstract fun boneDao(): BoneDao
    abstract fun contentDao(): ContentDao
    abstract fun imageDao(): ImageDao
    abstract fun userDataDao(): UserDataDao
    abstract fun searchDao(): SearchDao
    abstract fun suggestionDao(): SuggestionDao

    // Patient management module
    abstract fun patientDao(): PatientDao
    abstract fun timelineEventDao(): TimelineEventDao
    abstract fun surgeryDao(): SurgeryDao
    abstract fun patientImageDao(): PatientImageDao
    abstract fun patientDocumentDao(): PatientDocumentDao

    companion object {
        @Volatile
        private var INSTANCE: OrthoDatabase? = null

        /**
         * Adds the patient management tables. Existing clinical reference data
         * (chapters/bones/sections/images/bookmarks/notes) is left untouched.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `patients` (
                        `id` TEXT NOT NULL,
                        `patientCode` TEXT NOT NULL,
                        `fullName` TEXT NOT NULL,
                        `dateOfBirth` INTEGER,
                        `gender` TEXT NOT NULL,
                        `phone` TEXT NOT NULL,
                        `email` TEXT NOT NULL,
                        `address` TEXT NOT NULL,
                        `emergencyContactName` TEXT NOT NULL,
                        `emergencyContactPhone` TEXT NOT NULL,
                        `insuranceProvider` TEXT NOT NULL,
                        `insuranceNumber` TEXT NOT NULL,
                        `medicalHistory` TEXT NOT NULL,
                        `allergies` TEXT NOT NULL,
                        `chronicDiseases` TEXT NOT NULL,
                        `previousSurgeries` TEXT NOT NULL,
                        `medications` TEXT NOT NULL,
                        `generalNotes` TEXT NOT NULL,
                        `diagnosis` TEXT NOT NULL,
                        `bodyRegion` TEXT NOT NULL,
                        `side` TEXT NOT NULL,
                        `injuryDate` INTEGER,
                        `traumaMechanism` TEXT NOT NULL,
                        `fractureClassification` TEXT NOT NULL,
                        `imagingFindings` TEXT NOT NULL,
                        `treatmentPlan` TEXT NOT NULL,
                        `isArchived` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_patients_patientCode` ON `patients` (`patientCode`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_patients_fullName` ON `patients` (`fullName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_patients_diagnosis` ON `patients` (`diagnosis`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_patients_bodyRegion` ON `patients` (`bodyRegion`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_patients_isArchived` ON `patients` (`isArchived`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `timeline_events` (
                        `id` TEXT NOT NULL,
                        `patientId` TEXT NOT NULL,
                        `eventDate` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`patientId`) REFERENCES `patients`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_timeline_events_patientId` ON `timeline_events` (`patientId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_timeline_events_eventDate` ON `timeline_events` (`eventDate`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `surgeries` (
                        `id` TEXT NOT NULL,
                        `patientId` TEXT NOT NULL,
                        `surgeryDate` INTEGER NOT NULL,
                        `hospitalClinic` TEXT NOT NULL,
                        `surgeon` TEXT NOT NULL,
                        `assistants` TEXT NOT NULL,
                        `anesthesiaType` TEXT NOT NULL,
                        `diagnosis` TEXT NOT NULL,
                        `procedure` TEXT NOT NULL,
                        `materialsUsed` TEXT NOT NULL,
                        `complications` TEXT NOT NULL,
                        `postOpInstructions` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`patientId`) REFERENCES `patients`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_surgeries_patientId` ON `surgeries` (`patientId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_surgeries_surgeryDate` ON `surgeries` (`surgeryDate`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `patient_images` (
                        `id` TEXT NOT NULL,
                        `patientId` TEXT NOT NULL,
                        `timelineEventId` TEXT,
                        `surgeryId` TEXT,
                        `category` TEXT NOT NULL,
                        `filePath` TEXT NOT NULL,
                        `thumbnailPath` TEXT,
                        `ocrText` TEXT NOT NULL,
                        `annotationsJson` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `capturedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`patientId`) REFERENCES `patients`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`timelineEventId`) REFERENCES `timeline_events`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(`surgeryId`) REFERENCES `surgeries`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_patient_images_patientId` ON `patient_images` (`patientId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_patient_images_timelineEventId` ON `patient_images` (`timelineEventId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_patient_images_surgeryId` ON `patient_images` (`surgeryId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_patient_images_category` ON `patient_images` (`category`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `patient_documents` (
                        `id` TEXT NOT NULL,
                        `patientId` TEXT NOT NULL,
                        `timelineEventId` TEXT,
                        `surgeryId` TEXT,
                        `title` TEXT NOT NULL,
                        `filePath` TEXT NOT NULL,
                        `mimeType` TEXT NOT NULL,
                        `ocrText` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`patientId`) REFERENCES `patients`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`timelineEventId`) REFERENCES `timeline_events`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(`surgeryId`) REFERENCES `surgeries`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_patient_documents_patientId` ON `patient_documents` (`patientId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_patient_documents_timelineEventId` ON `patient_documents` (`timelineEventId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_patient_documents_surgeryId` ON `patient_documents` (`surgeryId`)")
            }
        }

        /**
         * Adds the operative-protocol field to surgeries and the learned-suggestions
         * table backing the smart autocomplete inputs. Nothing else is touched.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE surgeries ADD COLUMN operativeProtocol TEXT NOT NULL DEFAULT ''")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `field_suggestions` (
                        `fieldKey` TEXT NOT NULL,
                        `value` TEXT NOT NULL,
                        `useCount` INTEGER NOT NULL,
                        `lastUsedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`fieldKey`, `value`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_field_suggestions_fieldKey` ON `field_suggestions` (`fieldKey`)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Ensure table integrity between versions
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `field_suggestions` (
                        `fieldKey` TEXT NOT NULL,
                        `value` TEXT NOT NULL,
                        `useCount` INTEGER NOT NULL,
                        `lastUsedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`fieldKey`, `value`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_field_suggestions_fieldKey` ON `field_suggestions` (`fieldKey`)")
            }
        }

        fun getInstance(context: Context): OrthoDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): OrthoDatabase {
            val db = Room.databaseBuilder(
                context.applicationContext,
                OrthoDatabase::class.java,
                "ortho_tips.db"
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .fallbackToDestructiveMigration(true)
                .fallbackToDestructiveMigrationOnDowngrade(true)
                .build()

            return try {
                // Eagerly verify SQLite connectivity and schema integrity
                db.openHelper.writableDatabase
                db
            } catch (e: Throwable) {
                // If any schema mismatch or database corruption exists on disk,
                // safely wipe the invalid database and re-create it cleanly
                try {
                    db.close()
                } catch (_: Throwable) {}
                context.applicationContext.deleteDatabase("ortho_tips.db")
                val cleanDb = Room.databaseBuilder(
                    context.applicationContext,
                    OrthoDatabase::class.java,
                    "ortho_tips.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .fallbackToDestructiveMigrationOnDowngrade(true)
                    .build()
                cleanDb.openHelper.writableDatabase
                cleanDb
            }
        }
    }
}
