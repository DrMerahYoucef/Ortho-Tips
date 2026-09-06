package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ClinicalStudiesData
import com.example.data.local.model.BoneSummary
import com.example.data.local.model.ChapterWithCount
import com.example.data.local.model.ClinicalClassification
import com.example.data.local.model.ClinicalStudy
import com.example.data.repository.OrthoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ClinicalStudiesTab(val label: String) {
    OPERATIVE_GUIDES("Operative Guides"),
    LANDMARK_TRIALS("Landmark Trials"),
    CLASSIFICATIONS("Classifications")
}

data class HomeUiState(
    val chapters: List<ChapterWithCount> = emptyList(),
    val highYieldBones: List<BoneSummary> = emptyList(),
    val totalProceduresCount: Int = 0,
    val bookmarkedCount: Int = 0,
    val noteCount: Int = 0,
    val landmarkStudies: List<ClinicalStudy> = ClinicalStudiesData.landmarkStudies,
    val classifications: List<ClinicalClassification> = ClinicalStudiesData.clinicalClassifications,
    val activeTab: ClinicalStudiesTab = ClinicalStudiesTab.OPERATIVE_GUIDES,
    val specialtyFilter: String = "All",
    val searchQuery: String = "",
    val isLoading: Boolean = false
) {
    val isDatabaseEmpty: Boolean get() = !isLoading && chapters.isEmpty()

    val filteredLandmarkStudies: List<ClinicalStudy> get() {
        return landmarkStudies.filter { study ->
            val matchesSpecialty = specialtyFilter == "All" || study.specialty.equals(specialtyFilter, ignoreCase = true)
            val matchesQuery = searchQuery.isBlank() ||
                study.shortName.contains(searchQuery, ignoreCase = true) ||
                study.title.contains(searchQuery, ignoreCase = true) ||
                study.clinicalQuestion.contains(searchQuery, ignoreCase = true) ||
                study.keyFindings.contains(searchQuery, ignoreCase = true)
            matchesSpecialty && matchesQuery
        }
    }

    val filteredClassifications: List<ClinicalClassification> get() {
        return classifications.filter { item ->
            val matchesQuery = searchQuery.isBlank() ||
                item.name.contains(searchQuery, ignoreCase = true) ||
                item.anatomicRegion.contains(searchQuery, ignoreCase = true) ||
                item.description.contains(searchQuery, ignoreCase = true) ||
                item.highYieldPearl.contains(searchQuery, ignoreCase = true)
            matchesQuery
        }
    }
}

class HomeViewModel(
    private val repository: OrthoRepository
) : ViewModel() {

    private val _activeTab = MutableStateFlow(ClinicalStudiesTab.OPERATIVE_GUIDES)
    private val _specialtyFilter = MutableStateFlow("All")
    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<HomeUiState> = combine(
        repository.getAllChaptersWithCounts(),
        repository.getHighYieldBones(),
        repository.getBookmarkedBones(),
        repository.getAllNotes()
    ) { chapters, highYield, bookmarks, notes ->
        HomeRepoData(chapters, highYield, bookmarks.size, notes.size)
    }.combine(_activeTab) { data, tab ->
        Pair(data, tab)
    }.combine(_specialtyFilter) { (data, tab), specialty ->
        Triple(data, tab, specialty)
    }.combine(_searchQuery) { (data, tab, specialty), query ->
        val total = data.chapters.sumOf { it.boneCount }
        HomeUiState(
            chapters = data.chapters,
            highYieldBones = data.highYield,
            totalProceduresCount = total,
            bookmarkedCount = data.bookmarkCount,
            noteCount = data.noteCount,
            landmarkStudies = ClinicalStudiesData.landmarkStudies,
            classifications = ClinicalStudiesData.clinicalClassifications,
            activeTab = tab,
            specialtyFilter = specialty,
            searchQuery = query,
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

    fun setSpecialtyFilter(specialty: String) {
        _specialtyFilter.value = specialty
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
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

private data class HomeRepoData(
    val chapters: List<ChapterWithCount>,
    val highYield: List<BoneSummary>,
    val bookmarkCount: Int,
    val noteCount: Int
)
