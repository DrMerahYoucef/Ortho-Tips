package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MedicalInformation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.model.ChapterRef
import com.example.data.local.model.ClassificationBlock
import com.example.data.local.model.ClassificationChapter
import com.example.data.local.model.ClassificationEntry
import com.example.data.repository.ClassificationRepository

private data class ClassificationChoice(
    val label: String,
    val detail: String? = null
)

@Composable
fun ClassificationPickerButton(
    repository: ClassificationRepository,
    onClassificationSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { showPicker = true },
        modifier = modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.Add, contentDescription = null)
        Text("Add classification", modifier = Modifier.padding(start = 8.dp))
    }

    if (showPicker) {
        ClassificationPickerDialog(
            repository = repository,
            onDismiss = { showPicker = false },
            onClassificationSelected = {
                onClassificationSelected(it)
                showPicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClassificationPickerDialog(
    repository: ClassificationRepository,
    onDismiss: () -> Unit,
    onClassificationSelected: (String) -> Unit
) {
    var chapters by remember { mutableStateOf<List<ChapterRef>>(emptyList()) }
    var selectedChapter by remember { mutableStateOf<ClassificationChapter?>(null) }
    var selectedChapterRef by remember { mutableStateOf<ChapterRef?>(null) }
    var selectedPhrase by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            chapters = repository.getIndex().chapters
        } catch (error: Exception) {
            errorMessage = error.message ?: "Unable to load classifications."
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(selectedChapterRef) {
        val chapterRef = selectedChapterRef ?: return@LaunchedEffect
        isLoading = true
        errorMessage = null
        try {
            selectedChapter = repository.getChapter(chapterRef.file)
        } catch (error: Exception) {
            selectedChapter = null
            errorMessage = error.message ?: "Unable to load this chapter."
        } finally {
            isLoading = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            selectedChapter?.title ?: "Choose a classification",
                            maxLines = 2
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (selectedChapterRef != null) {
                                    selectedChapterRef = null
                                    selectedChapter = null
                                    selectedPhrase = null
                                } else onDismiss()
                            }
                        ) {
                            Icon(
                                imageVector = if (selectedChapter == null) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = if (selectedChapter == null) "Close" else "Back"
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            when {
                isLoading -> Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                errorMessage != null -> Text(
                    text = errorMessage ?: "Unable to load classifications.",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(paddingValues).padding(24.dp)
                )

                selectedChapter == null -> ChapterChoices(
                    chapters = chapters,
                    paddingValues = paddingValues,
                    onChapterSelected = { chapter ->
                        selectedChapterRef = chapter
                    }
                )

                else -> ClassificationChoices(
                    chapter = selectedChapter!!,
                    paddingValues = paddingValues,
                    selectedPhrase = selectedPhrase,
                    onChoiceSelected = { selectedPhrase = it },
                    onConfirm = {
                        selectedPhrase?.let(onClassificationSelected)
                    }
                )
            }
        }
    }
}

@Composable
private fun ChapterChoices(
    chapters: List<ChapterRef>,
    paddingValues: PaddingValues,
    onChapterSelected: (ChapterRef) -> Unit
) {
    LazyColumn(
        contentPadding = paddingValues,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
    ) {
        items(chapters, key = { it.id }) { chapter ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onChapterSelected(chapter) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.MedicalInformation, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(chapter.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "${chapter.classificationCount} classifications",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClassificationChoices(
    chapter: ClassificationChapter,
    paddingValues: PaddingValues,
    selectedPhrase: String?,
    onChoiceSelected: (String) -> Unit,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        LazyColumn(
            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            chapter.sections.forEach { section ->
                item {
                    Text(
                        section.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }

                items(section.classifications, key = { it.id }) { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = entry.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                            )
                            choicesForEntry(entry).forEach { choice ->
                                val classificationName = classificationName(entry.title)
                                val phrase = "${choice.label} selon la classification de $classificationName"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onChoiceSelected(phrase) }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedPhrase == phrase,
                                        onClick = { onChoiceSelected(phrase) }
                                    )
                                    Text(
                                        text = phrase,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = onConfirm,
            enabled = selectedPhrase != null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text("Confirm selection")
        }
    }
}

private fun choicesForEntry(entry: ClassificationEntry): List<ClassificationChoice> {
    val choices = buildList {
        entry.blocks.forEach { block ->
            when (block.type) {
                "item" -> {
                    block.label?.takeIf { it.isNotBlank() }?.let { add(ClassificationChoice(it, block.text)) }
                    block.sub.forEach { add(ClassificationChoice(it.label)) }
                }
                "table" -> block.rows.forEach { row ->
                    row.firstOrNull()?.takeIf { it.isNotBlank() }?.let { add(ClassificationChoice(it)) }
                }
            }
        }
    }
    return choices.distinctBy { it.label }.ifEmpty { listOf(ClassificationChoice(entry.title)) }
}

private fun classificationName(title: String): String {
    val normalized = title
        .replace(Regex("(?i)^la classification de "), "")
        .replace(Regex("(?i)^classification de "), "")
        .replace(Regex("(?i)^les classifications radiologiques de "), "")
        .replace(Regex("(?i)^cotation fonctionnelle de "), "")
        .substringBefore("(")
        .trim()
    return normalized.ifBlank { title }
}
