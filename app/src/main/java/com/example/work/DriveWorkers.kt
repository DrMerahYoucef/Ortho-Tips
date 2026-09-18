package com.example.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.OrthoApplication
import com.example.data.local.entity.TransferDirection
import com.example.data.local.entity.TransferQueueEntity
import com.example.data.local.entity.TransferState
import com.example.data.prefs.SyncFrequency
import com.example.data.storage.StorageKey
import com.example.data.storage.StorageObjectKind
import com.example.data.storage.TransferProgress
import com.example.data.sync.SyncScope
import com.example.data.telegram.DriveLog
import com.example.di.AppModule
import kotlinx.coroutines.flow.first
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.math.pow

private fun Context.appModule(): AppModule =
    (applicationContext as OrthoApplication).appModule

/**
 * Drains `transfer_queue`, one row at a time, until the queue is empty or the
 * batch limit is reached.
 *
 * Work is claimed from the database rather than passed in as input data, which is
 * what makes an interrupted transfer resumable: WorkManager can kill this worker
 * mid-upload and the row is still PENDING with its attempt count intact.
 */
class TransferQueueWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val module = applicationContext.appModule()
        val queueDao = module.database.transferQueueDao()
        val telegram = module.telegramStorageProvider
        val local = module.localStorageProvider

        if (!telegram.isReady()) {
            DriveLog.d(TAG, "Drive not ready; retrying later")
            return Result.retry()
        }

        queueDao.requeueInterrupted(System.currentTimeMillis())
        var processed = 0
        var failures = 0

        while (processed < BATCH_LIMIT) {
            val batch = queueDao.claimBatch(System.currentTimeMillis(), 1)
            val row = batch.firstOrNull() ?: break
            queueDao.update(
                row.copy(state = TransferState.RUNNING.name, updatedAt = System.currentTimeMillis())
            )

            val key = runCatching { StorageKey(row.storageKey) }.getOrNull()
            if (key == null) {
                queueDao.update(row.fail("Malformed storage key"))
                processed++
                failures++
                continue
            }

            val kind = runCatching { StorageObjectKind.valueOf(row.kind) }
                .getOrDefault(StorageObjectKind.OTHER)
            val file = File(row.localPath)
            val direction = runCatching { TransferDirection.valueOf(row.direction) }
                .getOrDefault(TransferDirection.UPLOAD)

            val flow = when (direction) {
                TransferDirection.UPLOAD -> {
                    if (!file.exists()) {
                        // The source is gone: the record was deleted after queueing.
                        queueDao.delete(row.id)
                        processed++
                        continue
                    }
                    telegram.put(key, file, kind, row.mimeType)
                }

                TransferDirection.DOWNLOAD -> telegram.get(key, file)
            }

            var terminal: TransferProgress? = null
            flow.collect { progress ->
                when (progress) {
                    is TransferProgress.Running -> setProgress(
                        workDataOf(
                            PROGRESS_KEY to row.storageKey,
                            PROGRESS_FRACTION to progress.fraction
                        )
                    )

                    is TransferProgress.Completed, is TransferProgress.Failed -> terminal = progress
                    else -> Unit
                }
            }

            when (val outcome = terminal) {
                is TransferProgress.Completed -> {
                    queueDao.update(
                        row.copy(
                            state = TransferState.SUCCEEDED.name,
                            bytesTransferred = outcome.obj.sizeBytes,
                            totalBytes = outcome.obj.sizeBytes,
                            lastError = null,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    // A download lands in the local tree, so the local provider
                    // now serves it and the next sync sees the two sides agree.
                    if (direction == TransferDirection.DOWNLOAD) {
                        DriveLog.d(TAG, "Fetched ${key.fileName} into local storage")
                    }
                }

                is TransferProgress.Failed -> {
                    failures++
                    if (outcome.retryable && row.attemptCount + 1 < MAX_ATTEMPTS) {
                        queueDao.update(row.backOff(outcome.reason))
                    } else {
                        queueDao.update(row.fail(outcome.reason))
                    }
                }

                else -> queueDao.update(row.backOff("Transfer produced no result"))
            }
            processed++
        }

        if (processed == 0) return Result.success()
        DriveLog.i(TAG, "Transfer batch: $processed handled, $failures failed")

        // More work waiting means another pass; WorkManager re-enqueues under the
        // same constraints instead of this worker running unbounded.
        val remaining = queueDao.claimBatch(System.currentTimeMillis(), 1).isNotEmpty()
        return if (remaining) Result.retry() else Result.success()
    }

    /** Exponential backoff capped so a flood-wait never parks work for hours. */
    private fun TransferQueueEntity.backOff(reason: String): TransferQueueEntity {
        val attempt = attemptCount + 1
        val delayMillis = min(
            BASE_BACKOFF_MILLIS * 2.0.pow(attempt.toDouble()).toLong(),
            MAX_BACKOFF_MILLIS
        )
        return copy(
            state = TransferState.PENDING.name,
            attemptCount = attempt,
            nextAttemptAt = System.currentTimeMillis() + delayMillis,
            lastError = reason,
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun TransferQueueEntity.fail(reason: String): TransferQueueEntity = copy(
        state = TransferState.FAILED.name,
        attemptCount = attemptCount + 1,
        lastError = reason,
        updatedAt = System.currentTimeMillis()
    )

    companion object {
        private const val TAG = "TransferQueueWorker"
        const val UNIQUE_NAME = "ortho_drive_transfers"
        const val PROGRESS_KEY = "storage_key"
        const val PROGRESS_FRACTION = "fraction"
        private const val BATCH_LIMIT = 40
        private const val MAX_ATTEMPTS = 6
        private const val BASE_BACKOFF_MILLIS = 15_000L
        private const val MAX_BACKOFF_MILLIS = 30 * 60 * 1000L
    }
}

/**
 * Plans a sync run, then hands execution to [TransferQueueWorker]. Kept separate
 * so a periodic sync that finds nothing to do costs one cheap pass rather than
 * holding a worker open for the duration of every transfer.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val module = applicationContext.appModule()
        val settings = module.telegramDriveRepository.current()
        if (!settings.setupCompleted) return Result.success()

        val scope = if (inputData.getBoolean(INPUT_FULL, false)) {
            SyncScope.FULL
        } else {
            SyncScope.INCREMENTAL
        }

        val report = module.syncEngine.sync(scope)
        if (!report.succeeded) {
            DriveLog.w(TAG, "Sync reported ${report.errors.size} errors")
            return Result.retry()
        }

        module.syncEngine.cleanUp(CLEANUP_RETENTION_MILLIS)

        if (report.uploadsQueued > 0 || report.downloadsQueued > 0) {
            SyncScheduler(applicationContext).kickTransfers()
        }
        return Result.success()
    }

    companion object {
        private const val TAG = "SyncWorker"
        const val UNIQUE_PERIODIC = "ortho_drive_periodic_sync"
        const val UNIQUE_ONE_SHOT = "ortho_drive_sync_now"
        const val INPUT_FULL = "full_sync"
        private const val CLEANUP_RETENTION_MILLIS = 30L * 24 * 60 * 60 * 1000
    }
}

/**
 * Single place that translates the user's sync preferences into WorkManager
 * constraints, so "Wi-Fi only" and "while charging only" mean the same thing on
 * every path that schedules work.
 */
class SyncScheduler(private val context: Context) {

    private val workManager get() = WorkManager.getInstance(context)

    suspend fun applyPreferences() {
        val settings = context.appModule().telegramDriveRepository.settings.first()
        val period = settings.syncFrequency.periodHours
        if (!settings.setupCompleted || !settings.backgroundSyncEnabled || period == null) {
            // MANUAL and ON_CHANGE need no periodic worker: the first waits for the
            // surgeon, the second is driven by saves as they happen.
            cancelPeriodic()
            return
        }
        schedulePeriodic(
            periodHours = period,
            wifiOnly = settings.syncOnWifiOnly,
            chargingOnly = settings.syncWhileChargingOnly
        )
    }

    fun schedulePeriodic(periodHours: Long, wifiOnly: Boolean, chargingOnly: Boolean) {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(
            periodHours.coerceAtLeast(MIN_INTERVAL_HOURS), TimeUnit.HOURS
        )
            .setConstraints(constraints(wifiOnly, chargingOnly))
            .build()
        workManager.enqueueUniquePeriodicWork(
            SyncWorker.UNIQUE_PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelPeriodic() {
        workManager.cancelUniqueWork(SyncWorker.UNIQUE_PERIODIC)
    }

    /** Runs a sync straight away, ignoring the Wi-Fi and charging preferences. */
    fun syncNow(full: Boolean = false) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .setInputData(workDataOf(SyncWorker.INPUT_FULL to full))
            .build()
        workManager.enqueueUniqueWork(
            SyncWorker.UNIQUE_ONE_SHOT,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    /**
     * Pushes a record snapshot shortly after the database changes.
     *
     * Delayed and REPLACE-policy on purpose: typing a patient's details fires a
     * save per field, and without the debounce that would upload a snapshot per
     * keystroke. Each new call pushes the timer out, so a burst of edits settles
     * into one upload.
     */
    fun scheduleRecordSnapshot() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .setInitialDelay(RECORD_DEBOUNCE_SECONDS, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(
            UNIQUE_RECORD_SNAPSHOT,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    /** Ensures the queue worker is scheduled; no-op when one is already queued. */
    fun kickTransfers(wifiOnly: Boolean = false, chargingOnly: Boolean = false) {
        val request = OneTimeWorkRequestBuilder<TransferQueueWorker>()
            .setConstraints(constraints(wifiOnly, chargingOnly))
            .build()
        workManager.enqueueUniqueWork(
            TransferQueueWorker.UNIQUE_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    /** Restarts the queue worker after constraint-relevant settings change. */
    fun restartTransfers(wifiOnly: Boolean, chargingOnly: Boolean) {
        val request = OneTimeWorkRequestBuilder<TransferQueueWorker>()
            .setConstraints(constraints(wifiOnly, chargingOnly))
            .build()
        workManager.enqueueUniqueWork(
            TransferQueueWorker.UNIQUE_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelAll() {
        workManager.cancelUniqueWork(SyncWorker.UNIQUE_PERIODIC)
        workManager.cancelUniqueWork(SyncWorker.UNIQUE_ONE_SHOT)
        workManager.cancelUniqueWork(TransferQueueWorker.UNIQUE_NAME)
        workManager.cancelUniqueWork(UNIQUE_RECORD_SNAPSHOT)
    }

    private fun constraints(wifiOnly: Boolean, chargingOnly: Boolean): Constraints =
        Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .setRequiresCharging(chargingOnly)
            // Patient media can be large; refusing to start on a nearly full disk
            // is cheaper than failing halfway through a 200 MB CT series.
            .setRequiresStorageNotLow(true)
            .build()

    companion object {
        /** WorkManager's floor for a periodic request. */
        private const val MIN_INTERVAL_HOURS = 1L
        private const val UNIQUE_RECORD_SNAPSHOT = "ortho_drive_record_snapshot"
        private const val RECORD_DEBOUNCE_SECONDS = 20L
    }
}
