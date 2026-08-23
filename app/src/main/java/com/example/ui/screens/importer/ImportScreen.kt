package com.example.ui.screens.importer

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.importer.ImportState
import com.example.ui.theme.ClinicalPearl
import com.example.viewmodel.ImportViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    viewModel: ImportViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val importState by viewModel.importState.collectAsStateWithLifecycle()
    var jsonInput by remember { mutableStateOf("") }

    val sampleJson = """
{
  "bones": [
    {
      "id": "bone_scaphoid_orif",
      "chapterId": "chap_upper_limb",
      "name": "Scaphoid Waist Fracture ORIF",
      "subtitle": "Percutaneous & Open Cannulated Headless Compression Screw",
      "region": "Wrist & Hand",
      "traumaType": "B2 Fracture",
      "classificationSummary": "Herbert-Fisher Type B2 (Unstable Acute Waist)",
      "isHighYield": true,
      "sections": [
        {
          "sectionType": "ANATOMY",
          "title": "Retrograde Blood Supply & Vascular Risk",
          "content": "80% of scaphoid blood supply enters through dorsal ridge via radial artery branches to supply the proximal pole retrogradely.",
          "pearls": "High risk of proximal pole avascular necrosis (AVN) in proximal 1/3 fractures.",
          "warnings": "Avoid excessive soft tissue stripping at the dorsal ridge during dorsal approaches."
        },
        {
          "sectionType": "APPROACH",
          "title": "Volar vs Dorsal Percutaneous Trajectory",
          "content": "1. Volar (Ruskin) approach: Best for waist and distal fractures.\n2. Dorsal approach: Best for proximal pole fractures via 3/4 extensor compartment interval.",
          "pearls": "Aim guide wire down the central axis of the scaphoid in both AP and lateral/pronated oblique views.",
          "warnings": "Countersink the screw head beneath the articular cartilage completely."
        }
      ]
    }
  ]
}
""".trimIndent()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Import Guidelines",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("import_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "JSON Surgical Technique Importer",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Import custom institution guidelines, departmental reduction checklists, or new fracture chapters directly into Ortho Tips' offline database.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Quick Sample Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "JSON Code Input:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedButton(
                        onClick = { jsonInput = sampleJson },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("load_sample_json_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Load Sample", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Input TextField
            item {
                OutlinedTextField(
                    value = jsonInput,
                    onValueChange = { jsonInput = it },
                    placeholder = { Text("Paste valid JSON array of chapters, bones, or sections...") },
                    minLines = 8,
                    maxLines = 14,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("json_import_input")
                )
            }

            // Action Button
            item {
                Button(
                    onClick = { viewModel.importJsonData(jsonInput) },
                    enabled = jsonInput.isNotBlank() && importState !is ImportState.Loading,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("execute_import_button")
                ) {
                    if (importState is ImportState.Loading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Importing...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import into Clinical Database", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Status message card
            when (val state = importState) {
                is ImportState.Success -> {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = ClinicalPearl.copy(alpha = 0.15f)
                            ),
                            border = BorderStroke(1.dp, ClinicalPearl.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Success",
                                    tint = ClinicalPearl,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Import Succeeded",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = ClinicalPearl
                                    )
                                    Text(
                                        text = state.details,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
                is ImportState.Error -> {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Import Failed",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}
