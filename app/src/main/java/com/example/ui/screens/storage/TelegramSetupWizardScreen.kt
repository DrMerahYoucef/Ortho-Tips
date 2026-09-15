package com.example.ui.screens.storage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.telegram.TelegramAuthState
import com.example.viewmodel.SetupStep
import com.example.viewmodel.SetupUiState
import com.example.viewmodel.TelegramSetupViewModel

/**
 * First-time setup for Telegram Drive.
 *
 * Linear and explicit: the surgeon sees what is about to happen to their patient
 * data at every step, and can leave at any point without anything having been
 * switched over — the backend only changes on the initial-sync step.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelegramSetupWizardScreen(
    viewModel: TelegramSetupViewModel,
    onNavigateBack: () -> Unit,
    onSetupComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Telegram Drive setup") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.cancel()
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            LinearProgressIndicator(
                progress = { (state.step.index + 1f) / SetupStep.total },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            )
            Text(
                text = "Step ${state.step.index + 1} of ${SetupStep.total}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                when (state.step) {
                    SetupStep.WELCOME -> WelcomeStep()
                    SetupStep.REQUIREMENTS -> RequirementsStep()
                    SetupStep.CREDENTIALS -> CredentialsStep(state, viewModel)
                    SetupStep.TELEGRAM_LOGIN -> LoginStep(state, viewModel)
                    SetupStep.NOTIFICATIONS -> PermissionsStep()
                    SetupStep.DRIVE_INITIALISATION -> DriveInitStep(state)
                    SetupStep.STORAGE_VERIFICATION -> VerificationStep(state)
                    SetupStep.CONNECTION_TEST -> ConnectionTestStep(state)
                    SetupStep.MANIFEST -> ManifestStep(state)
                    SetupStep.INITIAL_SYNC -> InitialSyncStep(state)
                    SetupStep.COMPLETE -> CompleteStep(state)
                }

                state.error?.let { error ->
                    Spacer(Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            if (state.statusLine.isNotBlank()) {
                Text(
                    text = state.statusLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.step != SetupStep.WELCOME) {
                    OutlinedButton(
                        onClick = viewModel::back,
                        enabled = !state.busy,
                        modifier = Modifier.weight(1f)
                    ) { Text("Back") }
                }
                Button(
                    onClick = {
                        if (state.step.isLast) onSetupComplete() else viewModel.advance()
                    },
                    enabled = state.canAdvance,
                    modifier = Modifier.weight(1f)
                ) {
                    if (state.busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.width(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(if (state.step.isLast) "Finish" else "Continue")
                    }
                }
            }
        }
    }
}

@Composable
private fun StepHeading(title: String, body: String) {
    Text(text = title, style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(10.dp))
    Text(
        text = body,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun WelcomeStep() {
    StepHeading(
        title = "Store your records in Telegram",
        body = "Telegram Drive gives Ortho Tips a private cloud backup using a channel " +
                "only you can see. Patient photos, radiographs, documents and records are " +
                "copied there so a lost or replaced phone does not mean lost cases.\n\n" +
                "Local storage keeps working exactly as it does today. You can switch back " +
                "at any time, and nothing is ever deleted when you switch."
    )
}

@Composable
private fun RequirementsStep() {
    StepHeading(
        title = "What you need",
        body = "• A Telegram account you already use.\n" +
                "• Your own API id and hash from my.telegram.org — free, takes two minutes.\n" +
                "• A network connection for the first synchronisation.\n\n" +
                "Ortho Tips never sends your credentials anywhere except Telegram itself, " +
                "and stores them sealed with this device's hardware keystore."
    )
    Spacer(Modifier.height(16.dp))
    ClinicalNoteCard(
        "Patient data is medical data. Everything uploaded is encrypted with a key that " +
                "never leaves this device, so even Telegram cannot read the contents."
    )
}

@Composable
private fun CredentialsStep(state: SetupUiState, viewModel: TelegramSetupViewModel) {
    StepHeading(
        title = "Telegram API credentials",
        body = "Sign in at my.telegram.org, open “API development tools”, and create an " +
                "application. Copy the App api_id and App api_hash here."
    )
    Spacer(Modifier.height(20.dp))
    OutlinedTextField(
        value = state.apiId,
        onValueChange = viewModel::onApiId,
        label = { Text("API id") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = state.apiHash,
        onValueChange = viewModel::onApiHash,
        label = { Text("API hash") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun LoginStep(state: SetupUiState, viewModel: TelegramSetupViewModel) {
    StepHeading(
        title = "Sign in to Telegram",
        body = "Ortho Tips signs in the same way an official Telegram client does."
    )
    Spacer(Modifier.height(20.dp))

    when (val auth = state.authState) {
        is TelegramAuthState.WaitingForPhoneNumber -> {
            OutlinedTextField(
                value = state.phoneNumber,
                onValueChange = viewModel::onPhoneNumber,
                label = { Text("Phone number with country code") },
                placeholder = { Text("+33…") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = viewModel::submitPhoneNumber,
                enabled = state.phoneNumber.length > 5 && !state.busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Send code") }
            TextButton(onClick = viewModel::useQrLogin, modifier = Modifier.fillMaxWidth()) {
                Text("Sign in by QR code instead")
            }
        }

        is TelegramAuthState.WaitingForCode -> {
            Text(
                text = "Code sent via ${auth.channel.name.lowercase().replace('_', ' ')}.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.code,
                onValueChange = viewModel::onCode,
                label = { Text("Login code") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = viewModel::submitCode,
                enabled = state.code.isNotBlank() && !state.busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Verify") }
            TextButton(onClick = viewModel::resendCode, modifier = Modifier.fillMaxWidth()) {
                Text("Resend code")
            }
        }

        is TelegramAuthState.WaitingForPassword -> {
            auth.passwordHint?.let {
                Text(
                    text = "Hint: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
            }
            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::onPassword,
                label = { Text("Two-step verification password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = viewModel::submitPassword,
                enabled = state.password.isNotBlank() && !state.busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Sign in") }
        }

        is TelegramAuthState.WaitingForEmailAddress -> {
            OutlinedTextField(
                value = state.email,
                onValueChange = viewModel::onEmail,
                label = { Text("Login email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = viewModel::submitEmail,
                enabled = state.email.contains('@') && !state.busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Send email code") }
        }

        is TelegramAuthState.WaitingForEmailCode -> {
            Text(
                text = "Code sent to ${auth.emailPattern}.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.code,
                onValueChange = viewModel::onCode,
                label = { Text("Email code") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = viewModel::submitEmailCode,
                enabled = state.code.isNotBlank() && !state.busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Verify") }
        }

        is TelegramAuthState.WaitingForQrScan -> {
            Text(
                text = "Open Telegram on another device, go to Settings → Devices → Link " +
                        "Desktop Device, and scan this link.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(12.dp))
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = auth.link,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(14.dp)
                )
            }
            TextButton(onClick = viewModel::usePhoneLogin, modifier = Modifier.fillMaxWidth()) {
                Text("Use a phone number instead")
            }
        }

        TelegramAuthState.RegistrationRequired -> ClinicalNoteCard(
            "That number has no Telegram account. Create one in the official Telegram app " +
                    "first, then come back."
        )

        TelegramAuthState.Ready -> SuccessNote("Signed in as ${state.accountName.ifBlank { "your account" }}.")

        is TelegramAuthState.Failed -> ClinicalNoteCard(auth.message)

        else -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.width(20.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(12.dp))
            Text("Connecting to Telegram…", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PermissionsStep() {
    StepHeading(
        title = "Notifications",
        body = "Ortho Tips shows a notification while large transfers run, so you can see " +
                "progress and cancel without keeping the app open.\n\n" +
                "Android will ask for permission the first time a transfer starts. Declining " +
                "is fine — synchronisation still works, you just will not see progress."
    )
}

@Composable
private fun DriveInitStep(state: SetupUiState) {
    StepHeading(
        title = "Create the drive",
        body = "Ortho Tips will look for an existing “Ortho Tips Drive” channel on your " +
                "account and create a private one if there is none. It is never public and " +
                "has no members other than you."
    )
    if (state.chatId != null) {
        Spacer(Modifier.height(16.dp))
        SuccessNote(
            if (state.existingDriveFound) "Found your existing drive."
            else "Private drive channel created."
        )
    }
}

@Composable
private fun VerificationStep(state: SetupUiState) {
    StepHeading(
        title = "Storage check",
        body = "Confirms this device has room for the local cache and prepares the " +
                "encryption key used for uploads."
    )
    if (state.verifiedBytes > 0) {
        Spacer(Modifier.height(16.dp))
        SuccessNote("${state.verifiedBytes / (1024 * 1024)} MB free on this device.")
    }
}

@Composable
private fun ConnectionTestStep(state: SetupUiState) {
    StepHeading(
        title = "Connection test",
        body = "Checks that Telegram accepts uploads from this account and reports the " +
                "per-file size limit that applies to you."
    )
    if (state.statusLine.startsWith("Connected")) {
        Spacer(Modifier.height(16.dp))
        SuccessNote(state.statusLine)
    }
}

@Composable
private fun ManifestStep(state: SetupUiState) {
    StepHeading(
        title = "Reading the drive",
        body = "Every object in the drive carries its own description, so Ortho Tips can " +
                "tell what is already there without any record on this device. This is what " +
                "makes the drive a real backup rather than a mirror."
    )
    if (state.manifestEntries > 0) {
        Spacer(Modifier.height(16.dp))
        SuccessNote("${state.manifestEntries} objects already in this drive.")
    }
}

@Composable
private fun InitialSyncStep(state: SetupUiState) {
    StepHeading(
        title = "First synchronisation",
        body = "Your existing patient media and records will be queued for upload. Local " +
                "copies stay exactly where they are — nothing is moved or removed."
    )
    state.initialSyncReport?.let { report ->
        Spacer(Modifier.height(16.dp))
        SuccessNote(
            "${report.uploadsQueued} objects queued for upload, " +
                    "${report.downloadsQueued} for download."
        )
    }
}

@Composable
private fun CompleteStep(state: SetupUiState) {
    StepHeading(
        title = "Telegram Drive is on",
        body = "Transfers run in the background under the network and battery rules you set " +
                "in Settings → Telegram Drive. You can switch back to local storage there at " +
                "any time."
    )
    state.initialSyncReport?.let {
        Spacer(Modifier.height(16.dp))
        SuccessNote("Queued ${it.uploadsQueued} uploads. Progress is shown in Settings.")
    }
}

@Composable
private fun SuccessNote(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(10.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ClinicalNoteCard(text: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(14.dp)
        )
    }
}
