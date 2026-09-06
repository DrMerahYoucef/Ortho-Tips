package com.example.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsKabaddi
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.model.ChapterWithCount
import com.example.ui.components.BoneRow
import com.example.ui.components.ClassificationCard
import com.example.ui.components.ClinicalStudyCard
import com.example.viewmodel.ClinicalStudiesTab
import com.example.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToChapter: (String, String) -> Unit,
    onNavigateToBoneDetail: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isSearchExpanded by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_ortho_logo),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Clinical Studies",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    letterSpacing = (-0.3).sp
                                )
                                Text(
                                    text = "Evidence-Based Orthopedics & Techniques",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                                modifier = Modifier
                                    .size(38.dp)
                                    .clickable { isSearchExpanded = !isSearchExpanded }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                                        contentDescription = "Search studies",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clickable(onClick = onNavigateToImport)
                                    .testTag("home_import_top_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.UploadFile,
                                        contentDescription = "Import Database",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                modifier = Modifier.testTag("clinical_studies_top_bar")
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Animated in-screen search bar
            AnimatedVisibility(visible = isSearchExpanded) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search trials, bones, classifications...", style = MaterialTheme.typography.bodyMedium) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotBlank()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("studies_search_input")
                    )
                }
            }

            // Clinical Studies Sub-Tabs
            PrimaryTabRow(
                selectedTabIndex = uiState.activeTab.ordinal,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                ClinicalStudiesTab.entries.forEach { tab ->
                    val isSelected = uiState.activeTab == tab
                    Tab(
                        selected = isSelected,
                        onClick = { viewModel.setActiveTab(tab) },
                        text = {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                    )
                }
            }

            when (uiState.activeTab) {
                ClinicalStudiesTab.OPERATIVE_GUIDES -> {
                    OperativeGuidesView(
                        uiState = uiState,
                        onNavigateToChapter = onNavigateToChapter,
                        onNavigateToBoneDetail = onNavigateToBoneDetail,
                        onNavigateToSearch = onNavigateToSearch,
                        onNavigateToImport = onNavigateToImport,
                        onToggleBookmark = { viewModel.toggleBookmark(it) },
                        onSwitchToLandmark = { viewModel.setActiveTab(ClinicalStudiesTab.LANDMARK_TRIALS) }
                    )
                }

                ClinicalStudiesTab.LANDMARK_TRIALS -> {
                    LandmarkTrialsView(
                        studies = uiState.filteredLandmarkStudies,
                        currentSpecialty = uiState.specialtyFilter,
                        onSpecialtySelected = { viewModel.setSpecialtyFilter(it) }
                    )
                }

                ClinicalStudiesTab.CLASSIFICATIONS -> {
                    ClassificationsView(
                        classifications = uiState.filteredClassifications
                    )
                }
            }
        }
    }
}

