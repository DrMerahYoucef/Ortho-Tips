package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.model.PatientSummary
import com.example.data.repository.PatientRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PatientListTab { ACTIVE, ARCHIVED }

data class PatientListUiState(
    val patients: List<PatientSummary> = emptyList(),
    val tab: PatientListTab = PatientListTab.ACTIVE,
    val searchQuery: String = "",
    val regionFilter: String = "All",
    val selectedIds: Set<String> = emptySet(),
    val activeCount: Int = 0,
    val archivedCount: Int = 0,
    val isLoading: Boolean = true
) {
    val isSelectionMode: Boolean get() = selectedIds.isNotEmpty()

    val filteredPatients: List<PatientSummary> get() {
        if (regionFilter == "All") return patients
        return patients.filter { patient ->
            patient.bodyRegion.contains(regionFilter, ignoreCase = true) ||
            patient.diagnosis.contains(regionFilter, ignoreCase = true)
        }
    }

    val isEmpty: Boolean get() = !isLoading && filteredPatients.isEmpty()
}

class PatientListViewModel(
    private val repository: PatientRepository
) : ViewModel() {

    private val _tab = MutableStateFlow(PatientListTab.ACTIVE)
    private val _searchQuery = MutableStateFlow("")
    private val _regionFilter = MutableStateFlow("All")
    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())

    val uiState: StateFlow<PatientListUiState> = combine(
        repository.getActivePatientSummaries(),
        repository.getArchivedPatientSummaries()
    ) { active, archived -> Pair(active, archived) }
        .combine(_tab) { (active, archived), tab -> Triple(active, archived, tab) }
        .combine(_searchQuery) { (active, archived, tab), query -> Tuple4(active, archived, tab, query) }
        .combine(_regionFilter) { (active, archived, tab, query), region -> Tuple5(active, archived, tab, query, region) }
        .combine(_selectedIds) { (active, archived, tab, query, region), selected ->
            val baseList = if (tab == PatientListTab.ACTIVE) active else archived
            val searchedList = if (query.isNotBlank()) {
                baseList.filter { patient ->
                    patient.fullName.contains(query, ignoreCase = true) ||
                    patient.patientCode.contains(query, ignoreCase = true) ||
                    patient.diagnosis.contains(query, ignoreCase = true) ||
                    patient.bodyRegion.contains(query, ignoreCase = true)
                }
            } else {
                baseList
            }

            PatientListUiState(
                patients = searchedList,
                tab = tab,
                searchQuery = query,
                regionFilter = region,
                selectedIds = selected,
                activeCount = active.size,
                archivedCount = archived.size,
                isLoading = false
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PatientListUiState(isLoading = true)
        )

    fun setTab(tab: PatientListTab) {
        _tab.value = tab
        _selectedIds.value = emptySet()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setRegionFilter(region: String) {
        _regionFilter.value = region
    }

    fun toggleSelection(patientId: String) {
        _selectedIds.value = _selectedIds.value.let { current ->
            if (current.contains(patientId)) current - patientId else current + patientId
        }
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun archiveSelected() = setArchivedForSelected(true)

    fun unarchiveSelected() = setArchivedForSelected(false)

    private fun setArchivedForSelected(archived: Boolean) {
        val ids = _selectedIds.value
        viewModelScope.launch {
            ids.forEach { repository.setArchived(it, archived) }
            _selectedIds.value = emptySet()
        }
    }

    fun deleteSelected() {
        val ids = _selectedIds.value.toList()
        viewModelScope.launch {
            repository.deletePatients(ids)
            _selectedIds.value = emptySet()
        }
    }

    companion object {
        fun provideFactory(repository: PatientRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PatientListViewModel(repository) as T
                }
            }
    }
}

private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
private data class Tuple5<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)
