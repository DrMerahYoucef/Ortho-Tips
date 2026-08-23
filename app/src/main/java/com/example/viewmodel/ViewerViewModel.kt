package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.ImageEntity
import com.example.data.repository.OrthoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ViewerUiState(
    val image: ImageEntity? = null,
    val isLoading: Boolean = true
)

class ViewerViewModel(
    private val repository: OrthoRepository,
    private val imageId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(ViewerUiState(isLoading = true))
    val uiState: StateFlow<ViewerUiState> = _uiState.asStateFlow()

    init {
        loadImage()
    }

    private fun loadImage() {
        viewModelScope.launch {
            val img = repository.getImageById(imageId)
            _uiState.value = ViewerUiState(image = img, isLoading = false)
        }
    }

    companion object {
        fun provideFactory(
            repository: OrthoRepository,
            imageId: String
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ViewerViewModel(repository, imageId) as T
            }
        }
    }
}
