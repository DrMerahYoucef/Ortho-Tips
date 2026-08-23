package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.importer.ImportState
import com.example.data.importer.JsonDatabaseImporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ImportViewModel(
    private val importer: JsonDatabaseImporter
) : ViewModel() {

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    fun importJsonData(jsonString: String) {
        if (jsonString.isBlank()) {
            _importState.value = ImportState.Error("JSON data cannot be empty")
            return
        }

        viewModelScope.launch {
            _importState.value = ImportState.Loading("Parsing and indexing surgical guidelines...")
            val result = importer.importOrthoJson(jsonString)
            result.fold(
                onSuccess = { count ->
                    _importState.value = ImportState.Success(
                        importedCount = count,
                        details = "Successfully imported $count orthopedic procedures/sections into local clinical library."
                    )
                },
                onFailure = { error ->
                    _importState.value = ImportState.Error("Import failed: ${error.localizedMessage ?: "Invalid JSON format"}")
                }
            )
        }
    }

    fun resetState() {
        _importState.value = ImportState.Idle
    }

    companion object {
        fun provideFactory(importer: JsonDatabaseImporter): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ImportViewModel(importer) as T
                }
            }
    }
}
