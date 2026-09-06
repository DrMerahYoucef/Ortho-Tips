package com.example.ui.screens.patients

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.OrthoSearchBar
import com.example.ui.components.PatientCard
import com.example.viewmodel.PatientListTab
import com.example.viewmodel.PatientListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientListScreen(
    viewModel: PatientListViewModel,
    onNavigateToPatientDetail: (String) -> Unit,
    onNavigateToAddPatient: () -> Unit,
    onNavigateToSurgeonProfile: () -> Unit,
    onNavigateToDataTransfer: () -> Unit,
    onNavigateToQrScanner: () -> Unit,
    onExportSelected: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val regionFilters = listOf("All", "Trauma", "Femur", "Tibia", "Radius", "Spine", "Ankle", "Humerus")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (uiState.isSelectionMode) {
                        Text(
                            text = "${uiState.selectedIds.size} selected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.MedicalServices,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Patients & Cases",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.2).sp
                                )
                                Text(
                                    text = "Clinical Records & Surgical Logs",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    if (uiState.isSelectionMode) {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel selection")
                        }
                    }
                },
                actions = {
                    if (uiState.isSelectionMode) {
                        IconButton(onClick = {
                            onExportSelected(uiState.selectedIds.toList())
                            viewModel.clearSelection()
                        }) {
                            Icon(Icons.Default.CloudUpload, contentDescription = "Export selected")
                        }
                        if (uiState.tab == PatientListTab.ACTIVE) {
                            IconButton(onClick = { viewModel.archiveSelected() }) {
                                Icon(Icons.Default.Archive, contentDescription = "Archive")
                            }
                        } else {
                            IconButton(onClick = { viewModel.unarchiveSelected() }) {
                                Icon(Icons.Default.Unarchive, contentDescription = "Unarchive")
                            }
                        }
                        IconButton(onClick = { viewModel.deleteSelected() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    } else {
                        IconButton(onClick = onNavigateToSurgeonProfile) {
                            Icon(Icons.Default.Badge, contentDescription = "Surgeon Profile")
                        }
                        IconButton(onClick = onNavigateToDataTransfer) {
                            Icon(Icons.Default.ImportExport, contentDescription = "Import / Export Data")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                modifier = Modifier.testTag("patient_list_top_bar")
            )
        },
        floatingActionButton = {
            if (!uiState.isSelectionMode) {
                ExtendedFloatingActionButton(
                    onClick = onNavigateToAddPatient,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("New Patient", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("add_patient_fab")
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Summary KPI banner
            PatientDashboardKpiBanner(
                activeCount = uiState.activeCount,
                archivedCount = uiState.archivedCount,
                onAddPatient = onNavigateToAddPatient,
                onScanQr = onNavigateToQrScanner
            )

            // Cohort switcher tabs
            PrimaryTabRow(
                selectedTabIndex = uiState.tab.ordinal,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = uiState.tab == PatientListTab.ACTIVE,
                    onClick = { viewModel.setTab(PatientListTab.ACTIVE) },
                    text = {
                        Text(
                            text = "Active Cohort (${uiState.activeCount})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (uiState.tab == PatientListTab.ACTIVE) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    modifier = Modifier.testTag("tab_patients_active")
                )
                Tab(
                    selected = uiState.tab == PatientListTab.ARCHIVED,
                    onClick = { viewModel.setTab(PatientListTab.ARCHIVED) },
                    text = {
                        Text(
                            text = "Archived (${uiState.archivedCount})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (uiState.tab == PatientListTab.ARCHIVED) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    modifier = Modifier.testTag("tab_patients_archived")
                )
            }

            // Search bar & QR Action
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        OrthoSearchBar(
                            query = uiState.searchQuery,
                            onQueryChange = { viewModel.setSearchQuery(it) },
                            onSearch = { viewModel.setSearchQuery(it) },
                            onClear = { viewModel.setSearchQuery("") },
                            placeholderText = "Search by name, ID, diagnosis..."
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                        modifier = Modifier
                            .size(48.dp)
                            .clickable(onClick = onNavigateToQrScanner)
                            .testTag("qr_scanner_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan QR",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Region filter chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    items(regionFilters) { region ->
                        val isSelected = uiState.regionFilter.equals(region, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setRegionFilter(region) },
                            label = { Text(region, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                uiState.isEmpty -> {
                    ModernPatientEmptyState(
                        isSearching = uiState.searchQuery.isNotBlank(),
                        isArchivedTab = uiState.tab == PatientListTab.ARCHIVED,
                        searchQuery = uiState.searchQuery,
                        onAddPatient = onNavigateToAddPatient
                    )
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 4.dp,
                            bottom = 96.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("patients_scroll_container")
                    ) {
                        items(uiState.filteredPatients, key = { it.id }) { patient ->
                            PatientCard(
                                patient = patient,
                                isSelected = uiState.selectedIds.contains(patient.id),
                                isSelectionMode = uiState.isSelectionMode,
                                onClick = {
                                    if (uiState.isSelectionMode) {
                                        viewModel.toggleSelection(patient.id)
                                    } else {
                                        onNavigateToPatientDetail(patient.id)
                                    }
                                },
                                onLongClick = { viewModel.toggleSelection(patient.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PatientDashboardKpiBanner(
    activeCount: Int,
    archivedCount: Int,
    onAddPatient: () -> Unit,
    onScanQr: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text(
                        text = "$activeCount",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Active Cohort",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(34.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                )

                Column {
                    Text(
                        text = "$archivedCount",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Archived Cases",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onAddPatient)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Add",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.clickable(onClick = onScanQr)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Scan",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernPatientEmptyState(
    isSearching: Boolean,
    isArchivedTab: Boolean,
    searchQuery: String,
    onAddPatient: () -> Unit
) {
    if (isSearching) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PersonSearch,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Patients Found",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "No patient matches \"$searchQuery\". Try checking the spelling or removing filters.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        return
    }

    if (isArchivedTab) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Archive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Archived Patients",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Patients you mark as archived after recovery will appear here for long-term records.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        return
    }

    // Modern Onboarding Guide for New Patient Management
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 24.dp)
    ) {
        item {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(76.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.MedicalServices,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Clinical Patient Management",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Organize surgical cases, operative protocols, pre/post-op imaging, and patient timelines in one secure place.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(onClick = onAddPatient)
                    .testTag("empty_state_register_patient_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 13.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Register First Patient",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "HOW PATIENT MANAGEMENT WORKS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )

                    WorkflowStep(
                        step = "1",
                        title = "Demographics & Orthopedic Diagnosis",
                        description = "Record patient identifiers, body region, fracture pattern, and medical history.",
                        icon = Icons.Default.PersonAdd
                    )

                    WorkflowStep(
                        step = "2",
                        title = "Surgical Procedures & Implant Logs",
                        description = "Log operative dates, hardware, anesthesia type, and surgical assistants.",
                        icon = Icons.Default.MedicalServices
                    )

                    WorkflowStep(
                        step = "3",
                        title = "Imaging, Photos & Annotations",
                        description = "Attach fluoroscopy, X-rays, clinical wounds, or scan documents with the camera.",
                        icon = Icons.Default.CameraAlt
                    )

                    WorkflowStep(
                        step = "4",
                        title = "PDF Protocols & Anonymized Charts",
                        description = "Generate multilingual PDF operative reports and print patient charts on demand.",
                        icon = Icons.Default.Description
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkflowStep(
    step: String,
    title: String,
    description: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(34.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 17.sp
            )
        }
    }
}
