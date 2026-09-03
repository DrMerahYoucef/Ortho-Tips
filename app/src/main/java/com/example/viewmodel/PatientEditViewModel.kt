package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.BodySide
import com.example.data.local.entity.PatientEntity
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

/** Mirrors [PatientEntity]'s editable fields as plain strings for two-way text field binding. */
data class PatientFormState(
    val patientCode: String = "",
    val fullName: String = "",
    val dateOfBirth: Long? = null,
    val gender: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = "",
    val insuranceProvider: String = "",
    val insuranceNumber: String = "",
    val medicalHistory: String = "",
    val allergies: String = "",
    val chronicDiseases: String = "",
    val previousSurgeries: String = "",
    val medications: String = "",
    val generalNotes: String = "",
    val diagnosis: String = "",
    val bodyRegion: String = "",
    val side: BodySide = BodySide.NOT_APPLICABLE,
    val injuryDate: Long? = null,
    val traumaMechanism: String = "",
    val fractureClassification: String = "",
    val imagingFindings: String = "",
    val treatmentPlan: String = ""
)

data class PatientEditUiState(
    val form: PatientFormState = PatientFormState(),
    val isEditMode: Boolean = false,
    val isLoading: Boolean = true,
    val nameError: String? = null,
    val patientCodeError: String? = null,
    val isSaving: Boolean = false,
    val savedPatientId: String? = null
)

/** Every patient field wired to smart suggestions, in the order the form learns from on save. */
private val SUGGESTION_FIELDS = listOf(
    SuggestionField.MEDICAL_HISTORY,
    SuggestionField.ALLERGIES,
    SuggestionField.CHRONIC_DISEASES,
    SuggestionField.PREVIOUS_SURGERIES,
    SuggestionField.MEDICATIONS,
    SuggestionField.DIAGNOSIS,
    SuggestionField.BODY_REGION,
    SuggestionField.TRAUMA_MECHANISM,
    SuggestionField.FRACTURE_CLASSIFICATION,
    SuggestionField.IMAGING_FINDINGS,
    SuggestionField.TREATMENT_PLAN
)

class PatientEditViewModel(
    private val repository: PatientRepository,
    private val suggestionRepository: SuggestionRepository,
    private val existingPatientId: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow(PatientEditUiState(isEditMode = existingPatientId != null))
    val uiState: StateFlow<PatientEditUiState> = _uiState.asStateFlow()

    val suggestionsEnabled: StateFlow<Boolean> = suggestionRepository.suggestionsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    /** fieldKey -> ranked list of previously-typed values, for every smart field on this form. */
    val suggestions: StateFlow<Map<String, List<String>>> = combine(
        SUGGESTION_FIELDS.map { key -> suggestionRepository.suggestionsFor(key) }
    ) { lists ->
        SUGGESTION_FIELDS.zip(lists).toMap()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        if (existingPatientId != null) {
            viewModelScope.launch {
                val patient = repository.getPatient(existingPatientId)
                _uiState.value = _uiState.value.copy(
                    form = patient?.toFormState() ?: PatientFormState(),
                    isLoading = false
                )
            }
        } else {
            viewModelScope.launch {
                val suggested = "P-" + (repository.getPatientCount() + 1).toString().padStart(4, '0')
                _uiState.value = _uiState.value.copy(
                    form = PatientFormState(patientCode = suggested),
                    isLoading = false
                )
            }
        }
    }

    fun update(transform: (PatientFormState) -> PatientFormState) {
        _uiState.value = _uiState.value.copy(
            form = transform(_uiState.value.form),
            nameError = null,
            patientCodeError = null
        )
    }

    fun setSuggestionsEnabled(enabled: Boolean) {
        viewModelScope.launch { suggestionRepository.setEnabled(enabled) }
    }

    fun save() {
        val state = _uiState.value
        val form = state.form

        if (form.fullName.isBlank()) {
            _uiState.value = state.copy(nameError = "Full name is required")
            return
        }
        if (form.patientCode.isBlank()) {
            _uiState.value = state.copy(patientCodeError = "Patient ID is required")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)

            val codeAvailable = repository.isPatientCodeAvailable(
                form.patientCode,
                excludePatientId = existingPatientId ?: ""
            )
            if (!codeAvailable) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    patientCodeError = "This patient ID is already in use"
                )
                return@launch
            }

            val id = existingPatientId ?: java.util.UUID.randomUUID().toString()
            val existing = existingPatientId?.let { repository.getPatient(it) }
            val entity = form.toEntity(id = id, createdAt = existing?.createdAt, isArchived = existing?.isArchived ?: false)

            repository.savePatient(entity)
            learnFromForm(form)
            _uiState.value = _uiState.value.copy(isSaving = false, savedPatientId = id)
        }
    }

    private suspend fun learnFromForm(form: PatientFormState) {
        suggestionRepository.learn(SuggestionField.MEDICAL_HISTORY, form.medicalHistory)
        suggestionRepository.learn(SuggestionField.ALLERGIES, form.allergies)
        suggestionRepository.learn(SuggestionField.CHRONIC_DISEASES, form.chronicDiseases)
        suggestionRepository.learn(SuggestionField.PREVIOUS_SURGERIES, form.previousSurgeries)
        suggestionRepository.learn(SuggestionField.MEDICATIONS, form.medications)
        suggestionRepository.learn(SuggestionField.DIAGNOSIS, form.diagnosis)
        suggestionRepository.learn(SuggestionField.BODY_REGION, form.bodyRegion)
        suggestionRepository.learn(SuggestionField.TRAUMA_MECHANISM, form.traumaMechanism)
        suggestionRepository.learn(SuggestionField.FRACTURE_CLASSIFICATION, form.fractureClassification)
        suggestionRepository.learn(SuggestionField.IMAGING_FINDINGS, form.imagingFindings)
        suggestionRepository.learn(SuggestionField.TREATMENT_PLAN, form.treatmentPlan)
    }

    companion object {
        fun provideFactory(
            repository: PatientRepository,
            suggestionRepository: SuggestionRepository,
            existingPatientId: String?
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PatientEditViewModel(repository, suggestionRepository, existingPatientId) as T
            }
        }
    }
}

