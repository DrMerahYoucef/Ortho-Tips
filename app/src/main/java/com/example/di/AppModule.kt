package com.example.di

import android.content.Context
import com.example.data.backup.BackupManager
import com.example.data.importer.JsonDatabaseImporter
import com.example.data.local.OrthoDatabase
import com.example.data.prefs.SettingsRepository
import com.example.data.repository.OrthoRepository

class AppModule(private val context: Context) {

    val database: OrthoDatabase by lazy {
        OrthoDatabase.getInstance(context)
    }

    val orthoRepository: OrthoRepository by lazy {
        OrthoRepository(database)
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(context)
    }

    val backupManager: BackupManager by lazy {
        BackupManager(context, database)
    }

    val jsonDatabaseImporter: JsonDatabaseImporter by lazy {
        JsonDatabaseImporter(database)
    }
}
