package com.example.data.storage

import android.content.Context
import com.example.data.local.entity.PatientDocumentEntity
import com.example.data.local.entity.PatientImageEntity
import com.example.data.prefs.TelegramDriveRepository
import com.example.data.sync.SyncEngine
import com.example.data.telegram.DriveLog
import com.example.work.SyncScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The seam between patient records and storage.
 *
 * [com.example.data.repository.PatientRepository] calls into this instead of
 * touching `File` directly, which is what turns the drive from a background
 * mirror into the actual backend: a saved radiograph is queued for upload, a
 * deleted one is tombstoned so the deletion propagates, and a record whose bytes
 * only exist in Telegram can still be opened.
 *
 * Every method is a no-op when Telegram Drive is off. That is deliberate — an
 * install that never enables the drive must behave exactly as it did before, so
 * the guard lives here rather than being repeated at each call site.
 */
class PatientMediaGateway(
    private val context: Context,
    private val registry: StorageProviderRegistry,
    private val syncEngine: SyncEngine,
    private val driveRepository: TelegramDriveRepository
) {

    private val filesRoot: String by lazy { context.filesDir.absolutePath }

    private val scheduler by lazy { SyncScheduler(context) }

    /**
     * Derives the logical key from an absolute media path.
     *
     * Paths stored in the database are absolute and were produced by
     * `PatientMediaStorage`, so relativising against `filesDir` yields exactly
     * the key layout [StorageKey] defines. Returns null for anything outside the
     * app-private tree, which is the correct answer for a path that predates the
     * media store or came from an external picker.
     */
    fun keyFor(absolutePath: String?): StorageKey? {
        if (absolutePath.isNullOrBlank()) return null
        val normalised = absolutePath.removePrefix(filesRoot).removePrefix(File.separator)
        if (normalised == absolutePath && !absolutePath.startsWith(StorageKey.PATIENTS_PREFIX)) {
            return null
        }
        val relative = normalised.replace(File.separatorChar, '/')
        if (!relative.startsWith(StorageKey.PATIENTS_PREFIX)) return null
        return runCatching { StorageKey(relative) }.getOrNull()
    }

    /**
     * Returns a readable local file for a stored media path, fetching it from the
     * drive if the local copy is gone. Null when the bytes cannot be produced —
     * callers must treat that as "not available right now", not as "deleted".
     */
    suspend fun resolve(absolutePath: String?): File? = withContext(Dispatchers.IO) {
        if (absolutePath.isNullOrBlank()) return@withContext null
        val direct = File(absolutePath)
        if (direct.exists() && direct.length() > 0) return@withContext direct
        val key = keyFor(absolutePath) ?: return@withContext null
        if (!isDriveActive()) return@withContext null
        registry.resolve(key)
    }

    /** True when the bytes behind [absolutePath] are on this device already. */
    fun isLocallyAvailable(absolutePath: String?): Boolean {
        if (absolutePath.isNullOrBlank()) return false
        val file = File(absolutePath)
        return file.exists() && file.length() > 0
    }

    // ---- Write hooks ---------------------------------------------------------

    suspend fun onImageSaved(image: PatientImageEntity) {
        if (!isDriveActive()) return
        // The full image and its thumbnail are queued separately: grids need
        // thumbnails on a second device long before anyone opens the original.
        enqueue(image.filePath, StorageObjectKind.PATIENT_IMAGE, "image/jpeg")
        enqueue(image.thumbnailPath, StorageObjectKind.PATIENT_THUMBNAIL, "image/jpeg")
        kick()
    }

    suspend fun onDocumentSaved(document: PatientDocumentEntity) {
        if (!isDriveActive()) return
        enqueue(
            document.filePath,
            StorageObjectKind.PATIENT_DOCUMENT,
            document.mimeType.ifBlank { "application/octet-stream" }
        )
        kick()
    }

    private suspend fun enqueue(path: String?, kind: StorageObjectKind, mimeType: String) {
        val key = keyFor(path) ?: return
        val file = File(path!!)
        if (!file.exists() || file.length() == 0L) return
        runCatching { syncEngine.enqueueUpload(key, file, kind, mimeType) }
            .onFailure { DriveLog.w(TAG, "Could not queue ${key.fileName}", it) }
    }

    // ---- Delete hooks --------------------------------------------------------

    /**
     * Soft-deletes media for a removed image. The local file is moved to the
     * recoverable trash rather than unlinked, and a tombstone is recorded so the
     * next sync removes it from the drive instead of the drive resurrecting it.
     */
    suspend fun onImageDeleted(image: PatientImageEntity) {
        remove(image.filePath, "patient image deleted")
        remove(image.thumbnailPath, "patient image deleted")
    }

    suspend fun onDocumentDeleted(document: PatientDocumentEntity) {
        remove(document.filePath, "patient document deleted")
    }

    suspend fun onPatientMediaDeleted(
        images: List<PatientImageEntity>,
        documents: List<PatientDocumentEntity>
    ) {
        images.forEach { onImageDeleted(it) }
        documents.forEach { onDocumentDeleted(it) }
    }

    private suspend fun remove(path: String?, reason: String) = withContext(Dispatchers.IO) {
        if (path.isNullOrBlank()) return@withContext
        val key = keyFor(path)
        if (key == null) {
            // Outside the managed tree, so nothing is tracking it; the old
            // behaviour of deleting outright is still the right one.
            File(path).delete()
            return@withContext
        }
        runCatching { registry.local.delete(key) }
            .onFailure { DriveLog.w(TAG, "Soft delete failed for ${key.fileName}", it) }
        if (driveRepository.current().setupCompleted) {
            runCatching { syncEngine.recordDeletion(key, reason) }
                .onFailure { DriveLog.w(TAG, "Could not record deletion", it) }
        }
    }

    /**
     * Undoes a soft delete. Present so a mis-tap in the patient screen is
     * recoverable, and so a failed migration can be rolled back.
     */
    suspend fun restore(absolutePath: String?): Boolean = withContext(Dispatchers.IO) {
        val key = keyFor(absolutePath) ?: return@withContext false
        val localRestored = registry.local.restore(key)
        if (localRestored) return@withContext true
        if (!isDriveActive()) return@withContext false
        registry.telegram.restore(key) && registry.telegram.materialize(key) != null
    }

    // ---- Prefetch ------------------------------------------------------------

    /**
     * Warms the cache for a set of media paths, skipping anything already local.
     * Used when a patient record is opened so scrolling their imaging does not
     * stall on a per-image round trip.
     */
    suspend fun prefetch(paths: List<String?>): Int = withContext(Dispatchers.IO) {
        if (!isDriveActive()) return@withContext 0
        var fetched = 0
        for (path in paths) {
            if (isLocallyAvailable(path)) continue
            val key = keyFor(path) ?: continue
            if (registry.telegram.peek(key) != null) continue
            if (registry.telegram.materialize(key) != null) fetched++
        }
        fetched
    }

    private suspend fun isDriveActive(): Boolean {
        val settings = driveRepository.current()
        return settings.setupCompleted && settings.backend == StorageBackend.TELEGRAM
    }

    private suspend fun kick() {
        val settings = driveRepository.current()
        runCatching {
            scheduler.kickTransfers(settings.syncOnWifiOnly, settings.syncWhileChargingOnly)
        }.onFailure { DriveLog.w(TAG, "Could not schedule the transfer worker", it) }
    }

    companion object {
        private const val TAG = "PatientMediaGateway"
    }
}
