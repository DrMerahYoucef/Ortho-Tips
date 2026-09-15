package com.example.data.sync

import com.example.data.crypto.Hashing
import com.example.data.local.dao.RemoteObjectDao
import com.example.data.local.dao.SyncTombstoneDao
import com.example.data.local.dao.TransferQueueDao
import com.example.data.local.entity.SyncTombstoneEntity
import com.example.data.local.entity.TransferDirection
import com.example.data.local.entity.TransferQueueEntity
import com.example.data.local.entity.TransferState
import com.example.data.prefs.TelegramDriveRepository
import com.example.data.storage.LocalStorageProvider
import com.example.data.storage.StorageBackend
import com.example.data.storage.StorageKey
import com.example.data.storage.StorageObjectKind
import com.example.data.storage.StorageProviderRegistry
import com.example.data.storage.TelegramStorageProvider
import com.example.data.storage.TransferProgress
import com.example.data.telegram.DriveLog
import com.example.data.telegram.TelegramClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

/** What a sync run should cover. */
enum class SyncScope {
    /** Only what changed since the last successful run. The normal path. */
    INCREMENTAL,

    /** Re-examine everything, including objects the index believes are current. */
    FULL
}

/** How a divergence between local and remote is settled. */
enum class ConflictPolicy {
    /** Whichever side was modified last wins. The default. */
    NEWEST_WINS,

    /** The device is authoritative; the drive is overwritten. */
    PREFER_LOCAL,

    /** The drive is authoritative; local content is refreshed from it. */
    PREFER_REMOTE,

    /** Nothing is overwritten; divergences are reported for the user to settle. */
    ASK
}

data class SyncConflict(
    val key: StorageKey,
    val localModifiedAt: Long,
    val remoteModifiedAt: Long,
    val localHash: String?,
    val remoteHash: String?
)

data class SyncReport(
    val scope: SyncScope,
    val uploadsQueued: Int = 0,
    val downloadsQueued: Int = 0,
    val manifestRowsSeen: Int = 0,
    val tombstonesPropagated: Int = 0,
    val snapshotPushed: Boolean = false,
    val snapshotPulled: Boolean = false,
    val conflicts: List<SyncConflict> = emptyList(),
    val errors: List<String> = emptyList(),
    val startedAt: Long = System.currentTimeMillis(),
    val finishedAt: Long = System.currentTimeMillis()
) {
    val succeeded: Boolean get() = errors.isEmpty()
    val durationMillis: Long get() = finishedAt - startedAt
}

/**
 * Coordinates the drive: what to push, what to pull, and what to do when the two
 * sides disagree.
 *
 * The engine never transfers bytes itself. It plans work into `transfer_queue`
 * and lets [com.example.work.TransferQueueWorker] execute it under WorkManager
 * constraints, so a sync survives the app being backgrounded mid-upload and
 * resumes with the same backoff state.
 *
 * Deletion is propagated through tombstones rather than by absence. "Not in the
 * local set" is indistinguishable from "not downloaded yet", and treating those
 * the same is how sync engines destroy data.
 */
