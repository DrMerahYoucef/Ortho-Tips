package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.TimelineEventEntity
import com.example.data.repository.PatientRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TimelineEventFormState(
    val eventDate: Long = System.currentTimeMillis(),
    val title: String = "",
    val description: String = ""
)

data class TimelineEventEditUiState(
    val form: TimelineEventFormState = TimelineEventFormState(),
    val titleError: String? = null,
    val isSaving: Boolean = false,
    val savedEventId: String? = null
)

class TimelineEventEditViewModel(
    private val repository: PatientRepository,
    private val patientId: String,
    private val existingEvent: TimelineEventEntity?
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TimelineEventEditUiState(
            form = existingEvent?.let {
                TimelineEventFormState(eventDate = it.eventDate, title = it.title, description = it.description)
            } ?: TimelineEventFormState()
        )
    )
    val uiState: StateFlow<TimelineEventEditUiState> = _uiState.asStateFlow()

    fun update(transform: (TimelineEventFormState) -> TimelineEventFormState) {
        _uiState.value = _uiState.value.copy(form = transform(_uiState.value.form), titleError = null)
    }

    fun save() {
        val form = _uiState.value.form
        if (form.title.isBlank()) {
            _uiState.value = _uiState.value.copy(titleError = "Title is required")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val id = existingEvent?.id ?: java.util.UUID.randomUUID().toString()
            repository.saveTimelineEvent(
                TimelineEventEntity(
                    id = id,
                    patientId = patientId,
                    eventDate = form.eventDate,
                    title = form.title.trim(),
                    description = form.description,
                    createdAt = existingEvent?.createdAt ?: System.currentTimeMillis()
                )
            )
            _uiState.value = _uiState.value.copy(isSaving = false, savedEventId = id)
        }
    }

    companion object {
        fun provideFactory(
            repository: PatientRepository,
            patientId: String,
            existingEvent: TimelineEventEntity?
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TimelineEventEditViewModel(repository, patientId, existingEvent) as T
            }
        }
    }
}
