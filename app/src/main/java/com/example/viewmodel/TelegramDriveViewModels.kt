package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.crypto.CryptoKeys
import com.example.data.local.entity.TransferDirection
import com.example.data.local.entity.TransferQueueEntity
import com.example.data.prefs.CachePolicy
import com.example.data.prefs.SyncMode
import com.example.data.prefs.TelegramDriveRepository
import com.example.data.prefs.TelegramDriveSettings
import com.example.data.storage.StorageBackend
import com.example.data.storage.StorageUsage
import com.example.data.sync.ConflictPolicy
import com.example.data.sync.IntegrityReport
import com.example.data.sync.SyncReport
import com.example.data.sync.SyncScope
import com.example.data.telegram.DriveLog
import com.example.data.telegram.TelegramAuthState
import com.example.data.telegram.TelegramConnectionState
import com.example.data.telegram.TelegramCredentials
import com.example.di.AppModule
import com.example.work.SyncScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// Setup wizard
// ---------------------------------------------------------------------------

/**
 * Ordered steps of first-time setup. The wizard is linear because every step
 * depends on the one before it: there is no drive to verify before the account
 * is linked, and no manifest to build before the drive exists.
 */
enum class SetupStep {
    WELCOME,
    REQUIREMENTS,
    CREDENTIALS,
    TELEGRAM_LOGIN,
    NOTIFICATIONS,
    DRIVE_INITIALISATION,
    STORAGE_VERIFICATION,
    CONNECTION_TEST,
    MANIFEST,
    INITIAL_SYNC,
    COMPLETE;

    val isLast: Boolean get() = this == COMPLETE

    val index: Int get() = ordinal

    companion object {
        val total: Int get() = entries.size
    }
}

data class SetupUiState(
    val step: SetupStep = SetupStep.WELCOME,
    val apiId: String = "",
    val apiHash: String = "",
    val phoneNumber: String = "",
    val code: String = "",
    val password: String = "",
    val email: String = "",
    val authState: TelegramAuthState = TelegramAuthState.Uninitialized,
    val connection: TelegramConnectionState = TelegramConnectionState.CONNECTING,
    val busy: Boolean = false,
    val statusLine: String = "",
    val error: String? = null,
    val accountName: String = "",
    val chatId: Long? = null,
    val verifiedBytes: Long = 0,
    val manifestEntries: Int = 0,
    val initialSyncReport: SyncReport? = null,
    val existingDriveFound: Boolean = false
) {
    val canAdvance: Boolean
        get() = !busy && when (step) {
            SetupStep.CREDENTIALS -> apiId.toIntOrNull() != null && apiHash.length >= 16
            SetupStep.TELEGRAM_LOGIN -> authState == TelegramAuthState.Ready
            SetupStep.DRIVE_INITIALISATION -> chatId != null
            else -> true
        }
}

/**
 * Drives first-time setup. Each step's work is idempotent, so backing out and
 * retrying never leaves half-created state on the account — important because a
 * failed run must not litter the user's Telegram with orphan channels.
 */