class SyncEngine(
    private val registry: StorageProviderRegistry,
    private val telegram: TelegramStorageProvider,
    private val local: LocalStorageProvider,
    private val client: TelegramClient,
    private val remoteObjectDao: RemoteObjectDao,
    private val transferQueueDao: TransferQueueDao,
    private val tombstoneDao: SyncTombstoneDao,
    private val manifestCodec: ManifestCodec,
    private val databaseSnapshot: DatabaseSnapshot,
    private val driveRepository: TelegramDriveRepository,
    /** Root the local provider resolves keys against; download destinations live here. */
    private val localRootDir: File,
    private val snapshotWorkDir: File
) {

    suspend fun sync(
        scope: SyncScope = SyncScope.INCREMENTAL,
        policy: ConflictPolicy = ConflictPolicy.NEWEST_WINS
    ): SyncReport = withContext(Dispatchers.IO) {
        val startedAt = System.currentTimeMillis()
        val errors = mutableListOf<String>()

        if (driveRepository.backend() != StorageBackend.TELEGRAM) {
            return@withContext SyncReport(
                scope = scope,
                errors = listOf("Telegram Drive is not the selected backend"),
                startedAt = startedAt
            )
        }
        if (!telegram.isReady()) {
            return@withContext SyncReport(
                scope = scope,
                errors = listOf("Telegram Drive is not connected"),
                startedAt = startedAt
            )
        }

        // Anything left RUNNING belongs to a process that no longer exists.
        transferQueueDao.requeueInterrupted(System.currentTimeMillis())

        val pulled = runCatching { pullManifest(scope) }
            .onFailure { errors += "Manifest pull failed: ${it.message}" }
            .getOrDefault(0)

        val tombstones = runCatching { propagateTombstones() }
            .onFailure { errors += "Deletion propagation failed: ${it.message}" }
            .getOrDefault(0)

        val plan = runCatching { planTransfers(scope, policy) }
            .onFailure { errors += "Planning failed: ${it.message}" }
            .getOrDefault(TransferPlan())

        val snapshotPushed = runCatching { pushDatabaseSnapshot() }
            .onFailure { errors += "Record snapshot failed: ${it.message}" }
            .getOrDefault(false)

        val finishedAt = System.currentTimeMillis()
        val report = SyncReport(
            scope = scope,
            uploadsQueued = plan.uploads,
            downloadsQueued = plan.downloads,
            manifestRowsSeen = pulled,
            tombstonesPropagated = tombstones,
            snapshotPushed = snapshotPushed,
            conflicts = plan.conflicts,
            errors = errors,
            startedAt = startedAt,
            finishedAt = finishedAt
        )
        if (report.succeeded) {
            driveRepository.recordSyncSuccess(finishedAt)
        } else {
            driveRepository.recordSyncFailure(errors.first())
        }
        DriveLog.i(
            TAG,
            "Sync ${scope.name.lowercase()} done in ${report.durationMillis}ms: " +
                    "${plan.uploads} up, ${plan.downloads} down, ${errors.size} errors"
        )
        report
    }

    // ---- Pull ----------------------------------------------------------------

    /**
     * Reads the drive channel and refreshes the local index from the captions.
     * On [SyncScope.FULL] the whole history is walked; incrementally it stops at
     * the first page that produced no new or changed rows.
     */
    private suspend fun pullManifest(scope: SyncScope): Int {
        val chatId = driveRepository.chatId() ?: return 0
        var fromMessageId = 0L
        var seen = 0
        var pages = 0

        while (pages < MAX_PAGES) {
            val page = client.fetchDocuments(chatId, fromMessageId, PAGE_SIZE)
            if (page.documents.isEmpty()) break
            var changedInPage = 0

            for (document in page.documents) {
                val manifest = manifestCodec.decode(document.caption)
                if (manifest == null) {
                    if (manifestCodec.isLocked(document.caption)) {
                        DriveLog.w(TAG, "Encrypted manifest unreadable on this device")
                    }
                    continue
                }
                seen++
                val existing = remoteObjectDao.get(manifest.storageKey)
                val unchanged = existing != null &&
                        existing.messageId == document.messageId &&
                        existing.revision == manifest.revision &&
                        existing.contentHash == manifest.contentHash &&
                        (existing.deletedAt != null) == (manifest.deletedAt != null)
                if (unchanged) continue

                changedInPage++
                remoteObjectDao.upsert(
                    com.example.data.local.entity.RemoteObjectEntity(
                        storageKey = manifest.storageKey,
                        kind = manifest.kind,
                        chatId = document.chatId,
                        messageId = document.messageId,
                        remoteFileId = document.remoteFileId,
                        uniqueFileId = document.uniqueFileId,
                        sizeBytes = manifest.sizeBytes,
                        contentHash = manifest.contentHash,
                        mimeType = manifest.mimeType,
                        encrypted = manifest.encrypted,
                        modifiedAt = manifest.modifiedAt,
                        syncedAt = System.currentTimeMillis(),
                        deletedAt = manifest.deletedAt,
                        revision = manifest.revision
                    )
                )
            }

            pages++
            fromMessageId = page.nextFromMessageId
            if (fromMessageId == 0L) break
            if (scope == SyncScope.INCREMENTAL && changedInPage == 0 && pages > 1) break
        }
        DriveLog.d(TAG, "Manifest pull: $seen entries over $pages pages")
        return seen
    }

    /**
     * Discards the local index and replays the entire channel. This is the
     * recovery path after a reinstall or a corrupted index — the reason the
     * caption manifest exists at all.
     */
    suspend fun rebuildManifest(): Int = withContext(Dispatchers.IO) {
        DriveLog.i(TAG, "Rebuilding manifest index from the drive channel")
        remoteObjectDao.clear()
        pullManifest(SyncScope.FULL)
    }

    // ---- Planning ------------------------------------------------------------

    private data class TransferPlan(
        val uploads: Int = 0,
        val downloads: Int = 0,
        val conflicts: List<SyncConflict> = emptyList()
    )

    private suspend fun planTransfers(scope: SyncScope, policy: ConflictPolicy): TransferPlan {
        val localObjects = local.list(StorageKey.PATIENTS_PREFIX).associateBy { it.key.value }
        val remoteObjects = remoteObjectDao.getAllLive().associateBy { it.storageKey }
        val conflicts = mutableListOf<SyncConflict>()
        var uploads = 0
        var downloads = 0

        // Local content the drive does not have, or has an older version of.
        for ((keyValue, localObject) in localObjects) {
            if (tombstoneDao.get(keyValue) != null) continue
            val remote = remoteObjects[keyValue]
            val localFile = local.peek(localObject.key) ?: continue

            if (remote == null) {
                if (enqueue(localObject.key, localFile, TransferDirection.UPLOAD, localObject.kind, localObject.mimeType)) uploads++
                continue
            }

            val localHash = if (scope == SyncScope.FULL || remote.sizeBytes != localFile.length()) {
                Hashing.sha256(localFile)
            } else {
                null // same size and incremental: trust the index rather than re-hash
            }
            val identical = localHash != null && localHash == remote.contentHash
            if (identical) continue
            if (localHash == null && remote.modifiedAt >= localFile.lastModified()) continue

            val localNewer = localFile.lastModified() > remote.modifiedAt
            val bothChanged = localHash != null && localHash != remote.contentHash &&
                    remote.modifiedAt > localFile.lastModified()

            when {
                bothChanged && policy == ConflictPolicy.ASK -> conflicts += SyncConflict(
                    key = localObject.key,
                    localModifiedAt = localFile.lastModified(),
                    remoteModifiedAt = remote.modifiedAt,
                    localHash = localHash,
                    remoteHash = remote.contentHash
                )

                policy == ConflictPolicy.PREFER_REMOTE ->
                    if (enqueue(localObject.key, localFile, TransferDirection.DOWNLOAD, localObject.kind, localObject.mimeType)) downloads++

                policy == ConflictPolicy.PREFER_LOCAL || localNewer ->
                    if (enqueue(localObject.key, localFile, TransferDirection.UPLOAD, localObject.kind, localObject.mimeType)) uploads++

                else ->
                    if (enqueue(localObject.key, localFile, TransferDirection.DOWNLOAD, localObject.kind, localObject.mimeType)) downloads++
            }
        }

        // Drive content this device has never seen. Thumbnails are fetched eagerly
        // because grids need them; full-size media waits until it is opened.
        for ((keyValue, remote) in remoteObjects) {
            if (localObjects.containsKey(keyValue)) continue
            if (keyValue.startsWith(StorageKey.SNAPSHOT_PREFIX)) continue
            val kind = runCatching { StorageObjectKind.valueOf(remote.kind) }
                .getOrDefault(StorageObjectKind.OTHER)
            if (!kind.isThumbnail && scope != SyncScope.FULL) continue
            val key = runCatching { StorageKey(keyValue) }.getOrNull() ?: continue
            val destination = File(localRootDir, keyValue)
            if (enqueue(key, destination, TransferDirection.DOWNLOAD, kind, remote.mimeType)) downloads++
        }

        return TransferPlan(uploads, downloads, conflicts)
    }

    /**
     * Queues a single upload. Called by
     * [com.example.data.storage.PatientMediaGateway] when a record is saved, so
     * new media starts transferring immediately rather than waiting for the next
     * periodic sync to notice it.
     */
    suspend fun enqueueUpload(
        key: StorageKey,
        file: File,
        kind: StorageObjectKind,
        mimeType: String
    ): Boolean = enqueue(key, file, TransferDirection.UPLOAD, kind, mimeType)

    private suspend fun enqueue(
        key: StorageKey,
        file: File,
        direction: TransferDirection,
        kind: StorageObjectKind,
        mimeType: String
    ): Boolean {
        if (transferQueueDao.findActiveFor(key.value) != null) return false
        transferQueueDao.insert(
            TransferQueueEntity(
                storageKey = key.value,
                direction = direction.name,
                localPath = file.absolutePath,
                kind = kind.name,
                mimeType = mimeType,
                state = TransferState.PENDING.name,
                totalBytes = if (direction == TransferDirection.UPLOAD) file.length() else 0
            )
        )
        return true
    }

    // ---- Deletions -----------------------------------------------------------

    private suspend fun propagateTombstones(): Int {
        val pending = tombstoneDao.getPending()
        var propagated = 0
        for (tombstone in pending) {
            val key = runCatching { StorageKey(tombstone.storageKey) }.getOrNull() ?: continue
            runCatching { telegram.delete(key) }
                .onSuccess {
                    tombstoneDao.markPropagated(tombstone.storageKey)
                    propagated++
                }
                .onFailure { DriveLog.w(TAG, "Could not propagate deletion", it) }
        }
        return propagated
    }

    /** Records a local deletion so the next sync removes it from the drive too. */
    suspend fun recordDeletion(key: StorageKey, reason: String) {
        tombstoneDao.upsert(
            SyncTombstoneEntity(
                storageKey = key.value,
                deletedAt = System.currentTimeMillis(),
                reason = reason
            )
        )
    }

    // ---- Records -------------------------------------------------------------

    private suspend fun pushDatabaseSnapshot(): Boolean {
        val timestamp = System.currentTimeMillis()
        val file = File(snapshotWorkDir, "ortho-db-$timestamp.json")
        databaseSnapshot.export(file)
        val key = StorageKey.snapshot(timestamp)
        return try {
            val terminal = telegram.put(
                key,
                file,
                StorageObjectKind.DATABASE_SNAPSHOT,
                "application/json"
            ).first { it is TransferProgress.Completed || it is TransferProgress.Failed }
            terminal is TransferProgress.Completed
        } finally {
            file.delete()
        }
    }

    /**
     * Fetches the newest snapshot from the drive and merges it. Used by the setup
     * wizard on a fresh install and by the explicit restore action.
     */
    suspend fun pullDatabaseSnapshot(): SnapshotSummary? = withContext(Dispatchers.IO) {
        val newest = remoteObjectDao.getByPrefix(StorageKey.SNAPSHOT_PREFIX)
            .maxByOrNull { it.modifiedAt } ?: return@withContext null
        val key = runCatching { StorageKey(newest.storageKey) }.getOrNull() ?: return@withContext null
        val file = telegram.materialize(key) ?: return@withContext null
        databaseSnapshot.import(file)
    }

    // ---- Integrity -----------------------------------------------------------

    /**
     * Walks the index and checks every entry still resolves. Returns the keys that
     * failed so the diagnostics screen can list them; nothing is deleted here.
     */
    suspend fun verifyIntegrity(): IntegrityReport = withContext(Dispatchers.IO) {
        val rows = remoteObjectDao.getAllLive()
        val missing = mutableListOf<String>()
        val corrupt = mutableListOf<String>()
        for (row in rows) {
            val key = runCatching { StorageKey(row.storageKey) }.getOrNull() ?: continue
            val document = runCatching { client.getDocument(row.chatId, row.messageId) }.getOrNull()
            if (document == null) {
                missing += row.storageKey
                continue
            }
            if (!telegram.verify(key)) corrupt += row.storageKey
        }
        val orphans = transferQueueDao.getFailed().count {
            remoteObjectDao.get(it.storageKey) == null && !File(it.localPath).exists()
        }
        IntegrityReport(
            checked = rows.size,
            missingRemotely = missing,
            hashMismatches = corrupt,
            orphanedTransfers = orphans
        )
    }

    /** Removes cached bytes and finished queue rows that no longer serve anything. */
    suspend fun cleanUp(retentionMillis: Long): CleanupReport = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - retentionMillis
        val trimmedHistory = transferQueueDao.trimHistory(cutoff)
        val trimmedTombstones = tombstoneDao.trim(cutoff)
        val budget = driveRepository.current().cacheBudgetMb.toLong() * 1024 * 1024
        val freed = telegram.trimCacheTo(budget)
        val purgedLocal = local.purge(retentionMillis)
        CleanupReport(trimmedHistory, trimmedTombstones, freed, purgedLocal)
    }

    companion object {
        private const val TAG = "SyncEngine"
        private const val PAGE_SIZE = 100
        private const val MAX_PAGES = 200
    }
}

data class IntegrityReport(
    val checked: Int,
    val missingRemotely: List<String>,
    val hashMismatches: List<String>,
    val orphanedTransfers: Int
) {
    val healthy: Boolean
        get() = missingRemotely.isEmpty() && hashMismatches.isEmpty() && orphanedTransfers == 0
}

data class CleanupReport(
    val transferRowsTrimmed: Int,
    val tombstonesTrimmed: Int,
    val cacheBytesFreed: Long,
    val localObjectsPurged: Int
)
