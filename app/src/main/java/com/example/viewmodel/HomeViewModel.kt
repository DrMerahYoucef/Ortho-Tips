package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.model.BoneSummary
import com.example.data.local.model.ChapterWithCount
import com.example.data.repository.OrthoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val chapters: List<ChapterWithCount> = emptyList(),
    val highYieldBones: List<BoneSummary> = emptyList(),
    val totalProceduresCount: Int = 0,
    val bookmarkedCount: Int = 0,
    val noteCount: Int = 0,
    val isLoading: Boolean = false
)

class HomeViewModel(
    private val repository: OrthoRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        repository.getAllChaptersWithCounts(),
        repository.getHighYieldBones(),
        repository.getBookmarkedBones(),
        repository.getAllNotes()
    ) { chapters, highYield, bookmarks, notes ->
        val total = chapters.sumOf { it.boneCount }
        HomeUiState(
            chapters = chapters,
            highYieldBones = highYield,
            totalProceduresCount = total,
            bookmarkedCount = bookmarks.size,
            noteCount = notes.size,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    fun toggleBookmark(boneId: String) {
        viewModelScope.launch {
            repository.toggleBookmark(boneId)
        }
    }

    companion object {
        fun provideFactory(repository: OrthoRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(repository) as T
                }
            }
    }
}
