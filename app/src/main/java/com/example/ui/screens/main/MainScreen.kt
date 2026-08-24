package com.example.ui.screens.main

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
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
import com.example.ui.screens.search.SearchScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.viewmodel.HomeViewModel
import com.example.viewmodel.SavedViewModel
import com.example.viewmodel.SearchViewModel
import com.example.viewmodel.SettingsViewModel

enum class NavigationItem(
    val title: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val testTag: String
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
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
    modifier: Modifier = Modifier
) {
    var selectedItem by rememberSaveable { mutableStateOf(NavigationItem.HOME) }

    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.provideFactory(appModule.orthoRepository)
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

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
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
                            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier.testTag(item.testTag)
                    )
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
                NavigationItem.HOME -> HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToChapter = onNavigateToChapter,
                    onNavigateToBoneDetail = onNavigateToBoneDetail,
                    onNavigateToSearch = { selectedItem = NavigationItem.SEARCH },
                    onNavigateToImport = onNavigateToImport
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
                    onNavigateToImport = onNavigateToImport
                )
            }
        }
    }
}
