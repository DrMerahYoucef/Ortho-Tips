package com.example.ui.screens.patients

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
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
import com.example.ui.components.EmptyStateView
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
    onNavigateBack: () -> Unit,
    onNavigateToEdit: () -> Unit,
    onNavigateToAddTimelineEvent: () -> Unit,
    onNavigateToEditTimelineEvent: (String) -> Unit,
    onNavigateToAddSurgery: () -> Unit,
    onNavigateToEditSurgery: (String) -> Unit,
    onNavigateToCapture: () -> Unit,
    onNavigateToAnnotate: (String) -> Unit,
    onGenerateProtocol: (String, com.example.utils.ProtocolLanguage, Boolean) -> Unit,
    onGenerateChartPdf: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(DetailTab.INFO.ordinal) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var protocolSurgeryId by remember { mutableStateOf<String?>(null) }

    if (uiState.wasDeleted && !uiState.isLoading) {
        onNavigateBack()
        return
    }

    val detail = uiState.detail

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
                            text = { Text("Export PDF") },
                            onClick = { menuExpanded = false; onGenerateChartPdf(false) }
                        )
                        DropdownMenuItem(
                            text = { Text("Export anonymous PDF") },
                            onClick = { menuExpanded = false; onGenerateChartPdf(true) }
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
            PatientHeader(detail)

            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Info") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Timeline") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Surgeries") })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Media") })
            }

            when (DetailTab.entries[selectedTab]) {
                DetailTab.INFO -> InfoTab(detail)
                DetailTab.TIMELINE -> TimelineTab(
                    detail = detail,
                    onEventClick = onNavigateToEditTimelineEvent,
                    onEventDelete = { viewModel.deleteTimelineEvent(it) }
                )
                DetailTab.SURGERIES -> SurgeriesTab(
                    detail = detail,
                    onSurgeryClick = onNavigateToEditSurgery,
                    onSurgeryDelete = { viewModel.deleteSurgery(it) },
                    onGenerateProtocol = { surgeryId -> protocolSurgeryId = surgeryId }
                )
                DetailTab.MEDIA -> MediaTab(
                    detail = detail,
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
            onDismiss = { protocolSurgeryId = null },
            onConfirm = { language, anonymize ->
                onGenerateProtocol(surgeryId, language, anonymize)
                protocolSurgeryId = null
            }
        )
    }
}

@Composable
private fun ProtocolLanguageDialog(
    onDismiss: () -> Unit,
    onConfirm: (com.example.utils.ProtocolLanguage, Boolean) -> Unit
) {
    var language by remember { mutableStateOf(com.example.utils.ProtocolLanguage.FRENCH) }
    var anonymize by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Generate operative protocol") },
        text = {
            Column {
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
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(language, anonymize) }) { Text("Generate") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun PatientHeader(detail: PatientDetail) {
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
            Text(
                text = detail.patient.diagnosis,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
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
private fun InfoTab(detail: PatientDetail) {
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
                )
            )
        }
        item {
            InfoSection(
                title = "Insurance",
                rows = listOf(
                    "Provider" to p.insuranceProvider,
                    "Policy number" to p.insuranceNumber
                )
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
                )
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
                )
            )
        }
    }
}

@Composable
private fun InfoSection(title: String, rows: List<Pair<String, String>>) {
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
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
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
    onEventDelete: (String) -> Unit
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
            items(timeline.future, key = { it.id }) { TimelineEventRow(it, onClick = { onEventClick(it.id) }, onDelete = { onEventDelete(it.id) }) }
        }
        if (timeline.current.isNotEmpty()) {
            item { TimelineSectionLabel("Today") }
            items(timeline.current, key = { it.id }) { TimelineEventRow(it, onClick = { onEventClick(it.id) }, onDelete = { onEventDelete(it.id) }) }
        }
        if (timeline.past.isNotEmpty()) {
            item { TimelineSectionLabel("Previous") }
            items(timeline.past, key = { it.id }) { TimelineEventRow(it, onClick = { onEventClick(it.id) }, onDelete = { onEventDelete(it.id) }) }
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
    onGenerateProtocol: (String) -> Unit
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
                onGenerateProtocol = { onGenerateProtocol(surgery.id) }
            )
        }
    }
}

@Composable
private fun MediaTab(
    detail: PatientDetail,
    onAddMedia: () -> Unit,
    onImageClick: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (detail.images.isEmpty() && detail.documents.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.Image,
                title = "No media yet",
                message = "Tap + to capture a photo, take a radiology shot, or scan a document.",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
                contentPadding = PaddingValues(16.dp, top = 16.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                gridItems(detail.images, key = { it.id }) { image ->
                    coil.compose.AsyncImage(
                        model = "file://${image.thumbnailPath ?: image.filePath}",
                        contentDescription = image.category,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                            .clickable { onImageClick(image.id) }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onAddMedia,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add media")
        }
    }
}
