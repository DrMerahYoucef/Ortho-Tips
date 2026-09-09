package com.example.ui.screens.patients

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.model.PatientDetail
import com.example.data.local.model.Cim10Disease
import com.example.data.repository.Cim10Repository
import com.example.ui.components.EmptyStateView
import com.example.ui.components.Cim10DiseaseText
import com.example.ui.components.RegionBadge
import com.example.ui.components.SurgeryRow
import com.example.ui.components.TimelineEventRow
import com.example.utils.DateFormat
import com.example.viewmodel.PatientDetailViewModel

private enum class DetailTab { INFO, TIMELINE, SURGERIES, MEDIA }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailScreen(
    viewModel: PatientDetailViewModel,
    cim10Repository: Cim10Repository,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: () -> Unit,
    onNavigateToAddTimelineEvent: () -> Unit,
    onNavigateToEditTimelineEvent: (String) -> Unit,
    onNavigateToAddSurgery: () -> Unit,
    onNavigateToEditSurgery: (String) -> Unit,
    onNavigateToCapture: () -> Unit,
    onNavigateToAnnotate: (String) -> Unit,
    onGenerateProtocol: (String, com.example.utils.ProtocolLanguage, Boolean, com.example.utils.SurgeryPdfOptions) -> Unit,
    onGenerateChartPdf: (Boolean, com.example.utils.PatientChartPdfOptions) -> Unit,
    savedSurgeryPdfOptions: com.example.utils.SurgeryPdfOptions = com.example.utils.SurgeryPdfOptions(),
    savedChartPdfOptions: com.example.utils.PatientChartPdfOptions = com.example.utils.PatientChartPdfOptions(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(DetailTab.INFO.ordinal) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var protocolSurgeryId by remember { mutableStateOf<String?>(null) }
    var showChartOptions by remember { mutableStateOf(false) }

    if (uiState.wasDeleted && !uiState.isLoading) {
        onNavigateBack()
        return
    }

    val detail = uiState.detail
    val cim10Diseases by androidx.compose.runtime.produceState(emptyList<Cim10Disease>(), cim10Repository) {
        value = cim10Repository.getDiseases()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detail?.patient?.fullName ?: "Patient", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Edit patient") },
                            onClick = { menuExpanded = false; onNavigateToEdit() }
                        )
                        DropdownMenuItem(
                            text = { Text("Export PDF...") },
                            onClick = { menuExpanded = false; showChartOptions = true }
                        )
                        DropdownMenuItem(
                            text = { Text(if (detail?.patient?.isArchived == true) "Unarchive" else "Archive") },
                            onClick = {
                                menuExpanded = false
                                if (detail?.patient?.isArchived == true) viewModel.unarchive() else viewModel.archive()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete patient") },
                            onClick = { menuExpanded = false; showDeleteConfirm = true }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            when (DetailTab.entries[selectedTab]) {
                DetailTab.TIMELINE -> FloatingActionButton(onClick = onNavigateToAddTimelineEvent) {
                    Icon(Icons.Default.Add, contentDescription = "Add event")
                }
                DetailTab.SURGERIES -> FloatingActionButton(onClick = onNavigateToAddSurgery) {
                    Icon(Icons.Default.Add, contentDescription = "Add surgery")
                }
                else -> {}
            }
        },
        modifier = modifier
    ) { paddingValues ->
        if (uiState.isLoading || detail == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(modifier = Modifier.padding(paddingValues)) {
            PatientHeader(detail, cim10Diseases)

            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Info") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Timeline") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Surgeries") })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Media") })
            }

            when (DetailTab.entries[selectedTab]) {
                DetailTab.INFO -> InfoTab(detail, cim10Diseases)
                DetailTab.TIMELINE -> TimelineTab(
                    detail = detail,
                    onEventClick = onNavigateToEditTimelineEvent,
                    onEventDelete = { viewModel.deleteTimelineEvent(it) },
                    cim10Diseases = cim10Diseases
                )
                DetailTab.SURGERIES -> SurgeriesTab(
                    detail = detail,
                    onSurgeryClick = onNavigateToEditSurgery,
                    onSurgeryDelete = { viewModel.deleteSurgery(it) },
                    onGenerateProtocol = { surgeryId -> protocolSurgeryId = surgeryId },
                    cim10Diseases = cim10Diseases
                )
                DetailTab.MEDIA -> MediaTab(
                    detail = detail,
                    viewModel = viewModel,
                    onAddMedia = onNavigateToCapture,
                    onImageClick = onNavigateToAnnotate
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete patient?") },
            text = { Text("This permanently deletes ${detail?.patient?.fullName ?: "this patient"}, their timeline, surgeries, and all attached media. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; viewModel.delete() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    protocolSurgeryId?.let { surgeryId ->
        ProtocolLanguageDialog(
            initialOptions = savedSurgeryPdfOptions,
            onDismiss = { protocolSurgeryId = null },
            onConfirm = { language, anonymize, options ->
                onGenerateProtocol(surgeryId, language, anonymize, options)
                protocolSurgeryId = null
            }
        )
    }

    if (showChartOptions) {
        PatientChartOptionsDialog(
            initialOptions = savedChartPdfOptions,
            onDismiss = { showChartOptions = false },
            onConfirm = { anonymize, options ->
                onGenerateChartPdf(anonymize, options)
                showChartOptions = false
            }
        )
    }
}

@Composable
private fun ProtocolLanguageDialog(
    initialOptions: com.example.utils.SurgeryPdfOptions,
    onDismiss: () -> Unit,
    onConfirm: (com.example.utils.ProtocolLanguage, Boolean, com.example.utils.SurgeryPdfOptions) -> Unit
) {
    var language by remember { mutableStateOf(com.example.utils.ProtocolLanguage.FRENCH) }
    var anonymize by remember { mutableStateOf(false) }
    var includeHospital by remember { mutableStateOf(initialOptions.includeHospital) }
    var includeSurgeon by remember { mutableStateOf(initialOptions.includeSurgeon) }
    var includeAssistants by remember { mutableStateOf(initialOptions.includeAssistants) }
    var includeAnesthesia by remember { mutableStateOf(initialOptions.includeAnesthesia) }
    var includeDiagnosis by remember { mutableStateOf(initialOptions.includeDiagnosis) }
    var includeProcedure by remember { mutableStateOf(initialOptions.includeProcedure) }
    var includeOperativeSteps by remember { mutableStateOf(initialOptions.includeOperativeSteps) }
    var includeMaterials by remember { mutableStateOf(initialOptions.includeMaterials) }
    var includeComplications by remember { mutableStateOf(initialOptions.includeComplications) }
    var includePostOp by remember { mutableStateOf(initialOptions.includePostOp) }
    var includeSignature by remember { mutableStateOf(initialOptions.includeSignature) }
    var includeQrCode by remember { mutableStateOf(initialOptions.includeQrCode) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Generate operative protocol") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                Text("Language", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                    androidx.compose.material3.FilterChip(
                        selected = language == com.example.utils.ProtocolLanguage.FRENCH,
                        onClick = { language = com.example.utils.ProtocolLanguage.FRENCH },
                        label = { Text("Français") }
                    )
                    androidx.compose.material3.FilterChip(
                        selected = language == com.example.utils.ProtocolLanguage.ENGLISH,
                        onClick = { language = com.example.utils.ProtocolLanguage.ENGLISH },
                        label = { Text("English") }
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    androidx.compose.material3.Checkbox(checked = anonymize, onCheckedChange = { anonymize = it })
                    Text("Anonymize patient name and surgeon")
                }

                androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Include in PDF", style = MaterialTheme.typography.labelMedium)
                PdfOptionRow("Hospital / clinic", includeHospital) { includeHospital = it }
                PdfOptionRow("Surgeon", includeSurgeon, enabled = !anonymize) { includeSurgeon = it }
                PdfOptionRow("Assistants", includeAssistants) { includeAssistants = it }
                PdfOptionRow("Anesthesia", includeAnesthesia) { includeAnesthesia = it }
                PdfOptionRow("Diagnosis", includeDiagnosis) { includeDiagnosis = it }
                PdfOptionRow("Procedure", includeProcedure) { includeProcedure = it }
                PdfOptionRow("Operative steps", includeOperativeSteps) { includeOperativeSteps = it }
                PdfOptionRow("Materials used", includeMaterials) { includeMaterials = it }
                PdfOptionRow("Complications", includeComplications) { includeComplications = it }
                PdfOptionRow("Postoperative instructions", includePostOp) { includePostOp = it }
                PdfOptionRow("Signature", includeSignature) { includeSignature = it }
                PdfOptionRow("QR code (patient lookup)", includeQrCode, enabled = !anonymize) { includeQrCode = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    language,
                    anonymize,
                    com.example.utils.SurgeryPdfOptions(
                        includeHospital = includeHospital,
                        includeSurgeon = includeSurgeon,
                        includeAssistants = includeAssistants,
                        includeAnesthesia = includeAnesthesia,
                        includeDiagnosis = includeDiagnosis,
                        includeProcedure = includeProcedure,
                        includeOperativeSteps = includeOperativeSteps,
                        includeMaterials = includeMaterials,
                        includeComplications = includeComplications,
                        includePostOp = includePostOp,
                        includeSignature = includeSignature,
                        includeQrCode = includeQrCode
                    )
                )
            }) { Text("Generate") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun PatientChartOptionsDialog(
    initialOptions: com.example.utils.PatientChartPdfOptions,
    onDismiss: () -> Unit,
    onConfirm: (Boolean, com.example.utils.PatientChartPdfOptions) -> Unit
) {
    var anonymize by remember { mutableStateOf(false) }
    var includeContact by remember { mutableStateOf(initialOptions.includeContact) }
    var includeInsurance by remember { mutableStateOf(initialOptions.includeInsurance) }
    var includeMedicalHistory by remember { mutableStateOf(initialOptions.includeMedicalHistory) }
    var includeAllergies by remember { mutableStateOf(initialOptions.includeAllergies) }
    var includeChronicDiseases by remember { mutableStateOf(initialOptions.includeChronicDiseases) }
    var includePreviousSurgeries by remember { mutableStateOf(initialOptions.includePreviousSurgeries) }
    var includeMedications by remember { mutableStateOf(initialOptions.includeMedications) }
    var includeNotes by remember { mutableStateOf(initialOptions.includeNotes) }
    var includeOrthopedicInfo by remember { mutableStateOf(initialOptions.includeOrthopedicInfo) }
    var includeTimeline by remember { mutableStateOf(initialOptions.includeTimeline) }
    var includeSurgeries by remember { mutableStateOf(initialOptions.includeSurgeries) }
    var includeQrCode by remember { mutableStateOf(initialOptions.includeQrCode) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export patient chart") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                ) {
                    androidx.compose.material3.Checkbox(checked = anonymize, onCheckedChange = { anonymize = it })
                    Text("Anonymize patient name, contact & insurance")
                }

                androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Include in PDF", style = MaterialTheme.typography.labelMedium)
                PdfOptionRow("Contact info", includeContact, enabled = !anonymize) { includeContact = it }
                PdfOptionRow("Insurance", includeInsurance, enabled = !anonymize) { includeInsurance = it }
                PdfOptionRow("Medical history", includeMedicalHistory) { includeMedicalHistory = it }
                PdfOptionRow("Allergies", includeAllergies) { includeAllergies = it }
                PdfOptionRow("Chronic diseases", includeChronicDiseases) { includeChronicDiseases = it }
                PdfOptionRow("Previous surgeries (history)", includePreviousSurgeries) { includePreviousSurgeries = it }
                PdfOptionRow("Medications", includeMedications) { includeMedications = it }
                PdfOptionRow("Notes", includeNotes) { includeNotes = it }
                PdfOptionRow("Orthopedic information", includeOrthopedicInfo) { includeOrthopedicInfo = it }
                PdfOptionRow("Timeline", includeTimeline) { includeTimeline = it }
                PdfOptionRow("Surgeries", includeSurgeries) { includeSurgeries = it }
                PdfOptionRow("QR code (patient lookup)", includeQrCode, enabled = !anonymize) { includeQrCode = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    anonymize,
                    com.example.utils.PatientChartPdfOptions(
                        includeContact = includeContact,
                        includeInsurance = includeInsurance,
                        includeMedicalHistory = includeMedicalHistory,
                        includeAllergies = includeAllergies,
                        includeChronicDiseases = includeChronicDiseases,
                        includePreviousSurgeries = includePreviousSurgeries,
                        includeMedications = includeMedications,
                        includeNotes = includeNotes,
                        includeOrthopedicInfo = includeOrthopedicInfo,
                        includeTimeline = includeTimeline,
                        includeSurgeries = includeSurgeries,
                        includeQrCode = includeQrCode
                    )
                )
            }) { Text("Generate") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun PdfOptionRow(label: String, checked: Boolean, enabled: Boolean = true, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        androidx.compose.material3.Checkbox(checked = checked && enabled, onCheckedChange = onCheckedChange, enabled = enabled)
        Text(
            text = label,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun PatientHeader(detail: PatientDetail, cim10Diseases: List<Cim10Disease>) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = detail.patient.patientCode,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            val ageGender = buildString {
                detail.age?.let { append("$it yrs") }
                if (detail.patient.gender.isNotBlank()) {
                    if (isNotEmpty()) append(" • ")
                    append(detail.patient.gender)
                }
            }
            if (ageGender.isNotBlank()) {
                Text(text = ageGender, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (detail.patient.diagnosis.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Cim10DiseaseText(
                text = detail.patient.diagnosis,
                diseases = cim10Diseases
            )
        }
        if (detail.patient.bodyRegion.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RegionBadge(region = detail.patient.bodyRegion)
                if (detail.patient.side.isNotBlank() && detail.patient.side != "NOT_APPLICABLE") {
                    RegionBadge(region = detail.patient.side)
                }
            }
        }
    }
}

@Composable
private fun InfoTab(detail: PatientDetail, cim10Diseases: List<Cim10Disease>) {
    val p = detail.patient
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            InfoSection(
                title = "Contact",
                rows = listOf(
                    "Date of birth" to DateFormat.formatDate(p.dateOfBirth),
                    "Phone" to p.phone,
                    "Email" to p.email,
                    "Address" to p.address,
                    "Emergency contact" to listOf(p.emergencyContactName, p.emergencyContactPhone).filter { it.isNotBlank() }.joinToString(" — ")
                ),
                cim10Diseases = cim10Diseases
            )
        }
        item {
            InfoSection(
                title = "Insurance",
                rows = listOf(
                    "Provider" to p.insuranceProvider,
                    "Policy number" to p.insuranceNumber
                ),
                cim10Diseases = cim10Diseases
            )
        }
        item {
            InfoSection(
                title = "Medical history",
                rows = listOf(
                    "History" to p.medicalHistory,
                    "Allergies" to p.allergies,
                    "Chronic diseases" to p.chronicDiseases,
                    "Previous surgeries" to p.previousSurgeries,
                    "Medications" to p.medications,
                    "Notes" to p.generalNotes
                ),
                cim10Diseases = cim10Diseases
            )
        }
        item {
            InfoSection(
                title = "Orthopedic",
                rows = listOf(
                    "Diagnosis" to p.diagnosis,
                    "Body region" to p.bodyRegion,
                    "Side" to p.side,
                    "Injury date" to DateFormat.formatDate(p.injuryDate),
                    "Trauma mechanism" to p.traumaMechanism,
                    "Fracture classification" to p.fractureClassification,
                    "Imaging findings" to p.imagingFindings,
                    "Treatment plan" to p.treatmentPlan
                ),
                cim10Diseases = cim10Diseases
            )
        }
    }
}

@Composable
private fun InfoSection(title: String, rows: List<Pair<String, String>>, cim10Diseases: List<Cim10Disease>) {
    val visibleRows = rows.filter { it.second.isNotBlank() && it.second != "—" }
    if (visibleRows.isEmpty()) return

    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        visibleRows.forEach { (label, value) ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(0.4f)
                )
                Cim10DiseaseText(
                    text = value,
                    diseases = cim10Diseases,
                    modifier = Modifier.weight(0.6f)
                )
            }
        }
    }
}

