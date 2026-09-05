package com.example.ui.screens.favorites

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.NoteEntity
import com.example.ui.components.BoneRow
import com.example.ui.components.EmptyStateView
import com.example.ui.components.NoteDialog
import com.example.viewmodel.SavedTab
import com.example.viewmodel.SavedViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    viewModel: SavedViewModel,
    onNavigateToBoneDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isAddNoteDialogOpen = uiState.isAddNoteDialogOpen
    val noteToEdit = uiState.noteToEdit

    if (isAddNoteDialogOpen) {
        NoteDialog(
            initialNote = noteToEdit,
            onDismiss = { viewModel.closeNoteDialog() },
            onSave = { note -> viewModel.saveNote(note) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Saved & Clinical Notes",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-0.3).sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (uiState.currentTab == SavedTab.NOTES) {
                FloatingActionButton(
                    onClick = { viewModel.openAddNoteDialog() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("add_standalone_note_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Case Note"
                    )
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = if (uiState.currentTab == SavedTab.BOOKMARKS) 0 else 1,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = uiState.currentTab == SavedTab.BOOKMARKS,
                    onClick = { viewModel.selectTab(SavedTab.BOOKMARKS) },
                    text = { Text(text = "Bookmarks (${uiState.bookmarks.size})") }
                )
                Tab(
                    selected = uiState.currentTab == SavedTab.NOTES,
                    onClick = { viewModel.selectTab(SavedTab.NOTES) },
                    text = { Text(text = "Clinical Notes (${uiState.notes.size})") }
                )
            }

            when (uiState.currentTab) {
                SavedTab.BOOKMARKS -> {
                    if (uiState.bookmarks.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.BookmarkBorder,
                            title = "No Bookmarks Yet",
                            message = "Save high-yield fracture guides and surgical approaches for fast reference during rounds or in the OR."
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("bookmarked_bones_list")
                        ) {
                            items(uiState.bookmarks, key = { it.id }) { bone ->
                                BoneRow(
                                    bone = bone,
                                    onClick = { onNavigateToBoneDetail(bone.id) },
                                    onBookmarkToggle = { viewModel.toggleBookmark(bone.id) }
                                )
                            }
                        }
                    }
                }
                SavedTab.NOTES -> {
                    if (uiState.notes.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.Description,
                            title = "No Case Notes Recorded",
                            message = "Tap the + button below to log operative notes, construct sizes, or clinical reminders."
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("saved_notes_list")
                        ) {
                            items(uiState.notes, key = { it.id }) { note ->
                                NoteListItem(
                                    note = note,
                                    onEdit = { viewModel.openEditNoteDialog(note) },
                                    onDelete = { viewModel.deleteNote(note.id) },
                                    onBoneClick = { boneId ->
                                        if (boneId != null) onNavigateToBoneDetail(boneId)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteListItem(
    note: NoteEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onBoneClick: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val tagColor = when (note.colorTag) {
        "amber" -> Color(0xFFA6720A)
        "blue" -> Color(0xFF2E6B9E)
        "purple" -> Color(0xFF6A4C93)
        "crimson" -> Color(0xFFBA1A1A)
        else -> Color(0xFF00677D)
    }

    val dateFormatter = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
    val formattedDate = dateFormatter.format(Date(note.updatedAt))

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .testTag("note_card_${note.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = tagColor,
                        modifier = Modifier.size(10.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit note",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete note",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (note.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 10.sp
                )

                if (note.boneId != null) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.clickable { onBoneClick(note.boneId) }
                    ) {
                        Text(
                            text = "Linked Procedure",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}