private fun PatientEntity.toFormState() = PatientFormState(
    patientCode = patientCode,
    fullName = fullName,
    dateOfBirth = dateOfBirth,
    gender = gender,
    phone = phone,
    email = email,
    address = address,
    emergencyContactName = emergencyContactName,
    emergencyContactPhone = emergencyContactPhone,
    insuranceProvider = insuranceProvider,
    insuranceNumber = insuranceNumber,
    medicalHistory = medicalHistory,
    allergies = allergies,
    chronicDiseases = chronicDiseases,
    previousSurgeries = previousSurgeries,
    medications = medications,
    generalNotes = generalNotes,
    diagnosis = diagnosis,
    bodyRegion = bodyRegion,
    side = try { BodySide.valueOf(side) } catch (_: Exception) { BodySide.NOT_APPLICABLE },
    injuryDate = injuryDate,
    traumaMechanism = traumaMechanism,
    fractureClassification = fractureClassification,
    imagingFindings = imagingFindings,
    treatmentPlan = treatmentPlan
)

private fun PatientFormState.toEntity(id: String, createdAt: Long?, isArchived: Boolean) = PatientEntity(
    id = id,
    patientCode = patientCode.trim(),
    fullName = fullName.trim(),
    dateOfBirth = dateOfBirth,
    gender = gender,
    phone = phone,
    email = email,
    address = address,
    emergencyContactName = emergencyContactName,
    emergencyContactPhone = emergencyContactPhone,
    insuranceProvider = insuranceProvider,
    insuranceNumber = insuranceNumber,
    medicalHistory = medicalHistory,
    allergies = allergies,
    chronicDiseases = chronicDiseases,
    previousSurgeries = previousSurgeries,
    medications = medications,
    generalNotes = generalNotes,
    diagnosis = diagnosis,
    bodyRegion = bodyRegion,
    side = side.name,
    injuryDate = injuryDate,
    traumaMechanism = traumaMechanism,
    fractureClassification = fractureClassification,
    imagingFindings = imagingFindings,
    treatmentPlan = treatmentPlan,
    isArchived = isArchived,
    createdAt = createdAt ?: System.currentTimeMillis(),
    updatedAt = System.currentTimeMillis()
)
