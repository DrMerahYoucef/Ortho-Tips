package com.example.ui.screens.main

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.di.AppModule
import com.example.ui.screens.favorites.FavoritesScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.patients.PatientListScreen
import com.example.ui.screens.search.SearchScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.viewmodel.ClassificationViewModel
import com.example.viewmodel.HomeViewModel
import com.example.viewmodel.PatientListViewModel
import com.example.viewmodel.SavedViewModel
import com.example.viewmodel.SearchViewModel
import com.example.viewmodel.SettingsViewModel

enum class NavigationItem(
    val title: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val testTag: String
) {
    STUDIES("Studies", Icons.Filled.MenuBook, Icons.Outlined.MenuBook, "nav_home"),
    PATIENTS("Patients", Icons.Filled.People, Icons.Outlined.PeopleOutline, "nav_patients"),
    SEARCH("Search", Icons.Filled.Search, Icons.Outlined.Search, "nav_search"),
    SAVED("Saved", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder, "nav_saved"),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings")
}

@Composable
fun MainScreen(
    appModule: AppModule,
    onNavigateToChapter: (String, String) -> Unit,
    onNavigateToBoneDetail: (String) -> Unit,
    onNavigateToImport: () -> Unit,
    onNavigateToClassificationChapter: (String) -> Unit,
    onNavigateToPatientDetail: (String) -> Unit,
    onNavigateToAddPatient: () -> Unit,
    onNavigateToSurgeonProfile: () -> Unit,
    onNavigateToDataTransfer: () -> Unit,
    onNavigateToQrScanner: () -> Unit,
    onNavigateToTelegramDrive: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedItem by rememberSaveable { mutableStateOf(NavigationItem.STUDIES) }

    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.provideFactory(appModule.orthoRepository)
    )
    val classificationViewModel: ClassificationViewModel = viewModel(
        factory = ClassificationViewModel.provideFactory(appModule.classificationRepository)
    )
    val searchViewModel: SearchViewModel = viewModel(
        factory = SearchViewModel.provideFactory(appModule.orthoRepository)
    )
    val savedViewModel: SavedViewModel = viewModel(
        factory = SavedViewModel.provideFactory(appModule.orthoRepository)
    )
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.provideFactory(
            appModule.settingsRepository,
            appModule.backupManager,
            appModule.orthoRepository
        )
    )
    val patientListViewModel: PatientListViewModel = viewModel(
        factory = PatientListViewModel.provideFactory(appModule.patientRepository)
    )
    val dataTransferViewModel: com.example.viewmodel.PatientDataTransferViewModel = viewModel(
        factory = com.example.viewmodel.PatientDataTransferViewModel.provideFactory(appModule.patientRepository)
    )
    val mainScreenContext = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        bottomBar = {
            Surface(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier.testTag("main_bottom_nav_container")
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    modifier = Modifier.testTag("main_bottom_nav")
                ) {
                    NavigationItem.entries.forEach { item ->
                        val isSelected = selectedItem == item
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { selectedItem = item },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Crossfade(
            targetState = selectedItem,
            label = "tab_crossfade",
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { currentTab ->
            when (currentTab) {
                NavigationItem.STUDIES -> HomeScreen(
                    viewModel = homeViewModel,
                    classificationViewModel = classificationViewModel,
                    onNavigateToChapter = onNavigateToChapter,
                    onNavigateToBoneDetail = onNavigateToBoneDetail,
                    onNavigateToSearch = { selectedItem = NavigationItem.SEARCH },
                    onNavigateToImport = onNavigateToImport,
                    onNavigateToClassificationChapter = onNavigateToClassificationChapter
                )
                NavigationItem.PATIENTS -> PatientListScreen(
                    viewModel = patientListViewModel,
                    onNavigateToPatientDetail = onNavigateToPatientDetail,
                    onNavigateToAddPatient = onNavigateToAddPatient,
                    onNavigateToSurgeonProfile = onNavigateToSurgeonProfile,
                    onNavigateToDataTransfer = onNavigateToDataTransfer,
                    onNavigateToQrScanner = onNavigateToQrScanner,
                    onExportSelected = { ids -> dataTransferViewModel.exportPatients(mainScreenContext, ids) }
                )
                NavigationItem.SEARCH -> SearchScreen(
                    viewModel = searchViewModel,
                    onNavigateToBoneDetail = onNavigateToBoneDetail
                )
                NavigationItem.SAVED -> FavoritesScreen(
                    viewModel = savedViewModel,
                    onNavigateToBoneDetail = onNavigateToBoneDetail
                )
                NavigationItem.SETTINGS -> SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateToImport = onNavigateToImport,
                    onNavigateToTelegramDrive = onNavigateToTelegramDrive
                )
            }
        }
    }
}