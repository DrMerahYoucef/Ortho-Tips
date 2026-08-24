package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.prefs.FontSizePreference
import com.example.data.prefs.ThemeMode
import com.example.di.AppModule
import com.example.navigation.OrthoDestinations
import com.example.ui.screens.bone.BoneDetailScreen
import com.example.ui.screens.bone.BoneListScreen
import com.example.ui.screens.importer.ImportScreen
import com.example.ui.screens.main.MainScreen
import com.example.ui.screens.splash.SplashScreen
import com.example.ui.screens.viewer.ImageViewerScreen
import com.example.ui.theme.OrthoTipsTheme
import com.example.viewmodel.AppViewModel
import com.example.viewmodel.BoneDetailViewModel
import com.example.viewmodel.ImportViewModel
import com.example.viewmodel.ViewerViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appModule = (application as OrthoApplication).appModule

        setContent {
            val appViewModel: AppViewModel = viewModel(
                factory = AppViewModel.provideFactory(appModule.settingsRepository)
            )
            val settings by appViewModel.settings.collectAsStateWithLifecycle()

            val isDarkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            val fontMultiplier = when (settings.fontSize) {
                FontSizePreference.STANDARD -> 1.0f
                FontSizePreference.LARGE -> 1.15f
                FontSizePreference.EXTRA_LARGE -> 1.30f
            }

            val currentDensity = LocalDensity.current
            val adjustedDensity = Density(
                density = currentDensity.density,
                fontScale = currentDensity.fontScale * fontMultiplier
            )

            CompositionLocalProvider(LocalDensity provides adjustedDensity) {
                OrthoTipsTheme(darkTheme = isDarkTheme) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        OrthoAppNavHost(appModule = appModule)
                    }
                }
            }
        }
    }
}

@Composable
fun OrthoAppNavHost(appModule: AppModule) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = OrthoDestinations.SPLASH
    ) {
        composable(OrthoDestinations.SPLASH) {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate(OrthoDestinations.MAIN) {
                        popUpTo(OrthoDestinations.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(OrthoDestinations.MAIN) {
            MainScreen(
                appModule = appModule,
                onNavigateToChapter = { chapterId, chapterTitle ->
                    navController.navigate(OrthoDestinations.chapterBones(chapterId, chapterTitle))
                },
                onNavigateToBoneDetail = { boneId ->
                    navController.navigate(OrthoDestinations.boneDetail(boneId))
                },
                onNavigateToImport = {
                    navController.navigate(OrthoDestinations.IMPORT)
                }
            )
        }

        composable(
            route = OrthoDestinations.CHAPTER_BONES,
            arguments = listOf(
                navArgument("chapterId") { type = NavType.StringType },
                navArgument("chapterTitle") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val chapterId = backStackEntry.arguments?.getString("chapterId")?.let { Uri.decode(it) } ?: ""
            val chapterTitle = backStackEntry.arguments?.getString("chapterTitle")?.let { Uri.decode(it) } ?: "Chapter"

            BoneListScreen(
                chapterId = chapterId,
                chapterTitle = chapterTitle,
                repository = appModule.orthoRepository,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToBoneDetail = { boneId ->
                    navController.navigate(OrthoDestinations.boneDetail(boneId))
                }
            )
        }

        composable(
            route = OrthoDestinations.BONE_DETAIL,
            arguments = listOf(
                navArgument("boneId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val boneId = backStackEntry.arguments?.getString("boneId")?.let { Uri.decode(it) } ?: ""
            val detailViewModel: BoneDetailViewModel = viewModel(
                key = "bone_detail_$boneId",
                factory = BoneDetailViewModel.provideFactory(
                    appModule.orthoRepository,
                    boneId
                )
            )
            BoneDetailScreen(
                viewModel = detailViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToImageViewer = { imageId ->
                    navController.navigate(OrthoDestinations.imageViewer(imageId))
                }
            )
        }

        composable(
            route = OrthoDestinations.IMAGE_VIEWER,
            arguments = listOf(
                navArgument("imageId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val imageId = backStackEntry.arguments?.getString("imageId")?.let { Uri.decode(it) } ?: ""
            val viewerViewModel: ViewerViewModel = viewModel(
                key = "viewer_$imageId",
                factory = ViewerViewModel.provideFactory(
                    appModule.orthoRepository,
                    imageId
                )
            )
            ImageViewerScreen(
                viewModel = viewerViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(OrthoDestinations.IMPORT) {
            val importViewModel: ImportViewModel = viewModel(
                factory = ImportViewModel.provideFactory(appModule.boneDatabaseImporter)
            )
            ImportScreen(
                viewModel = importViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
