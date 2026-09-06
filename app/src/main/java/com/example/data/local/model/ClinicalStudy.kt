package com.example.data.local.model

data class ClinicalStudy(
    val id: String,
    val shortName: String,
    val title: String,
    val specialty: String, // Trauma, Arthroplasty, Sports, Upper Extremity, Foot & Ankle, Spine
    val journalYear: String,
    val evidenceLevel: String, // Level I RCT, Level II, Meta-Analysis
    val patientCount: String,
    val clinicalQuestion: String,
    val studyDesign: String,
    val primaryOutcome: String,
    val keyFindings: String,
    val operativeImplication: String,
    val clinicalPearls: List<String>
)

data class ClassificationGrade(
    val grade: String,
    val criteria: String,
    val managementTakeaway: String
)

data class ClinicalClassification(
    val id: String,
    val name: String,
    val anatomicRegion: String,
    val description: String,
    val grades: List<ClassificationGrade>,
    val highYieldPearl: String
)
