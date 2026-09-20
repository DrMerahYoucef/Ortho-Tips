package com.example.ui.screens.extensions

import android.content.Intent
import android.webkit.WebView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import com.example.R
import com.example.data.extensions.ExtensionRepository
import com.example.data.extensions.OrthoExtension
import com.example.data.local.model.ClassificationBlock
import com.example.data.local.model.ClassificationChapter
import com.example.data.local.model.ClassificationEntry
import com.example.data.local.model.ClassificationSection
import com.example.data.repository.ClassificationRepository
import com.example.data.local.displayTitle
import com.example.ui.screens.classifications.ClassificationChaptersScreen
import com.example.viewmodel.ClassificationViewModel
import com.example.viewmodel.HomeUiState
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionsScreen(
    homeState: HomeUiState,
    classificationViewModel: ClassificationViewModel,
    classificationRepository: ClassificationRepository,
    extensionRepository: ExtensionRepository,
    onOpenChapter: (String) -> Unit,
    onOpenBoneChapter: (String, String) -> Unit,
    onOpenExtension: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var tab by remember { mutableStateOf(0) }
    var showEditor by remember { mutableStateOf(false) }
    var extensions by remember { mutableStateOf(extensionRepository.list()) }
    val context = LocalContext.current
    var exportId by remember { mutableStateOf<String?>(null) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            val installed = extensionRepository.importEncrypted(context.contentResolver.openInputStream(uri)!!.use { it.readBytes() })
            extensions = extensionRepository.list()
            onOpenExtension(installed.id)
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val id = exportId ?: return@rememberLauncherForActivityResult
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.openOutputStream(uri)?.use { it.write(extensionRepository.encryptedBytes(id)) }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Extensions") },
                actions = {
                    IconButton(onClick = { importLauncher.launch(arrayOf(".OTips", "application/octet-stream")) }) {
                        Icon(Icons.Default.FileOpen, contentDescription = "Importer une extension OTips")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            PrimaryTabRow(selectedTabIndex = tab) {
                listOf("Guides opératoires", "Classifications", "Mes extensions").forEachIndexed { index, title ->
                    Tab(selected = tab == index, onClick = { tab = index }, text = { Text(title) })
                }
            }
            when (tab) {
                0 -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item { Text("Guides opératoires intégrés", style = MaterialTheme.typography.titleLarge) }
                    items(homeState.chapters, key = { it.chapter.id }) { chapter ->
                        Card(onClick = { onOpenBoneChapter(chapter.chapter.id, chapter.chapter.displayTitle) }) {
                            Row(Modifier.fillMaxWidth().padding(16.dp)) {
                                Icon(Icons.Default.MenuBook, contentDescription = null)
                                Spacer(Modifier.padding(6.dp))
                                Column { Text(chapter.chapter.displayTitle); Text("${chapter.boneCount} techniques", style = MaterialTheme.typography.bodySmall) }
                            }
                        }
                    }
                }
                1 -> Column(Modifier.fillMaxSize()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.End) {
                        Button(onClick = { showEditor = true }) { Icon(Icons.Default.Add, null); Spacer(Modifier.padding(3.dp)); Text("Nouvelle classification") }
                    }
                    ClassificationChaptersScreen(
                        viewModel = classificationViewModel,
                        onOpenChapter = { onOpenChapter(it.file) }
                    )
                }
                else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        Text("Extensions installées", style = MaterialTheme.typography.titleLarge)
                        Text("Les fichiers .OTips sont chiffrés pour cette application et ne sont pas des HTML lisibles directement.", style = MaterialTheme.typography.bodySmall)
                    }
                    if (extensions.isEmpty()) item { Text("Aucune extension importée.") }
                    items(extensions, key = { it.id }) { extension ->
                        ExtensionCard(extension, onOpen = { onOpenExtension(extension.id) }, onExport = { exportId = extension.id; exportLauncher.launch("${extension.id}.OTips") }, onDelete = { extensionRepository.delete(extension.id); extensions = extensionRepository.list() })
                    }
                }
            }
        }
    }
    if (showEditor) {
        ClassificationEditorDialog(
            repository = classificationRepository,
            onSaved = { classificationViewModel.reloadIndex(); showEditor = false }
        )
    }
}

