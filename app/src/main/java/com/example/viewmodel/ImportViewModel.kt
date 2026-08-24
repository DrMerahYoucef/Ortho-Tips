package com.example.viewmodel

import android.content.Context
import android.net.Uri
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

    private val _selectedFileName = MutableStateFlow<String?>(null)
    val selectedFileName: StateFlow<String?> = _selectedFileName.asStateFlow()

    fun onFileSelected(context: Context, uri: Uri) {
        _selectedFileName.value = queryDisplayName(context, uri) ?: uri.lastPathSegment
        _importState.value = ImportState.Idle

        viewModelScope.launch {
            _importState.value = ImportState.Loading("Reading database file...")
            val jsonString = try {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            } catch (e: Exception) {
                null
            }

            if (jsonString.isNullOrBlank()) {
                _importState.value = ImportState.Error("Could not read the selected file.")
                return@launch
            }

            _importState.value = ImportState.Loading("Parsing and indexing clinical database...")
            val result = importer.importOrthoJson(jsonString)
            result.fold(
                onSuccess = { summary ->
                    _importState.value = ImportState.Success(
                        summary = summary,
                        details = "Imported ${summary.boneCount} bones across ${summary.chapterCount} regions, " +
                            "${summary.pageCount} pages, and ${summary.figureCount} figures."
                    )
                },
                onFailure = { error ->
                    _importState.value = ImportState.Error(
                        error.localizedMessage ?: "Invalid or malformed JSON file."
                    )
                }
            )
        }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun resetState() {
        _importState.value = ImportState.Idle
        _selectedFileName.value = null
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
