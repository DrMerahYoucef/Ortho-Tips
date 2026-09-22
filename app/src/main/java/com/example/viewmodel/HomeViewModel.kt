package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.extensions.ExtensionKinds
import com.example.data.extensions.ExtensionRepository
import com.example.data.local.model.BoneSummary
import com.example.data.local.model.ChapterWithCount
import com.example.data.repository.OrthoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The sub-tabs of the Extensions area (bottom navigation). A tab with a
 * [requiredKind] is shown only while an extension of that kind is installed.
 */
enum class ExtensionsHubTab(
    @androidx.annotation.StringRes val labelRes: Int,
    val requiredKind: String?
) {
    OPERATIVE_GUIDES(com.example.R.string.home_tab_operative_guides, ExtensionKinds.OPERATIVE_GUIDE),
    CLASSIFICATIONS(com.example.R.string.home_tab_classifications, ExtensionKinds.CLASSIFICATIONS),

    /** Every installed extension: open, details, export, delete. */
    MANAGE(com.example.R.string.ext_hub_tab_manage, null)
}

data class HomeUiState(
    val chapters: List<ChapterWithCount> = emptyList(),
    val highYieldBones: List<BoneSummary> = emptyList(),
    val totalProceduresCount: Int = 0,
    val bookmarkedCount: Int = 0,
    val noteCount: Int = 0,
    /** The sub-tabs available with the extensions currently installed. */
    val tabs: List<ExtensionsHubTab> = ExtensionsHubTab.entries,
    val activeTab: ExtensionsHubTab = ExtensionsHubTab.OPERATIVE_GUIDES,
    val isLoading: Boolean = false
) {
    /** Installed but not yet filled: the guide's tab offers to import its data. */
    val isDatabaseEmpty: Boolean get() = !isLoading && chapters.isEmpty()
}

class HomeViewModel(
    private val repository: OrthoRepository,
    extensionRepository: ExtensionRepository
) : ViewModel() {

    private val _activeTab = MutableStateFlow(ExtensionsHubTab.OPERATIVE_GUIDES)

    private data class GuideContent(
        val chapters: List<ChapterWithCount>,
        val highYield: List<BoneSummary>,
        val bookmarkedCount: Int,
        val noteCount: Int
    )

    private val guideContent = combine(
        repository.getAllChaptersWithCounts(),
        repository.getHighYieldBones(),
        repository.getBookmarkedBones(),
        repository.getAllNotes()
    ) { chapters, highYield, bookmarks, notes ->
        GuideContent(chapters, highYield, bookmarks.size, notes.size)
    }

    private val installedKinds = extensionRepository.extensions
        .map { list -> list.map { it.kind }.toSet() }
        .distinctUntilChanged()

    val uiState: StateFlow<HomeUiState> = combine(guideContent, installedKinds, _activeTab) { content, kinds, selected ->
        val tabs = ExtensionsHubTab.entries.filter { it.requiredKind == null || it.requiredKind in kinds }
        HomeUiState(
            chapters = content.chapters,
            highYieldBones = content.highYield,
            totalProceduresCount = content.chapters.sumOf { it.boneCount },
            bookmarkedCount = content.bookmarkedCount,
            noteCount = content.noteCount,
            tabs = tabs,
            // A tab whose extension was just removed falls back to the first one left.
            activeTab = selected.takeIf { it in tabs } ?: tabs.first(),
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    fun setActiveTab(tab: ExtensionsHubTab) {
        _activeTab.value = tab
    }

    fun toggleBookmark(boneId: String) {
        viewModelScope.launch {
            repository.toggleBookmark(boneId)
        }
    }

    companion object {
        fun provideFactory(
            repository: OrthoRepository,
            extensionRepository: ExtensionRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(repository, extensionRepository) as T
                }
            }
    }
}
