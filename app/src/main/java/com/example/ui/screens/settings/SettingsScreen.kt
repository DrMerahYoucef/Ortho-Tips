package com.example.ui.screens.settings

import android.app.Activity
import android.os.Build
import android.view.WindowManager
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.WbIncandescent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.prefs.FontSizePreference
import com.example.data.prefs.ThemeMode
import com.example.utils.MediaActions
import com.example.viewmodel.DatabaseConfirmAction
import com.example.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToImport: () -> Unit,
    onNavigateToTelegramDrive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isExportDialogOpen by viewModel.isExportDialogOpen.collectAsStateWithLifecycle()
    val isImportDialogOpen by viewModel.isImportDialogOpen.collectAsStateWithLifecycle()
    val backupJson by viewModel.backupJson.collectAsStateWithLifecycle()
    val importMessage by viewModel.importMessage.collectAsStateWithLifecycle()
    val pendingDatabaseAction by viewModel.pendingDatabaseAction.collectAsStateWithLifecycle()
    val databaseActionMessage by viewModel.databaseActionMessage.collectAsStateWithLifecycle()

    val context = LocalContext.current

    LaunchedEffect(settings.operativeMode) {
        val activity = context as? Activity
        if (settings.operativeMode) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    if (isExportDialogOpen && backupJson != null) {
        AlertDialog(
            onDismissRequest = { viewModel.closeExportDialog() },
            title = { Text(text = "Export Backup Data", style = MaterialTheme.typography.titleMedium) },
            text = {
                Column {
                    Text(
                        text = "Your bookmarks and case notes have been packaged as JSON data:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = backupJson ?: "",
                        onValueChange = {},
                        readOnly = true,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    MediaActions.shareClinicalTip(context, "Ortho Tips Backup", backupJson ?: "")
                    viewModel.closeExportDialog()
                }) { Text("Share / Export") }
            },
            dismissButton = {
                TextButton(onClick = { MediaActions.copyToClipboard(context, "Backup JSON", backupJson ?: "") }) {
                    Text("Copy")
                }
            }
        )
    }

    if (isImportDialogOpen) {
        var jsonInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.closeImportDialog() },
            title = { Text(text = "Restore Backup Data", style = MaterialTheme.typography.titleMedium) },
            text = {
                Column {
                    Text(
                        text = "Paste your exported JSON backup to restore notes and bookmarks:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = jsonInput,
                        onValueChange = { jsonInput = it },
                        placeholder = { Text("Paste JSON here...") },
                        minLines = 4,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (importMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = importMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.restoreBackup(jsonInput) }, enabled = jsonInput.isNotBlank()) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeImportDialog() }) { Text("Close") }
            }
        )
    }

    if (pendingDatabaseAction != null) {
        val isReinitialize = pendingDatabaseAction == DatabaseConfirmAction.REINITIALIZE
        AlertDialog(
            onDismissRequest = { viewModel.dismissDatabaseActionDialog() },
            title = {
                Text(
                    text = if (isReinitialize) "Reinitialize Database?" else "Remove Database?",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = if (isReinitialize) {
                        "This clears all imported clinical data and also resets your saved notes and bookmarks. This action cannot be undone."
                    } else {
                        "Are you sure you want to remove the database? All imported bones, chapters, and figures will be deleted. This action cannot be undone."
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDatabaseAction() },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(if (isReinitialize) "Reinitialize" else "Remove") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDatabaseActionDialog() }) { Text("Cancel") }
            }
        )
    }

    if (databaseActionMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDatabaseActionMessage() },
            title = { Text(text = "Clinical Data Management", style = MaterialTheme.typography.titleMedium) },
            text = { Text(text = databaseActionMessage ?: "", style = MaterialTheme.typography.bodySmall) },
            confirmButton = {
                Button(onClick = { viewModel.dismissDatabaseActionMessage() }) { Text("OK") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings & OR Tools",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-0.3).sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (settings.operativeMode) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth().testTag("operative_mode_card")
                ) {
                    Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (settings.operativeMode) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.WbIncandescent,
                                    contentDescription = null,
                                    tint = if (settings.operativeMode) Color.White else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Operating Room Mode",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (settings.operativeMode) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Prevents screen timeout during surgical cases & scrubbed procedures.",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (settings.operativeMode) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.operativeMode,
                            onCheckedChange = { viewModel.setOperativeMode(it) },
                            modifier = Modifier.testTag("operative_mode_switch")
                        )
                    }
                }
            }

            item {
                SettingsSection(title = "CLINICAL DISPLAY & THEME") {
                    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(text = "Material You Dynamic Color", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = if (supportsDynamicColor) {
                                    "App components adapt to Android system wallpaper and accent palette"
                                } else {
                                    "Requires Android 12+ (Using handcrafted clinical fallback)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.dynamicColor && supportsDynamicColor,
                            onCheckedChange = { viewModel.setDynamicColor(it) },
                            enabled = supportsDynamicColor,
                            modifier = Modifier.testTag("dynamic_color_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Theme Appearance", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "Current: ${settings.themeMode.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row {
                            ThemeMode.entries.forEach { mode ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (settings.themeMode == mode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    modifier = Modifier.clickable { viewModel.setThemeMode(mode) }.padding(horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = mode.name.take(3),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (settings.themeMode == mode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Technique Font Size", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = settings.fontSize.name.replace("_", " "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row {
                            FontSizePreference.entries.forEach { size ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (settings.fontSize == size) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    modifier = Modifier.clickable { viewModel.setFontSize(size) }.padding(horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = when (size) {
                                            FontSizePreference.STANDARD -> "1x"
                                            FontSizePreference.LARGE -> "1.2x"
                                            FontSizePreference.EXTRA_LARGE -> "1.4x"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (settings.fontSize == size) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                SettingsSection(title = "SMART INPUT") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Smart suggestions & autofill", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "Learns medical terms, hospitals, surgeons and anesthesia types you type, and suggests them next time.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = settings.smartSuggestionsEnabled, onCheckedChange = { viewModel.setSmartSuggestions(it) })
                    }
                }
            }

            item {
                SettingsSection(title = "CLOUD STORAGE") {
                    SettingsActionRow(
                        icon = Icons.Default.Cloud,
                        title = "Telegram Drive",
                        subtitle = "Optional encrypted cloud backup for patients, media and records",
                        onClick = onNavigateToTelegramDrive
                    )
                }
            }

            item {
                SettingsSection(title = "CLINICAL DATA MANAGEMENT") {
                    SettingsActionRow(
                        icon = Icons.Default.CloudUpload,
                        title = "Export Backup Data (JSON)",
                        subtitle = "Back up saved notes and bookmarks to portable format",
                        onClick = { viewModel.generateBackup() }
                    )
                    SettingsActionRow(
                        icon = Icons.Default.CloudDownload,
                        title = "Restore Backup Data (JSON)",
                        subtitle = "Import notes and bookmarks from saved backup",
                        onClick = { viewModel.openImportDialog() }
                    )
                    SettingsActionRow(
                        icon = Icons.Default.Download,
                        title = "Import Custom Surgical Guidelines (ZIP)",
                        subtitle = "Load new procedures, chapters, and reduction guides into database",
                        onClick = onNavigateToImport
                    )
                    SettingsActionRow(
                        icon = Icons.Default.RestartAlt,
                        title = "Reinitialize Database",
                        subtitle = "Fully reset the local database and clear related notes/bookmarks",
                        onClick = { viewModel.requestReinitializeDatabase() }
                    )
                    SettingsActionRow(
                        icon = Icons.Default.DeleteForever,
                        title = "Remove Database",
                        subtitle = "Delete all imported clinical data from this device",
                        onClick = { viewModel.requestRemoveDatabase() }
                    )
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CLINICAL REFERENCE DISCLAIMER",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Ortho Tips is an educational and surgical reference tool for orthopedic residents, fellows, and surgeons. It is not a substitute for clinical judgment, official AO/OTA guidelines, or individualized patient care.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Ortho Tips App • Version 1.0.0 (Build 2026)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
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
private fun SettingsActionRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(7.dp).size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
    }
}