class TelegramSetupViewModel(
    private val appModule: AppModule,
    private val applicationContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    private val client get() = appModule.telegramClient
    private val drive get() = appModule.telegramDriveRepository

    init {
        viewModelScope.launch {
            client.authState.collect { state ->
                _uiState.value = _uiState.value.copy(
                    authState = state,
                    error = (state as? TelegramAuthState.Failed)?.message
                )
            }
        }
        viewModelScope.launch {
            client.connectionState.collect { _uiState.value = _uiState.value.copy(connection = it) }
        }
        viewModelScope.launch {
            drive.credentials()?.let { existing ->
                _uiState.value = _uiState.value.copy(
                    apiId = existing.apiId.toString(),
                    apiHash = existing.apiHash
                )
            }
        }
    }

    fun onApiId(value: String) {
        _uiState.value = _uiState.value.copy(apiId = value.filter { it.isDigit() }, error = null)
    }

    fun onApiHash(value: String) {
        _uiState.value = _uiState.value.copy(apiHash = value.trim(), error = null)
    }

    fun onPhoneNumber(value: String) {
        _uiState.value = _uiState.value.copy(phoneNumber = value, error = null)
    }

    fun onCode(value: String) {
        _uiState.value = _uiState.value.copy(code = value.filter { it.isDigit() }, error = null)
    }

    fun onPassword(value: String) {
        _uiState.value = _uiState.value.copy(password = value, error = null)
    }

    fun onEmail(value: String) {
        _uiState.value = _uiState.value.copy(email = value.trim(), error = null)
    }

    fun back() {
        val current = _uiState.value.step
        if (current == SetupStep.WELCOME) return
        _uiState.value = _uiState.value.copy(
            step = SetupStep.entries[current.ordinal - 1],
            error = null
        )
    }

    /** Runs the work belonging to the current step, then advances on success. */
    fun advance() {
        val state = _uiState.value
        if (!state.canAdvance) return
        viewModelScope.launch {
            setBusy(true)
            val ok = runCatching {
                when (state.step) {
                    SetupStep.CREDENTIALS -> startTelegram()
                    SetupStep.DRIVE_INITIALISATION -> initialiseDrive()
                    SetupStep.STORAGE_VERIFICATION -> verifyStorage()
                    SetupStep.CONNECTION_TEST -> testConnection()
                    SetupStep.MANIFEST -> buildManifest()
                    SetupStep.INITIAL_SYNC -> runInitialSync()
                    SetupStep.COMPLETE -> finish()
                    else -> true
                }
            }.getOrElse { error ->
                DriveLog.e(TAG, "Setup step ${state.step} failed", error)
                _uiState.value = _uiState.value.copy(error = error.message ?: "Step failed")
                false
            }
            setBusy(false)
            if (ok && !_uiState.value.step.isLast) {
                _uiState.value = _uiState.value.copy(
                    step = SetupStep.entries[_uiState.value.step.ordinal + 1],
                    error = null
                )
            }
        }
    }

    private suspend fun startTelegram(): Boolean {
        val state = _uiState.value
        val credentials = TelegramCredentials(
            apiId = state.apiId.toIntOrNull() ?: return false,
            apiHash = state.apiHash
        )
        drive.storeCredentials(credentials)
        status("Starting Telegram…")
        client.start(credentials)
        return true
    }

    fun submitPhoneNumber() = runStep("Sending code…") {
        client.submitPhoneNumber(_uiState.value.phoneNumber.trim())
    }

    fun submitCode() = runStep("Checking code…") { client.submitCode(_uiState.value.code) }

    fun submitPassword() = runStep("Checking password…") {
        client.submitPassword(_uiState.value.password)
    }

    fun submitEmail() = runStep("Sending email code…") {
        client.submitEmailAddress(_uiState.value.email)
    }

    fun submitEmailCode() = runStep("Checking email code…") {
        client.submitEmailCode(_uiState.value.code)
    }

    fun resendCode() = runStep("Resending…") { client.resendCode() }

    fun useQrLogin() = runStep("Preparing QR code…") { client.requestQrCodeAuthentication() }

    fun usePhoneLogin() = runStep("Restarting sign-in…") { client.restartAuthentication() }

    private suspend fun initialiseDrive(): Boolean {
        status("Looking for an existing Ortho Tips Drive…")
        val existing = runCatching { client.listStorageChannels() }.getOrDefault(emptyList())
        val chatId = client.ensureStorageChat(drive.chatId())
        drive.setChatId(chatId)
        val user = runCatching { client.getCurrentUser() }.getOrNull()
        if (user != null) {
            drive.setAccount(user.displayName, user.phoneNumber)
            _uiState.value = _uiState.value.copy(accountName = user.displayName)
        }
        _uiState.value = _uiState.value.copy(
            chatId = chatId,
            existingDriveFound = existing.any { it.chatId == chatId }
        )
        status("Drive ready")
        return true
    }

    private suspend fun verifyStorage(): Boolean {
        status("Checking available space…")
        val usage = appModule.storageRegistry.local.usage()
        _uiState.value = _uiState.value.copy(verifiedBytes = usage.availableBytes)
        if (usage.availableBytes < MIN_FREE_BYTES) {
            _uiState.value = _uiState.value.copy(
                error = "Less than 200 MB free on this device. Free some space before syncing."
            )
            return false
        }
        // Creating the content key here rather than on first upload means a
        // failure surfaces during setup, not halfway through a patient's imaging.
        appModule.wrappedKeyRepository.getOrCreate(CryptoKeys.CONTENT)
        return true
    }

    private suspend fun testConnection(): Boolean {
        status("Sending a test object…")
        val limits = client.getLimits()
        status("Connected. Up to ${limits.maxFileBytes / (1024 * 1024)} MB per file.")
        return true
    }

    private suspend fun buildManifest(): Boolean {
        status("Reading the drive manifest…")
        val entries = appModule.syncEngine.rebuildManifest()
        _uiState.value = _uiState.value.copy(manifestEntries = entries)
        status(
            if (entries == 0) "New drive — nothing to restore."
            else "Found $entries objects already in this drive."
        )
        return true
    }

    private suspend fun runInitialSync(): Boolean {
        status("Running the first synchronisation…")
        drive.setBackend(StorageBackend.TELEGRAM)
        drive.setSetupCompleted(true)
        // An existing drive means this is a second device or a reinstall, so the
        // records it already holds are pulled before anything local is pushed.
        if (_uiState.value.manifestEntries > 0) {
            runCatching { appModule.syncEngine.pullDatabaseSnapshot() }
                .onFailure { DriveLog.w(TAG, "Snapshot restore skipped", it) }
        }
        val report = appModule.syncEngine.sync(SyncScope.FULL, ConflictPolicy.NEWEST_WINS)
        _uiState.value = _uiState.value.copy(initialSyncReport = report)
        SyncScheduler(applicationContext).kickTransfers()
        return true
    }

    private suspend fun finish(): Boolean {
        SyncScheduler(applicationContext).applyPreferences()
        return true
    }

    /** Abandons setup, leaving the backend on LOCAL and no data touched. */
    fun cancel() {
        viewModelScope.launch {
            drive.setSetupCompleted(false)
            drive.setBackend(StorageBackend.LOCAL)
        }
    }

    private fun runStep(message: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            setBusy(true)
            status(message)
            runCatching { block() }.onFailure { error ->
                _uiState.value = _uiState.value.copy(error = error.message ?: "Request failed")
            }
            setBusy(false)
        }
    }

    private fun setBusy(busy: Boolean) {
        _uiState.value = _uiState.value.copy(busy = busy)
    }

    private fun status(line: String) {
        _uiState.value = _uiState.value.copy(statusLine = line)
    }

    companion object {
        private const val TAG = "TelegramSetupVM"
        private const val MIN_FREE_BYTES = 200L * 1024 * 1024

        fun provideFactory(
            appModule: AppModule,
            applicationContext: Context
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TelegramSetupViewModel(appModule, applicationContext) as T
        }
    }
}