@Composable
private fun TimelineTab(
    detail: PatientDetail,
    onEventClick: (String) -> Unit,
    onEventDelete: (String) -> Unit,
    cim10Diseases: List<Cim10Disease>
) {
    val timeline = detail.timeline
    if (timeline.all.isEmpty()) {
        EmptyStateView(
            icon = Icons.Default.Add,
            title = "No timeline events",
            message = "Tap + to log a consultation, imaging, or a planned follow-up.",
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (timeline.future.isNotEmpty()) {
            item { TimelineSectionLabel("Upcoming") }
            items(timeline.future, key = { it.id }) { TimelineEventRow(it, onClick = { onEventClick(it.id) }, onDelete = { onEventDelete(it.id) }, cim10Diseases = cim10Diseases) }
        }
        if (timeline.current.isNotEmpty()) {
            item { TimelineSectionLabel("Today") }
            items(timeline.current, key = { it.id }) { TimelineEventRow(it, onClick = { onEventClick(it.id) }, onDelete = { onEventDelete(it.id) }, cim10Diseases = cim10Diseases) }
        }
        if (timeline.past.isNotEmpty()) {
            item { TimelineSectionLabel("Previous") }
            items(timeline.past, key = { it.id }) { TimelineEventRow(it, onClick = { onEventClick(it.id) }, onDelete = { onEventDelete(it.id) }, cim10Diseases = cim10Diseases) }
        }
    }
}

@Composable
private fun TimelineSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun SurgeriesTab(
    detail: PatientDetail,
    onSurgeryClick: (String) -> Unit,
    onSurgeryDelete: (String) -> Unit,
    onGenerateProtocol: (String) -> Unit,
    cim10Diseases: List<Cim10Disease>
) {
    if (detail.surgeries.isEmpty()) {
        EmptyStateView(
            icon = Icons.Default.Add,
            title = "No surgeries recorded",
            message = "Tap + to add a surgery record for this patient.",
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(detail.surgeries, key = { it.id }) { surgery ->
            SurgeryRow(
                surgery = surgery,
                onClick = { onSurgeryClick(surgery.id) },
                onDelete = { onSurgeryDelete(surgery.id) },
                onGenerateProtocol = { onGenerateProtocol(surgery.id) },
                cim10Diseases = cim10Diseases
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaTab(
    detail: PatientDetail,
    viewModel: PatientDetailViewModel,
    onAddMedia: () -> Unit,
    onImageClick: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showMoveMenu by remember { mutableStateOf(false) }
    var pendingGallerySaveIds by remember { mutableStateOf<Set<String>?>(null) }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        val ids = pendingGallerySaveIds
        pendingGallerySaveIds = null
        if (granted && ids != null) {
            saveImagesToGallery(context, detail, ids)
        }
    }

    fun requestSaveToGallery(ids: Set<String>) {
        val needsLegacyPermission = android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q &&
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (needsLegacyPermission) {
            pendingGallerySaveIds = ids
            permissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            saveImagesToGallery(context, detail, ids)
        }
    }

    val sections = remember(detail.images) {
        listOf(
            "Radiology" to detail.images.filter { it.category == com.example.data.local.entity.ImageCategory.RADIOLOGY.name },
            "Clinical" to detail.images.filter { it.category == com.example.data.local.entity.ImageCategory.CLINICAL_PHOTO.name },
            "Other" to detail.images.filter {
                it.category != com.example.data.local.entity.ImageCategory.RADIOLOGY.name &&
                    it.category != com.example.data.local.entity.ImageCategory.CLINICAL_PHOTO.name
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (detail.images.isEmpty() && detail.documents.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.Image,
                title = "No media yet",
                message = "Tap + to capture a photo, take a radiology shot, or scan a document.",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                if (selectedIds.isNotEmpty()) {
                    MediaSelectionBar(
                        count = selectedIds.size,
                        onClose = { selectedIds = emptySet() },
                        onShare = {
                            val files = detail.images.filter { it.id in selectedIds }.map { java.io.File(it.filePath) }
                            com.example.utils.ShareHelper.shareFiles(context, files, "image/jpeg", "Share images")
                        },
                        onSave = { requestSaveToGallery(selectedIds) },
                        onMove = { showMoveMenu = true },
                        onDelete = { showDeleteConfirm = true }
                    )
                }

                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
                    contentPadding = PaddingValues(16.dp, top = 16.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    sections.forEach { (label, images) ->
                        if (images.isNotEmpty()) {
                            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                                )
                            }
                            gridItems(images, key = { it.id }) { image ->
                                val isSelected = image.id in selectedIds
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                        .then(
                                            if (isSelected) {
                                                Modifier.border(3.dp, MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .combinedClickable(
                                            onClick = {
                                                if (selectedIds.isNotEmpty()) {
                                                    selectedIds = if (isSelected) selectedIds - image.id else selectedIds + image.id
                                                } else {
                                                    onImageClick(image.id)
                                                }
                                            },
                                            onLongClick = {
                                                selectedIds = if (isSelected) selectedIds - image.id else selectedIds + image.id
                                            }
                                        )
                                ) {
                                    coil.compose.AsyncImage(
                                        model = "file://${image.thumbnailPath ?: image.filePath}",
                                        contentDescription = image.category,
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                                        )
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (selectedIds.isEmpty()) {
            FloatingActionButton(
                onClick = onAddMedia,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add media")
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete ${selectedIds.size} item${if (selectedIds.size == 1) "" else "s"}?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteImages(selectedIds)
                    selectedIds = emptySet()
                    showDeleteConfirm = false
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }

    if (showMoveMenu) {
        AlertDialog(
            onDismissRequest = { showMoveMenu = false },
            title = { Text("Move to section") },
            text = {
                Column {
                    listOf(
                        "Radiology" to com.example.data.local.entity.ImageCategory.RADIOLOGY,
                        "Clinical" to com.example.data.local.entity.ImageCategory.CLINICAL_PHOTO,
                        "Other" to com.example.data.local.entity.ImageCategory.OTHER
                    ).forEach { (label, category) ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateImageCategory(selectedIds, category)
                                    selectedIds = emptySet()
                                    showMoveMenu = false
                                }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showMoveMenu = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun MediaSelectionBar(
    count: Int,
    onClose: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = "Clear selection")
        }
        Text(
            text = "$count selected",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onShare) { Icon(Icons.Default.Share, contentDescription = "Share") }
        IconButton(onClick = onSave) { Icon(Icons.Default.Download, contentDescription = "Save to gallery") }
        IconButton(onClick = onMove) { Icon(Icons.Default.DriveFileMove, contentDescription = "Move to section") }
        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error) }
    }
}

private fun saveImagesToGallery(context: android.content.Context, detail: PatientDetail, ids: Set<String>) {
    val images = detail.images.filter { it.id in ids }
    var successCount = 0
    images.forEach { image ->
        val result = com.example.utils.GallerySaver.saveToGallery(context, java.io.File(image.filePath), "IMG_${image.id}.jpg")
        if (result is com.example.utils.GallerySaver.Result.Success) successCount++
    }
    val message = if (successCount == images.size) {
        "Saved $successCount photo${if (successCount == 1) "" else "s"} to gallery"
    } else {
        "Saved $successCount of ${images.size} photos to gallery"
    }
    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
}
