package com.example.data.repository

import com.example.data.local.OrthoDatabase
import com.example.data.local.entity.FieldSuggestionEntity
import com.example.data.prefs.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Namespaced keys for every "smart" field across the patient & surgery forms. */
object SuggestionField {
    // Patient — medical history
    const val MEDICAL_HISTORY = "patient.medicalHistory"
    const val ALLERGIES = "patient.allergies"
    const val CHRONIC_DISEASES = "patient.chronicDiseases"
    const val PREVIOUS_SURGERIES = "patient.previousSurgeries"
    const val MEDICATIONS = "patient.medications"

    // Patient — orthopedic information
    const val DIAGNOSIS = "patient.diagnosis"
    const val BODY_REGION = "patient.bodyRegion"
    const val TRAUMA_MECHANISM = "patient.traumaMechanism"
    const val FRACTURE_CLASSIFICATION = "patient.fractureClassification"
    const val IMAGING_FINDINGS = "patient.imagingFindings"
    const val TREATMENT_PLAN = "patient.treatmentPlan"

    // Surgery record
    const val SURGERY_HOSPITAL = "surgery.hospital"
    const val SURGERY_SURGEON = "surgery.surgeon"
    const val SURGERY_ASSISTANTS = "surgery.assistants"
    const val SURGERY_ANESTHESIA = "surgery.anesthesia"
    const val SURGERY_DIAGNOSIS = "surgery.diagnosis"
    const val SURGERY_PROCEDURE = "surgery.procedure"
    const val SURGERY_MATERIALS = "surgery.materials"
    const val SURGERY_COMPLICATIONS = "surgery.complications"
    const val SURGERY_POSTOP = "surgery.postOp"
}

/**
 * Backs the "smart suggestions" input fields: reads/learns free-text values the
 * surgeon has typed before, scoped per field, and respects the global on/off toggle
 * in [SettingsRepository] both when suggesting *and* when learning (turning it off
 * stops the app from remembering anything new, on top of hiding the dropdown).
 */
class SuggestionRepository(
    private val database: OrthoDatabase,
    private val settingsRepository: SettingsRepository
) {
    private val dao = database.suggestionDao()

    val suggestionsEnabled: Flow<Boolean> =
        settingsRepository.settingsFlow.map { it.smartSuggestionsEnabled }

    fun suggestionsFor(fieldKey: String): Flow<List<String>> =
        dao.getSuggestions(fieldKey).map { list -> list.map { it.value } }.flowOn(Dispatchers.IO)

    suspend fun setEnabled(enabled: Boolean) {
        settingsRepository.setSmartSuggestions(enabled)
    }

    /**
     * Learns every discrete item in [rawValue] (split on commas/semicolons/newlines,
     * so "Penicillin, Latex" is remembered as two separate suggestions) under
     * [fieldKey]. No-ops silently when the smart-suggestions toggle is off.
     */
    suspend fun learn(fieldKey: String, rawValue: String) = withContext(Dispatchers.IO) {
        if (!settingsRepository.settingsFlow.first().smartSuggestionsEnabled) return@withContext

        rawValue.split(",", ";", "\n")
            .map { it.trim() }
            .filter { it.length in 2..80 }
            .distinct()
            .forEach { item ->
                val existingCount = dao.getUseCount(fieldKey, item) ?: 0
                dao.upsert(
                    FieldSuggestionEntity(
                        fieldKey = fieldKey,
                        value = item,
                        useCount = existingCount + 1,
                        lastUsedAt = System.currentTimeMillis()
                    )
                )
            }
    }

    suspend fun forget(fieldKey: String, value: String) = withContext(Dispatchers.IO) {
        dao.delete(fieldKey, value)
    }
}
