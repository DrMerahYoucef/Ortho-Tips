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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ClinicalStudiesTab(@androidx.annotation.StringRes val labelRes: Int) {
    OPERATIVE_GUIDES(com.example.R.string.home_tab_operative_guides),
    CLASSIFICATIONS(com.example.R.string.home_tab_classifications)
}

data class HomeUiState(
    val chapters: List<ChapterWithCount> = emptyList(),
    val highYieldBones: List<BoneSummary> = emptyList(),
    val totalProceduresCount: Int = 0,
    val bookmarkedCount: Int = 0,
    val noteCount: Int = 0,
    val activeTab: ClinicalStudiesTab = ClinicalStudiesTab.OPERATIVE_GUIDES,
    val isLoading: Boolean = false
) {
    val isDatabaseEmpty: Boolean get() = !isLoading && chapters.isEmpty()
}

class HomeViewModel(
    private val repository: OrthoRepository
) : ViewModel() {

    private val _activeTab = MutableStateFlow(ClinicalStudiesTab.OPERATIVE_GUIDES)

    val uiState: StateFlow<HomeUiState> = combine(
        repository.getAllChaptersWithCounts(),
        repository.getHighYieldBones(),
        repository.getBookmarkedBones(),
        repository.getAllNotes(),
        _activeTab
    ) { chapters, highYield, bookmarks, notes, tab ->
        HomeUiState(
            chapters = chapters,
            highYieldBones = highYield,
            totalProceduresCount = chapters.sumOf { it.boneCount },
            bookmarkedCount = bookmarks.size,
            noteCount = notes.size,
            activeTab = tab,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    fun setActiveTab(tab: ClinicalStudiesTab) {
        _activeTab.value = tab
    }

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
