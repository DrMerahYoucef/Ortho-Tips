package com.example.ui.screens.storage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.TransferState
import com.example.data.prefs.CachePolicy
import com.example.data.prefs.SyncMode
import com.example.data.storage.StorageBackend
import com.example.data.telegram.TelegramAuthState
import com.example.data.telegram.TelegramConnectionState
import com.example.viewmodel.DriveSettingsUiState
import com.example.viewmodel.TelegramDriveSettingsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The Telegram Drive section of Settings.
 *
 * Grouped so the destructive-looking actions sit apart from the everyday ones,
 * and every action that could be read as "this might delete my patients" says
 * plainly in its subtitle what it does not touch.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelegramDriveSettingsScreen(
    viewModel: TelegramDriveSettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSetup: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmAction by remember { mutableStateOf<ConfirmAction?>(null) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    confirmAction?.let { action ->
        AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text(action.title) },
            text = { Text(action.body, style = MaterialTheme.typography.bodySmall) },
            confirmButton = {
                Button(onClick = {
                    action.onConfirm()
                    confirmAction = null
                }) { Text(action.confirmLabel) }
            },
            dismissButton = {
                TextButton(onClick = { confirmAction = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Telegram Drive") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item { ConnectionCard(state) }

                item {
                    DriveSection("STORAGE BACKEND") {
                        BackendRow(
                            label = "Local storage",
                            description = "Records stay on this device only. This is the default.",
                            selected = state.settings.backend == StorageBackend.LOCAL,
                            onClick = { viewModel.selectBackend(StorageBackend.LOCAL) }
                        )
                        BackendRow(
                            label = "Telegram Drive",
                            description = if (state.settings.setupCompleted) {
                                "Encrypted copies in your private Telegram channel."
                            } else {
                                "Not set up yet — tap Run setup below."
                            },
                            selected = state.settings.backend == StorageBackend.TELEGRAM,
                            onClick = { viewModel.selectBackend(StorageBackend.TELEGRAM) }
                        )
                        if (!state.settings.setupCompleted) {
                            DriveActionRow(
                                icon = Icons.Default.CloudUpload,
                                title = "Run Telegram Drive setup",
                                subtitle = "Link an account and create your private drive",
                                onClick = onNavigateToSetup
                            )
                        }
                    }
                }

                if (state.settings.setupCompleted) {
                    item { DriveSection("ACCOUNT") { AccountRows(state, viewModel, onNavigateToSetup) { confirmAction = it } } }
                    item { DriveSection("STORAGE USE") { UsageRows(state) } }
                    item { DriveSection("SYNCHRONISATION") { SyncRows(state, viewModel) } }
                    item { DriveSection("TRANSFERS") { TransferRows(state, viewModel) } }
                    item { DriveSection("CACHE") { CacheRows(state, viewModel) } }
                    item { DriveSection("MIGRATION") { MigrationRows(viewModel) { confirmAction = it } } }
                    item { DriveSection("MAINTENANCE") { MaintenanceRows(viewModel, onNavigateToDiagnostics) { confirmAction = it } } }
                    item { TransferHistoryCard(state) }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }

            state.busyLabel?.let { label ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

private data class ConfirmAction(
    val title: String,
    val body: String,
    val confirmLabel: String,
    val onConfirm: () -> Unit
)

@Composable
private fun ConnectionCard(state: DriveSettingsUiState) {
    val connected = state.authState == TelegramAuthState.Ready
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (connected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(18.dp)
        ) {
            Icon(
                imageVector = if (connected) Icons.Default.CloudDone else Icons.Default.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        !state.settings.setupCompleted -> "Not set up"
                        connected -> "Connected"
                        state.authState is TelegramAuthState.Failed ->
                            (state.authState as TelegramAuthState.Failed).message
                        else -> "Connecting…"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = buildString {
                        if (state.settings.accountName.isNotBlank()) {
                            append(state.settings.accountName)
                            if (state.settings.accountPhoneSuffix.isNotBlank()) {
                                append(" (•••${state.settings.accountPhoneSuffix})")
                            }
                            append(" • ")
                        }
                        append(
                            when (state.connection) {
                                TelegramConnectionState.READY -> "network ready"
                                TelegramConnectionState.UPDATING -> "updating"
                                TelegramConnectionState.CONNECTING -> "connecting"
                                TelegramConnectionState.WAITING_FOR_NETWORK -> "offline"
                            }
                        )
                    },
                    style = MaterialTheme.typography.bodySmall
                )
                if (state.settings.lastSyncAt > 0) {
                    Text(
                        text = "Last sync ${formatTime(state.settings.lastSyncAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountRows(
    state: DriveSettingsUiState,
    viewModel: TelegramDriveSettingsViewModel,
    onNavigateToSetup: () -> Unit,
    onConfirm: (ConfirmAction) -> Unit
) {
    DriveActionRow(
        icon = Icons.Default.Refresh,
        title = "Reconnect",
        subtitle = "Restart the Telegram session using stored credentials",
        onClick = viewModel::reconnect
    )
    DriveActionRow(
        icon = Icons.Default.CloudUpload,
        title = "Re-run setup",
        subtitle = "Link a different account or rebuild the drive",
        onClick = onNavigateToSetup
    )
    state.keyFingerprint?.let {
        DriveInfoRow("Encryption key", "Fingerprint $it — keep this device to keep access")
    }
    DriveActionRow(
        icon = Icons.Default.Logout,
        title = "Sign out of Telegram",
        subtitle = "Reverts to local storage. Nothing is deleted on either side.",
        onClick = {
            onConfirm(
                ConfirmAction(
                    title = "Sign out of Telegram?",
                    body = "Ortho Tips will switch back to local storage. Your local files " +
                            "stay on this device and the drive channel stays in your Telegram " +
                            "account. Nothing is deleted.",
                    confirmLabel = "Sign out",
                    onConfirm = viewModel::logout
                )
            )
        }
    )
}

@Composable
private fun UsageRows(state: DriveSettingsUiState) {
    state.localUsage?.let {
        DriveInfoRow(
            "Local storage",
            "${formatBytes(it.usedBytes)} across ${it.objectCount} files • " +
                    "${formatBytes(it.availableBytes)} free on device"
        )
    }
    state.driveUsage?.let {
        DriveInfoRow(
            "Telegram Drive",
            "${formatBytes(it.usedBytes)} across ${it.objectCount} objects • " +
                    "no account quota, 2 GB per file (4 GB with Premium)"
        )
    }
}

@Composable
private fun SyncRows(state: DriveSettingsUiState, viewModel: TelegramDriveSettingsViewModel) {
    DriveToggleRow(
        title = "Automatic synchronisation",
        subtitle = "Queue changes as you work instead of only on demand",
        checked = state.settings.syncMode == SyncMode.AUTOMATIC,
        onChange = { viewModel.setSyncMode(if (it) SyncMode.AUTOMATIC else SyncMode.MANUAL) }
    )
    DriveToggleRow(
        title = "Background synchronisation",
        subtitle = "Run a periodic sync even when Ortho Tips is closed",
        checked = state.settings.backgroundSyncEnabled,
        onChange = viewModel::setBackgroundSync
    )
    DriveToggleRow(
        title = "Sync on Wi-Fi only",
        subtitle = "Radiographs are large; this avoids mobile data charges",
        checked = state.settings.syncOnWifiOnly,
        onChange = viewModel::setWifiOnly
    )
    DriveToggleRow(
        title = "Sync while charging only",
        subtitle = "Defers transfers until the phone is on a charger",
        checked = state.settings.syncWhileChargingOnly,
        onChange = viewModel::setChargingOnly
    )
    DriveToggleRow(
        title = "Encrypt uploads",
        subtitle = "AES-256 before anything leaves the device. Recommended for patient data.",
        checked = state.settings.encryptUploads,
        onChange = viewModel::setEncryptUploads
    )
    DriveToggleRow(
        title = "Keep version history",
        subtitle = "Retain superseded copies in the drive instead of replacing them",
        checked = state.settings.keepVersionHistory,
        onChange = viewModel::setKeepVersionHistory
    )
    DriveActionRow(
        icon = Icons.Default.Sync,
        title = "Synchronise now",
        subtitle = "Run an incremental sync straight away",
        onClick = { viewModel.syncNow(full = false) }
    )
    DriveActionRow(
        icon = Icons.Default.Sync,
        title = "Full synchronisation",
        subtitle = "Re-examine every object rather than only what changed",
        onClick = { viewModel.syncNow(full = true) }
    )
}

@Composable
private fun TransferRows(state: DriveSettingsUiState, viewModel: TelegramDriveSettingsViewModel) {
    DriveInfoRow("Pending uploads", "${state.pendingUploads} waiting")
    DriveInfoRow("Pending downloads", "${state.pendingDownloads} waiting")
    if (state.failedTransfers > 0) {
        DriveActionRow(
            icon = Icons.Default.Refresh,
            title = "Retry failed transfers",
            subtitle = "${state.failedTransfers} failed and can be requeued",
            onClick = viewModel::retryFailedTransfers
        )
    }
}

@Composable
private fun CacheRows(state: DriveSettingsUiState, viewModel: TelegramDriveSettingsViewModel) {
    Column(modifier = Modifier.padding(14.dp)) {
        Text("What stays on this device", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CachePolicy.entries.forEach { policy ->
                FilterChip(
                    selected = state.settings.cachePolicy == policy,
                    onClick = { viewModel.setCachePolicy(policy) },
                    label = {
                        Text(
                            when (policy) {
                                CachePolicy.KEEP_EVERYTHING -> "Everything"
                                CachePolicy.HYBRID -> "Recent"
                                CachePolicy.MINIMAL -> "Minimal"
                            }
                        )
                    }
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Cache limit: ${state.settings.cacheBudgetMb} MB",
            style = MaterialTheme.typography.bodySmall
        )
        Slider(
            value = state.settings.cacheBudgetMb.toFloat(),
            onValueChange = { viewModel.setCacheBudgetMb(it.toInt()) },
            valueRange = 128f..4096f,
            steps = 30
        )
    }
    DriveActionRow(
        icon = Icons.Default.DeleteSweep,
        title = "Clear cache",
        subtitle = "Frees downloaded copies only. Drive content is not affected.",
        onClick = viewModel::clearCache
    )
}

@Composable
private fun MigrationRows(
    viewModel: TelegramDriveSettingsViewModel,
    onConfirm: (ConfirmAction) -> Unit
) {
    DriveActionRow(
        icon = Icons.Default.CloudUpload,
        title = "Copy local content to the drive",
        subtitle = "Uploads everything on this device. Local copies are kept.",
        onClick = {
            onConfirm(
                ConfirmAction(
                    title = "Copy everything to Telegram Drive?",
                    body = "Every patient image, document and record on this device will be " +
                            "queued for upload. Local files are kept exactly as they are.",
                    confirmLabel = "Start",
                    onConfirm = viewModel::migrateLocalToDrive
                )
            )
        }
    )
    DriveActionRow(
        icon = Icons.Default.Download,
        title = "Restore drive content to this device",
        subtitle = "Downloads everything from the drive and merges records.",
        onClick = {
            onConfirm(
                ConfirmAction(
                    title = "Restore from Telegram Drive?",
                    body = "Records from the drive are merged into this device. Rows that " +
                            "exist only here are kept — nothing local is erased.",
                    confirmLabel = "Restore",
                    onConfirm = viewModel::migrateDriveToLocal
                )
            )
        }
    )
}

@Composable
private fun MaintenanceRows(
    viewModel: TelegramDriveSettingsViewModel,
    onNavigateToDiagnostics: () -> Unit,
    onConfirm: (ConfirmAction) -> Unit
) {
    DriveActionRow(
        icon = Icons.Default.VerifiedUser,
        title = "Verify integrity",
        subtitle = "Checks every indexed object still resolves and hashes correctly",
        onClick = viewModel::verifyIntegrity
    )
    DriveActionRow(
        icon = Icons.Default.Restore,
        title = "Rebuild manifest",
        subtitle = "Re-reads the drive to repair the local index. Nothing is uploaded.",
        onClick = {
            onConfirm(
                ConfirmAction(
                    title = "Rebuild the manifest?",
                    body = "The local index is discarded and rebuilt from the drive channel. " +
                            "No file is uploaded, downloaded or deleted.",
                    confirmLabel = "Rebuild",
                    onConfirm = viewModel::rebuildManifest
                )
            )
        }
    )
    DriveActionRow(
        icon = Icons.Default.BugReport,
        title = "Diagnostics and logs",
        subtitle = "Transfer history, storage health and recent events",
        onClick = onNavigateToDiagnostics
    )
}

@Composable
private fun TransferHistoryCard(state: DriveSettingsUiState) {
    if (state.history.isEmpty()) return
    DriveSection("RECENT TRANSFERS") {
        state.history.take(12).forEach { row ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(14.dp)
            ) {
                Icon(
                    imageVector = when (row.state) {
                        TransferState.SUCCEEDED.name -> Icons.Default.CheckCircle
                        TransferState.FAILED.name -> Icons.Default.CloudOff
                        else -> Icons.Default.Sync
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = row.storageKey.substringAfterLast('/'),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${row.direction.lowercase()} • ${row.state.lowercase()}" +
                                (row.lastError?.let { " • $it" } ?: ""),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (row.state == TransferState.RUNNING.name && row.totalBytes > 0) {
                        LinearProgressIndicator(
                            progress = { row.bytesTransferred.toFloat() / row.totalBytes },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// ---- Shared row composables -----------------------------------------------

@Composable
internal fun DriveSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun BackendRow(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(14.dp)
    ) {
        Icon(
            imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.CloudOff,
            contentDescription = null,
            tint = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            },
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun DriveActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(14.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(7.dp).size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DriveToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(14.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
internal fun DriveInfoRow(title: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

internal fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024 * 1024 -> "%.1f GB".format(bytes / (1024.0 * 1024 * 1024))
    bytes >= 1024L * 1024 -> "%.0f MB".format(bytes / (1024.0 * 1024))
    bytes >= 1024 -> "%.0f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}

internal fun formatTime(epochMillis: Long): String =
    SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(epochMillis))