@Composable
private fun ExtensionCard(extension: OrthoExtension, onOpen: () -> Unit, onExport: () -> Unit, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp)) {
            Text("${extension.icon}  ${extension.name}", style = MaterialTheme.typography.titleMedium)
            Text(extension.description.ifBlank { "Extension locale" }, style = MaterialTheme.typography.bodySmall)
            Text("Version ${extension.version}", style = MaterialTheme.typography.labelSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onOpen) { Icon(Icons.Default.Extension, null); Spacer(Modifier.padding(3.dp)); Text("Ouvrir") }
                OutlinedButton(onClick = onExport) { Icon(Icons.Default.FileDownload, null); Spacer(Modifier.padding(3.dp)); Text("Exporter") }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Supprimer") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClassificationEditorDialog(repository: ClassificationRepository, onSaved: () -> Unit) {
    var chapterTitle by remember { mutableStateOf("") }
    var sectionTitle by remember { mutableStateOf("") }
    var classificationTitle by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var bullets by remember { mutableStateOf("") }
    var style by remember { mutableStateOf("bullet") }
    var expanded by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onSaved,
        title = { Text("Nouvelle classification") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(chapterTitle, { chapterTitle = it }, label = { Text("Titre du chapitre") }, singleLine = true)
                OutlinedTextField(sectionTitle, { sectionTitle = it }, label = { Text("Sous-titre / section") }, singleLine = true)
                OutlinedTextField(classificationTitle, { classificationTitle = it }, label = { Text("Titre de classification") }, singleLine = true)
                OutlinedTextField(subtitle, { subtitle = it }, label = { Text("Sous-titre de l'entrée") }, singleLine = true)
                OutlinedTextField(details, { details = it }, label = { Text("Texte") })
                OutlinedTextField(bullets, { bullets = it }, label = { Text("Un élément par ligne") }, minLines = 3)
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(style, {}, readOnly = true, label = { Text("Liste") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor())
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        listOf("bullet" to "Puces", "number" to "Nombres", "letter" to "Lettres").forEach { (value, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { style = value; expanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val id = "custom-" + UUID.randomUUID().toString().take(8)
                val chapter = ClassificationChapter(id, chapterTitle.ifBlank { "Nouvelle classification" }, listOf(
                    ClassificationSection(sectionTitle.ifBlank { "Généralités" }, sectionTitle.ifBlank { "Généralités" }, listOf(
                        ClassificationEntry(id + "-entry", classificationTitle.ifBlank { "Classification" }, blocks = listOf(
                            ClassificationBlock("item", bulletStyle = style, label = subtitle, text = details, bullets = bullets.lines().filter { it.isNotBlank() })
                        ))
                    ))
                ))
                repository.saveCustomChapter(chapter)
                onSaved()
            }) { Icon(Icons.Default.Save, null); Spacer(Modifier.padding(3.dp)); Text("Enregistrer") }
        },
        dismissButton = { OutlinedButton(onClick = onSaved) { Text("Annuler") } }
    )
}

@Composable
fun ExtensionRunnerScreen(extensionId: String, html: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(topBar = { SmallTopAppBar(title = { Text("Extension") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Retour") } }) }, modifier = modifier) { padding ->
        AndroidView(
            factory = { context -> WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = false
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.blockNetworkLoads = true
                val activity = context as? FragmentActivity ?: return@apply
                val bridge = ExtensionWebBridge(activity, context.getSharedPreferences("ext_data_$extensionId", 0), extensionId)
                addJavascriptInterface(bridge, "OrthoTipsNative")
                loadDataWithBaseURL("https://$extensionId.ext.orthotips.invalid/", hostScript(extensionId) + html, "text/html", "UTF-8", null)
            } },
            update = { it.loadDataWithBaseURL("https://$extensionId.ext.orthotips.invalid/", hostScript(extensionId) + html, "text/html", "UTF-8", null) },
            modifier = Modifier.fillMaxSize().padding(padding)
        )
    }
}

private fun hostScript(extensionId: String): String = """
        <script>
    window.OrthoTips={bridgeVersion:1,extensionId:"$extensionId",extensionVersion:"",appVersion:"",language:"fr",theme:"light",
            storage:{get:function(k){return OrthoTipsNative.get(k)},set:function(k,v){return OrthoTipsNative.set(k,String(v))},remove:function(k){return OrthoTipsNative.remove(k)},keys:function(p){return JSON.parse(OrthoTipsNative.keys(p||""))}},
            toast:function(m){OrthoTipsNative.toast(String(m))},close:function(){OrthoTipsNative.close()},onThemeChange:function(){}}
        """ + "</scr" + "ipt>"
