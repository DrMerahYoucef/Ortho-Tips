package com.example.ui.screens.patients

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import com.example.data.local.entity.BodySide
import com.example.utils.DateFormat
import com.example.viewmodel.PatientEditViewModel
import com.example.viewmodel.PatientFormState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientEditScreen(
    viewModel: PatientEditViewModel,
    onNavigateBack: () -> Unit,
    onSaved: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.savedPatientId) {
        uiState.savedPatientId?.let { onSaved(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "Edit patient" else "New patient", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
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
            item { FormSectionLabel("Identification") }
            item {
                FormTextField(
                    value = uiState.form.patientCode,
                    onValueChange = { v -> viewModel.update { it.copy(patientCode = v) } },
                    label = "Patient ID",
                    isError = uiState.patientCodeError != null,
                    errorText = uiState.patientCodeError
                )
            }
            item {
                FormTextField(
                    value = uiState.form.fullName,
                    onValueChange = { v -> viewModel.update { it.copy(fullName = v) } },
                    label = "Full name",
                    isError = uiState.nameError != null,
                    errorText = uiState.nameError
                )
            }
            item {
                DateField(
                    label = "Date of birth",
                    dateMillis = uiState.form.dateOfBirth,
                    onDateSelected = { v -> viewModel.update { it.copy(dateOfBirth = v) } }
                )
            }
            item {
                GenderDropdown(
                    value = uiState.form.gender,
                    onValueChange = { v -> viewModel.update { it.copy(gender = v) } }
                )
            }

            item { FormSectionLabel("Contact") }
            item { FormTextField(uiState.form.phone, { v -> viewModel.update { it.copy(phone = v) } }, "Phone") }
            item { FormTextField(uiState.form.email, { v -> viewModel.update { it.copy(email = v) } }, "Email") }
            item { FormTextField(uiState.form.address, { v -> viewModel.update { it.copy(address = v) } }, "Address", singleLine = false) }
            item { FormTextField(uiState.form.emergencyContactName, { v -> viewModel.update { it.copy(emergencyContactName = v) } }, "Emergency contact name") }
            item { FormTextField(uiState.form.emergencyContactPhone, { v -> viewModel.update { it.copy(emergencyContactPhone = v) } }, "Emergency contact phone") }

            item { FormSectionLabel("Insurance") }
            item { FormTextField(uiState.form.insuranceProvider, { v -> viewModel.update { it.copy(insuranceProvider = v) } }, "Provider") }
            item { FormTextField(uiState.form.insuranceNumber, { v -> viewModel.update { it.copy(insuranceNumber = v) } }, "Policy number") }

            item { FormSectionLabel("Medical history") }
            item { FormTextField(uiState.form.medicalHistory, { v -> viewModel.update { it.copy(medicalHistory = v) } }, "Medical history", singleLine = false) }
            item { FormTextField(uiState.form.allergies, { v -> viewModel.update { it.copy(allergies = v) } }, "Allergies", singleLine = false) }
            item { FormTextField(uiState.form.chronicDiseases, { v -> viewModel.update { it.copy(chronicDiseases = v) } }, "Chronic diseases", singleLine = false) }
            item { FormTextField(uiState.form.previousSurgeries, { v -> viewModel.update { it.copy(previousSurgeries = v) } }, "Previous surgeries", singleLine = false) }
            item { FormTextField(uiState.form.medications, { v -> viewModel.update { it.copy(medications = v) } }, "Medications", singleLine = false) }
            item { FormTextField(uiState.form.generalNotes, { v -> viewModel.update { it.copy(generalNotes = v) } }, "Notes", singleLine = false) }

            item { FormSectionLabel("Orthopedic information") }
            item { FormTextField(uiState.form.diagnosis, { v -> viewModel.update { it.copy(diagnosis = v) } }, "Diagnosis", singleLine = false) }
            item { FormTextField(uiState.form.bodyRegion, { v -> viewModel.update { it.copy(bodyRegion = v) } }, "Body region") }
            item {
                SideDropdown(
                    value = uiState.form.side,
                    onValueChange = { v -> viewModel.update { it.copy(side = v) } }
                )
            }
            item {
                DateField(
                    label = "Injury date",
                    dateMillis = uiState.form.injuryDate,
                    onDateSelected = { v -> viewModel.update { it.copy(injuryDate = v) } }
                )
            }
            item { FormTextField(uiState.form.traumaMechanism, { v -> viewModel.update { it.copy(traumaMechanism = v) } }, "Trauma mechanism", singleLine = false) }
            item { FormTextField(uiState.form.fractureClassification, { v -> viewModel.update { it.copy(fractureClassification = v) } }, "Fracture classification") }
            item { FormTextField(uiState.form.imagingFindings, { v -> viewModel.update { it.copy(imagingFindings = v) } }, "Imaging findings", singleLine = false) }
            item { FormTextField(uiState.form.treatmentPlan, { v -> viewModel.update { it.copy(treatmentPlan = v) } }, "Treatment plan", singleLine = false) }

            item {
                Button(
                    onClick = { viewModel.save() },
                    enabled = !uiState.isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (uiState.isSaving) "Saving..." else "Save patient")
                }
            }
        }
    }
}

@Composable
private fun FormSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    singleLine: Boolean = true,
    isError: Boolean = false,
    errorText: String? = null
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = singleLine,
            isError = isError,
            modifier = Modifier.fillMaxWidth()
        )
        if (isError && errorText != null) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 16.dp, top = 2.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(label: String, dateMillis: Long?, onDateSelected: (Long?) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = DateFormat.formatDate(dateMillis).let { if (it == "—") "" else it },
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        trailingIcon = {
            IconButton(onClick = { showPicker = true }) {
                Icon(Icons.Default.CalendarToday, contentDescription = "Pick date")
            }
        },
        modifier = Modifier.fillMaxWidth()
    )

    if (showPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = { onDateSelected(state.selectedDateMillis); showPicker = false }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GenderDropdown(value: String, onValueChange: (String) -> Unit) {
    val options = listOf("Male", "Female", "Other")
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            readOnly = false,
            label = { Text("Gender") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onValueChange(option); expanded = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SideDropdown(value: BodySide, onValueChange: (BodySide) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = value.name.lowercase().replaceFirstChar { it.uppercase() },
            onValueChange = {},
            readOnly = true,
            label = { Text("Side") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            BodySide.entries.forEach { side ->
                DropdownMenuItem(
                    text = { Text(side.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    onClick = { onValueChange(side); expanded = false }
                )
            }
        }
    }
}
