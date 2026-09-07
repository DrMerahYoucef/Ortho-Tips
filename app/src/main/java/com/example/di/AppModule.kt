package com.example.di

import android.content.Context
import com.example.data.backup.BackupManager
import com.example.data.importer.BoneDatabaseImporter
import com.example.data.local.OrthoDatabase
import com.example.data.prefs.PdfOptionsRepository
import com.example.data.prefs.SettingsRepository
import com.example.data.prefs.SurgeonProfileRepository
import com.example.data.repository.ClassificationRepository
import com.example.data.repository.OrthoRepository
import com.example.data.repository.PatientRepository
import com.example.data.repository.SuggestionRepository
import com.example.utils.PatientMediaStorage
import java.io.File

class AppModule(private val context: Context) {

    val database: OrthoDatabase by lazy {
        OrthoDatabase.getInstance(context)
    }

    private val importedFiguresDir: File by lazy {
        File(context.filesDir, BoneDatabaseImporter.IMAGES_DIR_NAME)
    }

    val orthoRepository: OrthoRepository by lazy {
        OrthoRepository(database, importedFiguresDir)
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(context)
    }

    val backupManager: BackupManager by lazy {
        BackupManager(context, database)
    }

    val boneDatabaseImporter: BoneDatabaseImporter by lazy {
        BoneDatabaseImporter(context, database)
    }

    // Static, asset-backed classification reference (read-only, see assets/classifications/)
    val classificationRepository: ClassificationRepository by lazy {
        ClassificationRepository(context)
    }

    // Patient management module
    private val patientMediaRootDir: File by lazy {
        PatientMediaStorage.rootDir(context)
    }

    val patientRepository: PatientRepository by lazy {
        PatientRepository(database, patientMediaRootDir)
    }

    val surgeonProfileRepository: SurgeonProfileRepository by lazy {
        SurgeonProfileRepository(context)
    }

    val pdfOptionsRepository: PdfOptionsRepository by lazy {
        PdfOptionsRepository(context)
    }

    val suggestionRepository: SuggestionRepository by lazy {
        SuggestionRepository(database, settingsRepository)
    }
}