// ---------------------------------------------------------------------------
// Settings
// ---------------------------------------------------------------------------

data class DriveSettingsUiState(
    val settings: TelegramDriveSettings = TelegramDriveSettings(),
    val authState: TelegramAuthState = TelegramAuthState.Uninitialized,
    val connection: TelegramConnectionState = TelegramConnectionState.CONNECTING,
    val localUsage: StorageUsage? = null,
    val driveUsage: StorageUsage? = null,
    val pendingUploads: Int = 0,
    val pendingDownloads: Int = 0,
    val failedTransfers: Int = 0,
    val history: List<TransferQueueEntity> = emptyList(),
    val keyFingerprint: String? = null,
    val busyLabel: String? = null,
    val message: String? = null,
    val lastReport: SyncReport? = null,
    val integrity: IntegrityReport? = null
)

class TelegramDriveSettingsViewModel(
    private val appModule: AppModule,
    private val applicationContext: Context
) : ViewModel() {

    private val drive: TelegramDriveRepository get() = appModule.telegramDriveRepository
    private val queueDao get() = appModule.database.transferQueueDao()
    private val scheduler = SyncScheduler(applicationContext)

    private val _uiState = MutableStateFlow(DriveSettingsUiState())
    val uiState: StateFlow<DriveSettingsUiState> = _uiState.asStateFlow()

    /** Ring buffer shared with the whole drive layer; see [DriveLog]. */
    val logs: StateFlow<List<DriveLog.Entry>> = DriveLog.entries

    init {
        viewModelScope.launch {
            drive.settings.collect { settings ->
                _uiState.value = _uiState.value.copy(settings = settings)
                refreshUsage()
            }
        }
        viewModelScope.launch {
            appModule.telegramClient.authState.collect {
                _uiState.value = _uiState.value.copy(authState = it)
            }
        }
        viewModelScope.launch {
            appModule.telegramClient.connectionState.collect {
                _uiState.value = _uiState.value.copy(connection = it)
            }
        }
        viewModelScope.launch {
            queueDao.observePendingCount(TransferDirection.UPLOAD.name).collect {
                _uiState.value = _uiState.value.copy(pendingUploads = it)
            }
        }
        viewModelScope.launch {
            queueDao.observePendingCount(TransferDirection.DOWNLOAD.name).collect {
                _uiState.value = _uiState.value.copy(pendingDownloads = it)
            }
        }
        viewModelScope.launch {
            queueDao.observeFailedCount().collect {
                _uiState.value = _uiState.value.copy(failedTransfers = it)
            }
        }
        viewModelScope.launch {
            queueDao.observeHistory(HISTORY_LIMIT).collect {
                _uiState.value = _uiState.value.copy(history = it)
            }
        }
        _uiState.value = _uiState.value.copy(
            keyFingerprint = appModule.wrappedKeyRepository.fingerprint(CryptoKeys.CONTENT)
        )
    }

    // ---- Backend -------------------------------------------------------------

    /**
     * Switching backends is metadata only. Both stores keep every byte they hold;
     * moving content between them is the separate, explicit migration action.
     */
    fun selectBackend(backend: StorageBackend) = launchWith("Switching storage…") {
        if (backend == StorageBackend.TELEGRAM && !drive.current().setupCompleted) {
            message("Run Telegram Drive setup first.")
            return@launchWith
        }
        drive.setBackend(backend)
        scheduler.applyPreferences()
        message(
            if (backend == StorageBackend.LOCAL) {
                "Now using local storage. Nothing was deleted from your drive."
            } else {
                "Now using Telegram Drive. Your local files were left in place."
            }
        )
    }

    // ---- Sync preferences ----------------------------------------------------

    fun setSyncMode(mode: SyncMode) = launchWith(null) {
        drive.setSyncMode(mode)
        scheduler.applyPreferences()
    }

    fun setWifiOnly(enabled: Boolean) = launchWith(null) {
        drive.setSyncOnWifiOnly(enabled)
        scheduler.applyPreferences()
        val settings = drive.current()
        scheduler.restartTransfers(enabled, settings.syncWhileChargingOnly)
    }

    fun setChargingOnly(enabled: Boolean) = launchWith(null) {
        drive.setSyncWhileChargingOnly(enabled)
        scheduler.applyPreferences()
        val settings = drive.current()
        scheduler.restartTransfers(settings.syncOnWifiOnly, enabled)
    }

    fun setBackgroundSync(enabled: Boolean) = launchWith(null) {
        drive.setBackgroundSyncEnabled(enabled)
        scheduler.applyPreferences()
    }

    fun setEncryptUploads(enabled: Boolean) = launchWith(null) {
        drive.setEncryptUploads(enabled)
        message(
            if (enabled) "New uploads will be encrypted."
            else "New uploads will not be encrypted. Existing encrypted files stay readable."
        )
    }

    fun setKeepVersionHistory(enabled: Boolean) = launchWith(null) {
        drive.setKeepVersionHistory(enabled)
    }

    fun setCachePolicy(policy: CachePolicy) = launchWith(null) { drive.setCachePolicy(policy) }

    fun setCacheBudgetMb(megabytes: Int) = launchWith(null) { drive.setCacheBudgetMb(megabytes) }

    // ---- Actions -------------------------------------------------------------

    fun syncNow(full: Boolean = false) = launchWith("Synchronising…") {
        val report = appModule.syncEngine.sync(
            if (full) SyncScope.FULL else SyncScope.INCREMENTAL
        )
        _uiState.value = _uiState.value.copy(lastReport = report)
        if (report.uploadsQueued > 0 || report.downloadsQueued > 0) {
            val settings = drive.current()
            scheduler.kickTransfers(settings.syncOnWifiOnly, settings.syncWhileChargingOnly)
        }
        message(
            if (report.succeeded) {
                "Queued ${report.uploadsQueued} uploads and ${report.downloadsQueued} downloads."
            } else {
                report.errors.first()
            }
        )
    }

    fun retryFailedTransfers() = launchWith("Retrying…") {
        queueDao.retryAllFailed(System.currentTimeMillis())
        val settings = drive.current()
        scheduler.kickTransfers(settings.syncOnWifiOnly, settings.syncWhileChargingOnly)
        message("Failed transfers were requeued.")
    }

    fun reconnect() = launchWith("Reconnecting…") {
        val credentials = drive.credentials()
        if (credentials == null) {
            message("No stored Telegram credentials. Run setup again.")
            return@launchWith
        }
        appModule.telegramClient.start(credentials)
        message("Reconnecting to Telegram.")
    }

    /**
     * Signs out of Telegram and reverts to local storage. Nothing is deleted:
     * local files stay, and the drive channel stays in the user's account.
     */
    fun logout() = launchWith("Signing out…") {
        runCatching { appModule.telegramClient.logout() }
        scheduler.cancelAll()
        drive.clearAccountState()
        message("Signed out. Local storage is active and no data was removed.")
    }

    fun rebuildManifest() = launchWith("Rebuilding manifest…") {
        val entries = appModule.syncEngine.rebuildManifest()
        message("Manifest rebuilt from $entries drive entries.")
    }

    fun verifyIntegrity() = launchWith("Verifying…") {
        val report = appModule.syncEngine.verifyIntegrity()
        _uiState.value = _uiState.value.copy(integrity = report)
        message(
            if (report.healthy) "All ${report.checked} objects verified."
            else "${report.missingRemotely.size} missing, ${report.hashMismatches.size} mismatched."
        )
    }

    fun clearCache() = launchWith("Clearing cache…") {
        val freed = appModule.telegramStorageProvider.clearCache()
        refreshUsage()
        message("Freed ${freed / (1024 * 1024)} MB of cache. No stored data was removed.")
    }

    fun migrateLocalToDrive() = launchWith("Copying local content to the drive…") {
        val report = appModule.syncEngine.sync(SyncScope.FULL, ConflictPolicy.PREFER_LOCAL)
        _uiState.value = _uiState.value.copy(lastReport = report)
        val settings = drive.current()
        scheduler.kickTransfers(settings.syncOnWifiOnly, settings.syncWhileChargingOnly)
        message("Queued ${report.uploadsQueued} objects for upload. Local copies are kept.")
    }

    fun migrateDriveToLocal() = launchWith("Restoring drive content to this device…") {
        runCatching { appModule.syncEngine.pullDatabaseSnapshot() }
        val report = appModule.syncEngine.sync(SyncScope.FULL, ConflictPolicy.PREFER_REMOTE)
        _uiState.value = _uiState.value.copy(lastReport = report)
        val settings = drive.current()
        scheduler.kickTransfers(settings.syncOnWifiOnly, settings.syncWhileChargingOnly)
        message("Queued ${report.downloadsQueued} objects for download.")
    }

    fun clearLogs() {
        DriveLog.clear()
    }

    fun dismissMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private suspend fun refreshUsage() {
        val local = runCatching { appModule.storageRegistry.local.usage() }.getOrNull()
        val remote = runCatching { appModule.telegramStorageProvider.usage() }.getOrNull()
        _uiState.value = _uiState.value.copy(localUsage = local, driveUsage = remote)
    }

    private fun message(text: String) {
        _uiState.value = _uiState.value.copy(message = text)
    }

    private fun launchWith(label: String?, block: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busyLabel = label)
            runCatching { block() }.onFailure { error ->
                DriveLog.e(TAG, "Settings action failed", error)
                message(error.message ?: "Action failed")
            }
            _uiState.value = _uiState.value.copy(busyLabel = null)
        }
    }

    companion object {
        private const val TAG = "DriveSettingsVM"
        private const val HISTORY_LIMIT = 50

        fun provideFactory(
            appModule: AppModule,
            applicationContext: Context
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TelegramDriveSettingsViewModel(appModule, applicationContext) as T
        }
    }
}
