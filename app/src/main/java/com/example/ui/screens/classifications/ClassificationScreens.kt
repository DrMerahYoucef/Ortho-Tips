package com.example.ui.screens.classifications

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MedicalInformation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.model.ChapterRef
import com.example.data.local.model.ClassificationBlock
import com.example.data.local.model.ClassificationChapter
import com.example.data.local.model.ClassificationEntry
import com.example.data.repository.ClassificationRepository
import com.example.viewmodel.ClassificationViewModel

/**
 * Level 1: list of classification chapters, read from assets/classifications/index.json.
 * Meant to be embedded as tab content (e.g. Home > Classifications) — no Scaffold/back button.
 */
@Composable
fun ClassificationChaptersScreen(
    viewModel: ClassificationViewModel,
    onOpenChapter: (ChapterRef) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) { viewModel.loadIndex() }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            viewModel.isLoading && viewModel.chapters.isEmpty() -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            viewModel.errorMessage != null && viewModel.chapters.isEmpty() -> {
                Text(
                    text = viewModel.errorMessage ?: "Something went wrong.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("classification_chapters_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(viewModel.chapters, key = { it.id }) { chapter ->
                        ChapterRefCard(chapter = chapter, onClick = { onOpenChapter(chapter) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterRefCard(chapter: ChapterRef, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("classification_chapter_card_${chapter.id}")
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .width(44.dp)
                    .height(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.MedicalInformation,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chapter.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${chapter.classificationCount} classifications • ${chapter.sectionCount} sections",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

/**
 * Level 2/3 combined: a chapter's sections and their classification entries, as a standalone
 * navigation destination (has its own Scaffold + back button). Loads [chapterFile] into
 * [viewModel] itself, so callers only need to pass the file path from a [ChapterRef].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassificationChapterDetailRoute(
    viewModel: ClassificationViewModel,
    repository: ClassificationRepository,
    chapterFile: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(chapterFile) { viewModel.openChapter(chapterFile) }
    val chapter = viewModel.currentChapter

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = chapter?.title ?: "Classifications",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("classification_detail_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                viewModel.isLoading && chapter == null -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                viewModel.errorMessage != null && chapter == null -> {
                    Text(
                        text = viewModel.errorMessage ?: "Something went wrong.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                    )
                }
                chapter != null -> {
                    ClassificationChapterDetailScreen(chapter = chapter, repository = repository)
                }
            }
        }
    }
}

@Composable
fun ClassificationChapterDetailScreen(
    chapter: ClassificationChapter,
    repository: ClassificationRepository,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("classification_chapter_detail"),
        contentPadding = PaddingValues(16.dp)
    ) {
        chapter.sections.forEach { section ->
            item {
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 12.dp, bottom = 10.dp)
                )
            }
            items(section.classifications, key = { it.id }) { entry ->
                ClassificationEntryCard(
                    entry = entry,
                    repository = repository,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun ClassificationEntryCard(
    entry: ClassificationEntry,
    repository: ClassificationRepository,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("classification_entry_${entry.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            entry.images.forEach { path ->
                val bitmap = remember(path) { repository.loadAssetImage(path) }
                bitmap?.let {
                    Image(
                        bitmap = it,
                        contentDescription = entry.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                    )
                }
            }

            entry.blocks.forEach { block ->
                ClassificationBlockView(block)
            }
        }
    }
}

@Composable
private fun ClassificationBlockView(block: ClassificationBlock) {
    when (block.type) {
        "text" -> Column(modifier = Modifier.padding(vertical = 2.dp)) {
            block.lines.forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }

        "item" -> Column(modifier = Modifier.padding(top = 8.dp)) {
            if (!block.label.isNullOrBlank()) {
                Text(
                    text = block.label + (block.text?.let { " : $it" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            block.bullets.forEach { bullet ->
                Text(
                    text = "• $bullet",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 12.dp, top = 2.dp)
                )
            }
            block.sub.forEach { sub ->
                Text(
                    text = sub.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 12.dp, top = 6.dp)
                )
                sub.bullets.forEach { bullet ->
                    Text(
                        text = "– $bullet",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 24.dp, top = 2.dp)
                    )
                }
            }
        }

        "table" -> Column(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .horizontalScroll(rememberScrollState())
        ) {
            Row {
                block.headers.forEach { header ->
                    Text(
                        text = header,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .width(140.dp)
                            .padding(4.dp)
                    )
                }
            }
            HorizontalDivider()
            block.rows.forEach { row ->
                Row {
                    row.forEach { cell ->
                        Text(
                            text = cell,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .width(140.dp)
                                .padding(4.dp)
                        )
                    }
                }
                HorizontalDivider()
            }
        }

        else -> Unit
    }
}