package com.example.ui.screens.patients

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.utils.DateFormat
import com.example.viewmodel.SurgeryEditViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurgeryEditScreen(
    viewModel: SurgeryEditViewModel,
    onNavigateBack: () -> Unit,
    onSaved: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.savedSurgeryId) {
        uiState.savedSurgeryId?.let { onSaved(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Surgery record", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    TextButton(onClick = { viewModel.save() }, enabled = !uiState.isSaving) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            item {
                OutlinedTextField(
                    value = DateFormat.formatDate(uiState.form.surgeryDate),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Surgery date") },
                    trailingIcon = {
                        IconButton(onClick = { showPicker = true }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Pick date")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Column {
                    OutlinedTextField(
                        value = uiState.form.procedure,
                        onValueChange = { v -> viewModel.update { it.copy(procedure = v) } },
                        label = { Text("Procedure") },
                        isError = uiState.procedureError != null,
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                    uiState.procedureError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item { LabeledField("Hospital / clinic", uiState.form.hospitalClinic) { v -> viewModel.update { it.copy(hospitalClinic = v) } } }
            item { LabeledField("Surgeon", uiState.form.surgeon) { v -> viewModel.update { it.copy(surgeon = v) } } }
            item { LabeledField("Assistants", uiState.form.assistants) { v -> viewModel.update { it.copy(assistants = v) } } }
            item { LabeledField("Anesthesia", uiState.form.anesthesiaType) { v -> viewModel.update { it.copy(anesthesiaType = v) } } }
            item { LabeledField("Diagnosis", uiState.form.diagnosis, singleLine = false) { v -> viewModel.update { it.copy(diagnosis = v) } } }
            item { LabeledField("Materials used", uiState.form.materialsUsed, singleLine = false) { v -> viewModel.update { it.copy(materialsUsed = v) } } }
            item { LabeledField("Complications", uiState.form.complications, singleLine = false) { v -> viewModel.update { it.copy(complications = v) } } }
            item { LabeledField("Postoperative instructions", uiState.form.postOpInstructions, singleLine = false) { v -> viewModel.update { it.copy(postOpInstructions = v) } } }
            item {
                Button(
                    onClick = { viewModel.save() },
                    enabled = !uiState.isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (uiState.isSaving) "Saving..." else "Save surgery")
                }
            }
        }
    }

    if (showPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = uiState.form.surgeryDate)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis -> viewModel.update { it.copy(surgeryDate = millis) } }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun LabeledField(label: String, value: String, singleLine: Boolean = true, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        modifier = Modifier.fillMaxWidth()
    )
}
