package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupManager
import com.example.data.prefs.FontSizePreference
import com.example.data.prefs.SettingsRepository
import com.example.data.prefs.ThemeMode
import com.example.data.prefs.UserSettings
import com.example.data.repository.OrthoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DatabaseConfirmAction { REMOVE, REINITIALIZE }

data class SettingsUiState(
    val userSettings: UserSettings = UserSettings(),
    val backupJson: String? = null,
    val importMessage: String? = null,
    val isExportDialogOpen: Boolean = false,
    val isImportDialogOpen: Boolean = false
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val backupManager: BackupManager,
    private val orthoRepository: OrthoRepository
) : ViewModel() {

    val settings: StateFlow<UserSettings> = settingsRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettings()
        )

    private val _backupJson = MutableStateFlow<String?>(null)
    val backupJson = _backupJson.asStateFlow()

    private val _importMessage = MutableStateFlow<String?>(null)
    val importMessage = _importMessage.asStateFlow()

    private val _isExportDialogOpen = MutableStateFlow(false)
    val isExportDialogOpen = _isExportDialogOpen.asStateFlow()

    private val _isImportDialogOpen = MutableStateFlow(false)
    val isImportDialogOpen = _isImportDialogOpen.asStateFlow()

    private val _pendingDatabaseAction = MutableStateFlow<DatabaseConfirmAction?>(null)
    val pendingDatabaseAction = _pendingDatabaseAction.asStateFlow()

    private val _databaseActionMessage = MutableStateFlow<String?>(null)
    val databaseActionMessage = _databaseActionMessage.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun setOperativeMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setOperativeMode(enabled)
        }
    }

    fun setHighContrast(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHighContrast(enabled)
        }
    }

    fun setFontSize(size: FontSizePreference) {
        viewModelScope.launch {
            settingsRepository.setFontSize(size)
        }
    }

    fun setQuickPearls(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setQuickPearls(enabled)
        }
    }

    fun setSmartSuggestions(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSmartSuggestions(enabled)
        }
    }

    fun generateBackup() {
        viewModelScope.launch {
            val json = backupManager.exportDataJson()
            _backupJson.value = json
            _isExportDialogOpen.value = true
        }
    }

    fun closeExportDialog() {
        _isExportDialogOpen.value = false
        _backupJson.value = null
    }

    fun openImportDialog() {
        _isImportDialogOpen.value = true
        _importMessage.value = null
    }

    fun closeImportDialog() {
        _isImportDialogOpen.value = false
        _importMessage.value = null
    }

    fun restoreBackup(jsonString: String) {
        viewModelScope.launch {
            val res = backupManager.importDataJson(jsonString)
            if (res.isSuccess) {
                _importMessage.value = "Successfully restored ${res.getOrNull()} items!"
            } else {
                _importMessage.value = "Error restoring backup: ${res.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun requestRemoveDatabase() {
        _databaseActionMessage.value = null
        _pendingDatabaseAction.value = DatabaseConfirmAction.REMOVE
    }

    fun requestReinitializeDatabase() {
        _databaseActionMessage.value = null
        _pendingDatabaseAction.value = DatabaseConfirmAction.REINITIALIZE
    }

    fun dismissDatabaseActionDialog() {
        _pendingDatabaseAction.value = null
    }

    fun confirmDatabaseAction() {
        val action = _pendingDatabaseAction.value ?: return
        _pendingDatabaseAction.value = null
        viewModelScope.launch {
            when (action) {
                DatabaseConfirmAction.REMOVE -> {
                    orthoRepository.removeDatabase()
                    _databaseActionMessage.value = "Clinical database removed."
                }
                DatabaseConfirmAction.REINITIALIZE -> {
                    orthoRepository.reinitializeDatabase()
                    _databaseActionMessage.value = "Database reinitialized. Notes and bookmarks were cleared."
                }
            }
        }
    }

    fun dismissDatabaseActionMessage() {
        _databaseActionMessage.value = null
    }

    companion object {
        fun provideFactory(
            settingsRepository: SettingsRepository,
            backupManager: BackupManager,
            orthoRepository: OrthoRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(settingsRepository, backupManager, orthoRepository) as T
            }
        }
    }
}
