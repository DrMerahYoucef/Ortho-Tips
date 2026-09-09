package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.SurgeryEntity
import com.example.data.repository.PatientRepository
import com.example.data.repository.SuggestionField
import com.example.data.repository.SuggestionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SurgeryFormState(
    val surgeryDate: Long = System.currentTimeMillis(),
    val hospitalClinic: String = "",
    val surgeon: String = "",
    val assistants: String = "",
    val anesthesiaType: String = "",
    val diagnosis: String = "",
    val procedure: String = "",
    val materialsUsed: String = "",
    val complications: String = "",
    val postOpInstructions: String = "",
    val operativeProtocol: String = ""
)

data class SurgeryEditUiState(
    val form: SurgeryFormState = SurgeryFormState(),
    val procedureError: String? = null,
    val isSaving: Boolean = false,
    val savedSurgeryId: String? = null
)

/** Every surgery field wired to smart suggestions, in the order the form learns from on save. */
private val SUGGESTION_FIELDS = listOf(
    SuggestionField.SURGERY_HOSPITAL,
    SuggestionField.SURGERY_SURGEON,
    SuggestionField.SURGERY_ASSISTANTS,
    SuggestionField.SURGERY_ANESTHESIA,
    SuggestionField.SURGERY_DIAGNOSIS,
    SuggestionField.SURGERY_PROCEDURE,
    SuggestionField.SURGERY_MATERIALS,
    SuggestionField.SURGERY_COMPLICATIONS,
    SuggestionField.SURGERY_POSTOP,
    SuggestionField.SURGERY_PROTOCOL
)

class SurgeryEditViewModel(
    private val repository: PatientRepository,
    private val suggestionRepository: SuggestionRepository,
    private val patientId: String,
    private val existingSurgery: SurgeryEntity?
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SurgeryEditUiState(
            form = existingSurgery?.let {
                SurgeryFormState(
                    surgeryDate = it.surgeryDate,
                    hospitalClinic = it.hospitalClinic,
                    surgeon = it.surgeon,
                    assistants = it.assistants,
                    anesthesiaType = it.anesthesiaType,
                    diagnosis = it.diagnosis,
                    procedure = it.procedure,
                    materialsUsed = it.materialsUsed,
                    complications = it.complications,
                    postOpInstructions = it.postOpInstructions,
                    operativeProtocol = it.operativeProtocol
                )
            } ?: SurgeryFormState()
        )
    )
    val uiState: StateFlow<SurgeryEditUiState> = _uiState.asStateFlow()

    val suggestionsEnabled: StateFlow<Boolean> = suggestionRepository.suggestionsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    /** fieldKey -> ranked list of previously-typed values, for every smart field on this form. */
    val suggestions: StateFlow<Map<String, List<String>>> = combine(
        SUGGESTION_FIELDS.map { key -> suggestionRepository.suggestionsFor(key) }
    ) { lists ->
        SUGGESTION_FIELDS.zip(lists).toMap()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun update(transform: (SurgeryFormState) -> SurgeryFormState) {
        _uiState.value = _uiState.value.copy(form = transform(_uiState.value.form), procedureError = null)
    }

    fun setSuggestionsEnabled(enabled: Boolean) {
        viewModelScope.launch { suggestionRepository.setEnabled(enabled) }
    }

    fun save() {
        val form = _uiState.value.form
        if (form.procedure.isBlank()) {
            _uiState.value = _uiState.value.copy(procedureError = "Procedure is required")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val id = existingSurgery?.id ?: java.util.UUID.randomUUID().toString()
            repository.saveSurgery(
                SurgeryEntity(
                    id = id,
                    patientId = patientId,
                    surgeryDate = form.surgeryDate,
                    hospitalClinic = form.hospitalClinic,
                    surgeon = form.surgeon,
                    assistants = form.assistants,
                    anesthesiaType = form.anesthesiaType,
                    diagnosis = form.diagnosis,
                    procedure = form.procedure.trim(),
                    materialsUsed = form.materialsUsed,
                    complications = form.complications,
                    postOpInstructions = form.postOpInstructions,
                    operativeProtocol = form.operativeProtocol,
                    createdAt = existingSurgery?.createdAt ?: System.currentTimeMillis()
                )
            )
            learnFromForm(form)
            _uiState.value = _uiState.value.copy(isSaving = false, savedSurgeryId = id)
        }
    }

    private suspend fun learnFromForm(form: SurgeryFormState) {
        suggestionRepository.learn(SuggestionField.SURGERY_HOSPITAL, form.hospitalClinic)
        suggestionRepository.learn(SuggestionField.SURGERY_SURGEON, form.surgeon)
        suggestionRepository.learn(SuggestionField.SURGERY_ASSISTANTS, form.assistants)
        suggestionRepository.learn(SuggestionField.SURGERY_ANESTHESIA, form.anesthesiaType)
        suggestionRepository.learn(SuggestionField.SURGERY_DIAGNOSIS, form.diagnosis)
        suggestionRepository.learn(SuggestionField.SURGERY_PROCEDURE, form.procedure)
        suggestionRepository.learn(SuggestionField.SURGERY_MATERIALS, form.materialsUsed)
        suggestionRepository.learn(SuggestionField.SURGERY_COMPLICATIONS, form.complications)
        suggestionRepository.learn(SuggestionField.SURGERY_POSTOP, form.postOpInstructions)
        suggestionRepository.learn(SuggestionField.SURGERY_PROTOCOL, form.operativeProtocol)
    }

    companion object {
        fun provideFactory(
            repository: PatientRepository,
            suggestionRepository: SuggestionRepository,
            patientId: String,
            existingSurgery: SurgeryEntity?
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SurgeryEditViewModel(repository, suggestionRepository, patientId, existingSurgery) as T
            }
        }
    }
}
