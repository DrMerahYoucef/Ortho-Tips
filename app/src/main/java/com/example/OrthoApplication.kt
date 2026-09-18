package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.data.local.ContentLocale
import com.example.data.storage.DriveMediaInterceptor
import com.example.data.storage.StorageBackend
import com.example.data.telegram.DriveLog
import com.example.di.AppModule
import com.example.work.SyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class OrthoApplication : Application(), ImageLoaderFactory {

    lateinit var appModule: AppModule
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        appModule = AppModule(this)
        // Resolved once per process: the language only changes with an activity
        // recreate, so every read below sees a stable value.
        ContentLocale.refresh(this)
        restoreTelegramDriveSession()
    }

    /**
     * Installs the app-wide Coil loader.
     *
     * The only addition is [DriveMediaInterceptor], which lets every existing
     * `AsyncImage` call render a patient image whose bytes currently live only in
     * Telegram. No screen needed changing for this; the interception happens
     * below them.
     */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components { add(DriveMediaInterceptor(appModule.patientMediaGateway)) }
        .crossfade(true)
        .build()

    /**
     * Brings the Telegram session back up after a cold start, but only when the
     * user actually finished setup and chose the drive as their backend. An
     * install that never enabled it does no work here at all — the whole Telegram
     * graph stays unconstructed.
     *
     * Runs off the main thread and swallows failures: a drive that cannot connect
     * must never prevent the app from opening, because local records have to stay
     * reachable whatever the network is doing.
     */
    private fun restoreTelegramDriveSession() {
        applicationScope.launch {
            runCatching {
                val settings = appModule.telegramDriveRepository.current()
                if (!settings.setupCompleted || settings.backend != StorageBackend.TELEGRAM) {
                    return@runCatching
                }
                val credentials = appModule.telegramDriveRepository.credentials()
                if (credentials == null) {
                    DriveLog.w(TAG, "Drive enabled but no stored credentials; staying offline")
                    return@runCatching
                }
                appModule.telegramClient.start(credentials)
                SyncScheduler(this@OrthoApplication).applyPreferences()
                DriveLog.d(TAG, "Telegram Drive session restored")
            }.onFailure {
                DriveLog.e(TAG, "Could not restore the Telegram Drive session", it)
            }
        }
    }

    companion object {
        private const val TAG = "OrthoApplication"
    }
}