@Composable
private fun OperativeGuidesView(
    uiState: com.example.viewmodel.HomeUiState,
    onNavigateToChapter: (String, String) -> Unit,
    onNavigateToBoneDetail: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToImport: () -> Unit,
    onToggleBookmark: (String) -> Unit,
    onSwitchToLandmark: () -> Unit
) {
    if (uiState.isDatabaseEmpty) {
        EmptyDatabaseState(
            onImportClick = onNavigateToImport,
            onBrowseLandmarkClick = onSwitchToLandmark,
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_empty_state")
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_scroll_container"),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            HomeHeroBanner(
                totalProcedures = uiState.totalProceduresCount,
                totalBookmarks = uiState.bookmarkedCount,
                totalNotes = uiState.noteCount,
                onSearchClick = onNavigateToSearch
            )
        }

        item {
            SectionHeader(
                title = "ANATOMIC CHAPTERS",
                trailing = "${uiState.chapters.size} sections"
            )
        }

        items(uiState.chapters, key = { it.chapter.id }) { item ->
            val index = uiState.chapters.indexOf(item)
            ChapterCard(
                chapterWithCount = item,
                index = index,
                onClick = { onNavigateToChapter(item.chapter.id, item.chapter.title) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
            )
        }

        if (uiState.highYieldBones.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, top = 24.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(8.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "HIGH-YIELD OPERATIVE GUIDES",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(uiState.highYieldBones, key = { it.id }) { bone ->
                BoneRow(
                    bone = bone,
                    onClick = { onNavigateToBoneDetail(bone.id) },
                    onBookmarkToggle = { onToggleBookmark(bone.id) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun LandmarkTrialsView(
    studies: List<com.example.data.local.model.ClinicalStudy>,
    currentSpecialty: String,
    onSpecialtySelected: (String) -> Unit
) {
    val specialties = listOf("All", "Trauma", "Arthroplasty", "Upper Extremity", "Foot & Ankle")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("landmark_trials_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Specialty filter chip row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(specialties) { specialty ->
                    val isSelected = currentSpecialty.equals(specialty, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSpecialtySelected(specialty) },
                        label = { Text(specialty, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LANDMARK ORTHOPEDIC TRIALS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "${studies.size} landmark studies",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (studies.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No trials match your search query.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(studies, key = { it.id }) { study ->
                ClinicalStudyCard(study = study)
            }
        }
    }
}

@Composable
private fun ClassificationsView(
    classifications: List<com.example.data.local.model.ClinicalClassification>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("classifications_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ESSENTIAL CLASSIFICATION SYSTEMS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "${classifications.size} systems",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (classifications.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No classifications match your search query.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(classifications, key = { it.id }) { item ->
                ClassificationCard(classification = item)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, trailing: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = trailing,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun HomeHeroBanner(
    totalProcedures: Int,
    totalBookmarks: Int,
    totalNotes: Int,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("home_hero_card")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                        )
                    )
                )
                .padding(22.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.20f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(start = 10.dp)
                                    .size(13.dp)
                            )
                            Text(
                                text = "OPERATIVE PEARL",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(start = 6.dp, end = 10.dp, top = 5.dp, bottom = 5.dp)
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier
                            .size(36.dp)
                            .clickable(onClick = onSearchClick)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Evidence-Based Fixation Principles",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    lineHeight = 26.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Always align reduction vectors perpendicular to fracture lines before locking screw compression.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.90f)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        StatPill(count = "$totalProcedures", label = "Procedures")
                        StatPill(count = "$totalBookmarks", label = "Saved")
                        StatPill(count = "$totalNotes", label = "Notes")
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White,
                        modifier = Modifier.clickable(onClick = onSearchClick)
                    ) {
                        Text(
                            text = "Explore",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDatabaseState(
    onImportClick: () -> Unit,
    onBrowseLandmarkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.FolderOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "No Anatomical Database Loaded",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Import a JSON database file to load custom bone chapters, surgical operative guides, and anatomical approaches.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable(onClick = onImportClick)
                .testTag("empty_state_import_button")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 13.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.UploadFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Import Database (JSON)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Helpful tip that landmark studies are available
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onBrowseLandmarkClick)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Landmark Trials Ready",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Browse SPRINT, FLOW, DRAFFT & major orthopedic evidence trials right now.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun StatPill(count: String, label: String) {
    Column {
        Text(
            text = count,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.80f)
        )
    }
}

@Composable
private fun ChapterCard(
    chapterWithCount: ChapterWithCount,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chapter = chapterWithCount.chapter
    val icon = when (chapter.icon) {
        "healing" -> Icons.Default.Healing
        "accessibility_new" -> Icons.Default.AccessibilityNew
        "view_agenda" -> Icons.Default.ViewAgenda
        "child_care" -> Icons.Default.ChildCare
        "sports_kabaddi" -> Icons.Default.SportsKabaddi
        else -> Icons.Default.MedicalServices
    }

    val iconBg = when (index % 4) {
        0 -> MaterialTheme.colorScheme.primaryContainer
        1 -> MaterialTheme.colorScheme.tertiaryContainer
        2 -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val iconColor = when (index % 4) {
        0 -> MaterialTheme.colorScheme.onPrimaryContainer
        1 -> MaterialTheme.colorScheme.onTertiaryContainer
        2 -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("chapter_card_${chapter.id}")
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = iconBg,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chapter.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = chapter.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            ) {
                Text(
                    text = "${chapterWithCount.boneCount} guides",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
