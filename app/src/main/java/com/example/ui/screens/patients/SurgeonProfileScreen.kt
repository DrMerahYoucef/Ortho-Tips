package com.example.ui.screens.patients

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.prefs.SurgeonProfile
import com.example.utils.ImageCompressor
import com.example.utils.PatientMediaStorage
import com.example.viewmodel.SurgeonProfileViewModel
import java.io.File

@Composable
fun SurgeonProfileScreen(
    viewModel: SurgeonProfileViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val storedProfile by viewModel.profile.collectAsStateWithLifecycle()
    var form by remember { mutableStateOf(SurgeonProfile()) }
    var initialized by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(storedProfile) {
        if (!initialized) {
            form = storedProfile
            initialized = true
        }
    }

    val signaturePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val destination = File(PatientMediaStorage.rootDir(context), "surgeon_signature.jpg").also { file ->
                file.parentFile?.mkdirs()
            }
            ImageCompressor.compressToFile(context.contentResolver, uri, destination)
            form = form.copy(signatureImagePath = destination.absolutePath)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Surgeon profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
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
                Text(
                    text = "Used to auto-fill operative protocols and PDF exports.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item { ProfileField("Full name", form.name) { form = form.copy(name = it) } }
            item { ProfileField("Specialty", form.specialty) { form = form.copy(specialty = it) } }
            item { ProfileField("Hospital / clinic", form.hospital) { form = form.copy(hospital = it) } }
            item { ProfileField("Address", form.address, singleLine = false) { form = form.copy(address = it) } }
            item { ProfileField("Phone", form.phone) { form = form.copy(phone = it) } }
            item { ProfileField("Email", form.email) { form = form.copy(email = it) } }
            item { ProfileField("Registration number", form.registrationNumber) { form = form.copy(registrationNumber = it) } }

            item {
                Text(
                    text = "SIGNATURE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .padding(bottom = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (form.signatureImagePath != null) {
                        AsyncImage(
                            model = "file://${form.signatureImagePath}",
                            contentDescription = "Signature",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Default.Draw, contentDescription = null, modifier = Modifier.size(32.dp))
                    }
                }
            }
            item {
                OutlinedButton(onClick = { signaturePicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (form.signatureImagePath != null) "Replace signature image" else "Add signature image")
                }
            }

            item {
                Button(
                    onClick = { viewModel.save(form); onNavigateBack() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save")
                }
            }
        }
    }
}

@Composable
private fun ProfileField(label: String, value: String, singleLine: Boolean = true, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        modifier = Modifier.fillMaxWidth()
    )
}
