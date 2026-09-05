package com.example.ui.screens.patients

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.EmptyStateView
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (uiState.isSelectionMode) {
                        Text("${uiState.selectedIds.size} selected")
                    } else {
                        Text("Patients", fontWeight = FontWeight.Bold)
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
                        IconButton(onClick = { onExportSelected(uiState.selectedIds.toList()); viewModel.clearSelection() }) {
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
                            Icon(Icons.Default.Badge, contentDescription = "Surgeon profile")
                        }
                        IconButton(onClick = onNavigateToDataTransfer) {
                            Icon(Icons.Default.ImportExport, contentDescription = "Import / export database")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            if (!uiState.isSelectionMode) {
                FloatingActionButton(
                    onClick = onNavigateToAddPatient,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add patient")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            PrimaryTabRow(selectedTabIndex = uiState.tab.ordinal) {
                Tab(
                    selected = uiState.tab == PatientListTab.ACTIVE,
                    onClick = { viewModel.setTab(PatientListTab.ACTIVE) },
                    text = { Text("Active") }
                )
                Tab(
                    selected = uiState.tab == PatientListTab.ARCHIVED,
                    onClick = { viewModel.setTab(PatientListTab.ARCHIVED) },
                    text = { Text("Archived") }
                )
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        OrthoSearchBar(
                            query = uiState.searchQuery,
                            onQueryChange = { viewModel.setSearchQuery(it) },
                            onSearch = { viewModel.setSearchQuery(it) },
                            onClear = { viewModel.setSearchQuery("") },
                            placeholderText = "Search name, ID, diagnosis..."
                        )
                    }
                    IconButton(onClick = onNavigateToQrScanner) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan patient QR code")
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
                    EmptyStateView(
                        icon = Icons.Default.PersonSearch,
                        title = if (uiState.searchQuery.isNotBlank()) "No matches" else "No patients yet",
                        message = if (uiState.searchQuery.isNotBlank()) {
                            "No patient matches \"${uiState.searchQuery}\"."
                        } else if (uiState.tab == PatientListTab.ARCHIVED) {
                            "Archived patients will appear here."
                        } else {
                            "Tap the + button to add your first patient."
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                else -> {
                    LazyColumn(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 96.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.patients, key = { it.id }) { patient ->
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
