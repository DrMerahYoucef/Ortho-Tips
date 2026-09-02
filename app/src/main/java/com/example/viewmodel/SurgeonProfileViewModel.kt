package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.prefs.SurgeonProfile
import com.example.data.prefs.SurgeonProfileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SurgeonProfileViewModel(
    private val repository: SurgeonProfileRepository
) : ViewModel() {

    val profile: StateFlow<SurgeonProfile> = repository.profileFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SurgeonProfile()
    )

    fun save(profile: SurgeonProfile) {
        viewModelScope.launch { repository.saveProfile(profile) }
    }

    companion object {
        fun provideFactory(repository: SurgeonProfileRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SurgeonProfileViewModel(repository) as T
                }
            }
    }
}
