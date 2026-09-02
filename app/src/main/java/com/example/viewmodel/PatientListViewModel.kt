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
    val selectedIds: Set<String> = emptySet(),
    val isLoading: Boolean = true
) {
    val isSelectionMode: Boolean get() = selectedIds.isNotEmpty()
    // Only ever true once loading has settled and there is genuinely no patient data —
    // this app never falls back to sample/demo patients.
    val isEmpty: Boolean get() = !isLoading && patients.isEmpty()
}

class PatientListViewModel(
    private val repository: PatientRepository
) : ViewModel() {

    private val _tab = MutableStateFlow(PatientListTab.ACTIVE)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())

    val uiState: StateFlow<PatientListUiState> = combine(
        _tab,
        _searchQuery,
        _selectedIds
    ) { tab, query, selected -> Triple(tab, query, selected) }
        .flatMapLatest { (tab, query, selected) ->
            val source = when {
                query.isNotBlank() -> repository.searchPatientSummaries(query)
                tab == PatientListTab.ACTIVE -> repository.getActivePatientSummaries()
                else -> repository.getArchivedPatientSummaries()
            }
            source.map { patients ->
                PatientListUiState(
                    patients = if (query.isNotBlank() && tab == PatientListTab.ARCHIVED) {
                        patients.filter { it.isArchived }
                    } else if (query.isNotBlank()) {
                        patients.filter { !it.isArchived }
                    } else {
                        patients
                    },
                    tab = tab,
                    searchQuery = query,
                    selectedIds = selected,
                    isLoading = false
                )
            }
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
