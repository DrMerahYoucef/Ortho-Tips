package com.example.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.model.ChapterRef
import com.example.data.local.model.ClassificationChapter
import com.example.data.repository.ClassificationRepository
import kotlinx.coroutines.launch

/**
 * Backs both the chapters list (Home > Classifications tab) and the chapter detail screen.
 * All data comes from [ClassificationRepository], which reads static bundled JSON assets.
 */
class ClassificationViewModel(
    private val repository: ClassificationRepository
) : ViewModel() {

    var chapters by mutableStateOf<List<ChapterRef>>(emptyList())
        private set

    var currentChapter by mutableStateOf<ClassificationChapter?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    private var loadedChapterFile: String? = null

    /** No-ops if the index is already loaded, so re-entering the tab doesn't re-parse it. */
    fun loadIndex() {
        if (chapters.isNotEmpty() || isLoading) return
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                chapters = repository.getIndex().chapters
            } catch (e: Exception) {
                errorMessage = e.message ?: "Unable to load the classifications index."
            }
            isLoading = false
        }
    }

    fun openChapter(fileRelativePath: String) {
        // Already showing this chapter — avoid a redundant reload/flash of the loading state.
        if (loadedChapterFile == fileRelativePath && currentChapter != null) return
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            currentChapter = null
            try {
                currentChapter = repository.getChapter(fileRelativePath)
                loadedChapterFile = fileRelativePath
            } catch (e: Exception) {
                errorMessage = e.message ?: "Unable to load this chapter."
            }
            isLoading = false
        }
    }

    companion object {
        fun provideFactory(repository: ClassificationRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ClassificationViewModel(repository) as T
                }
            }
    }
}