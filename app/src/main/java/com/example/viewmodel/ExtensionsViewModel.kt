package com.example.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.extensions.ExtensionRepository
import com.example.data.extensions.OrthoExtension
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** One-off outcomes shown as a short message by the Extensions tab. */
sealed interface ExtensionEvent {
    data class Installed(val name: String, val version: String, val previousVersion: String?) : ExtensionEvent
    data object ImportUnreadable : ExtensionEvent
    data object ImportNotHtml : ExtensionEvent
    data object ImportTooLarge : ExtensionEvent
    data class Deleted(val name: String) : ExtensionEvent
    data class DataCleared(val name: String) : ExtensionEvent
    data class Exported(val ok: Boolean) : ExtensionEvent
}

class ExtensionsViewModel(
    private val repository: ExtensionRepository
) : ViewModel() {

    val extensions: StateFlow<List<OrthoExtension>> = repository.extensions

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _events = MutableSharedFlow<ExtensionEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<ExtensionEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.refresh()
            _loading.value = false
        }
    }

    fun import(uri: Uri) {
        viewModelScope.launch {
            val event = when (val result = repository.import(uri)) {
                is ExtensionRepository.ImportResult.Installed -> ExtensionEvent.Installed(
                    name = result.extension.name,
                    version = result.extension.version,
                    previousVersion = result.previousVersion
                )
                ExtensionRepository.ImportResult.Unreadable -> ExtensionEvent.ImportUnreadable
                ExtensionRepository.ImportResult.NotHtml -> ExtensionEvent.ImportNotHtml
                ExtensionRepository.ImportResult.TooLarge -> ExtensionEvent.ImportTooLarge
            }
            _events.emit(event)
        }
    }

    fun delete(extension: OrthoExtension) {
        viewModelScope.launch {
            repository.delete(extension.id)
            _events.emit(ExtensionEvent.Deleted(extension.name))
        }
    }

    fun clearData(extension: OrthoExtension) {
        viewModelScope.launch {
            repository.clearData(extension.id)
            _events.emit(ExtensionEvent.DataCleared(extension.name))
        }
    }

    fun export(extension: OrthoExtension, destination: Uri) {
        viewModelScope.launch {
            _events.emit(ExtensionEvent.Exported(repository.export(extension.id, destination)))
        }
    }

    companion object {
        fun provideFactory(repository: ExtensionRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ExtensionsViewModel(repository) as T
            }
    }
}
