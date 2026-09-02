package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.SurgeryEntity
import com.example.data.repository.PatientRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val postOpInstructions: String = ""
)

data class SurgeryEditUiState(
    val form: SurgeryFormState = SurgeryFormState(),
    val procedureError: String? = null,
    val isSaving: Boolean = false,
    val savedSurgeryId: String? = null
)

class SurgeryEditViewModel(
    private val repository: PatientRepository,
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
                    postOpInstructions = it.postOpInstructions
                )
            } ?: SurgeryFormState()
        )
    )
    val uiState: StateFlow<SurgeryEditUiState> = _uiState.asStateFlow()

    fun update(transform: (SurgeryFormState) -> SurgeryFormState) {
        _uiState.value = _uiState.value.copy(form = transform(_uiState.value.form), procedureError = null)
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
                    createdAt = existingSurgery?.createdAt ?: System.currentTimeMillis()
                )
            )
            _uiState.value = _uiState.value.copy(isSaving = false, savedSurgeryId = id)
        }
    }

    companion object {
        fun provideFactory(
            repository: PatientRepository,
            patientId: String,
            existingSurgery: SurgeryEntity?
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SurgeryEditViewModel(repository, patientId, existingSurgery) as T
            }
        }
    }
}
