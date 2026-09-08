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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.example.ui.screens.classifications.ClassificationChapterDetailRoute
import com.example.ui.screens.importer.ImportScreen
import com.example.ui.screens.main.MainScreen
import com.example.ui.screens.patients.ImageAnnotationScreen
import com.example.ui.screens.patients.PatientCameraCaptureScreen
import com.example.ui.screens.patients.PatientDataTransferScreen
import com.example.ui.screens.patients.PatientDetailScreen
import com.example.ui.screens.patients.PatientEditScreen
import com.example.ui.screens.patients.QrScannerScreen
import com.example.ui.screens.patients.SurgeryEditScreen
import com.example.ui.screens.patients.SurgeonProfileScreen
import com.example.ui.screens.patients.TimelineEventEditScreen
import com.example.ui.screens.splash.SplashScreen
import com.example.ui.screens.viewer.ImageViewerScreen
import com.example.ui.theme.OrthoTipsTheme
import com.example.utils.ProtocolLanguage
import com.example.utils.rememberDocumentScannerLauncher
import com.example.data.local.entity.ImageCategory
import com.example.viewmodel.AppViewModel
import com.example.viewmodel.BoneDetailViewModel
import com.example.viewmodel.ClassificationViewModel
import com.example.viewmodel.ImageAnnotationViewModel
import com.example.viewmodel.ImportViewModel
import com.example.viewmodel.MediaCaptureViewModel
import com.example.viewmodel.OperativeProtocolViewModel
import com.example.viewmodel.PatientDataTransferViewModel
import com.example.viewmodel.PatientDetailViewModel
import com.example.viewmodel.PatientEditViewModel
import com.example.viewmodel.SurgeonProfileViewModel
import com.example.viewmodel.SurgeryEditViewModel
import com.example.viewmodel.TimelineEventEditViewModel
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
                OrthoTipsTheme(
                    darkTheme = isDarkTheme,
                    dynamicColor = settings.dynamicColor
                ) {
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
                },
                onNavigateToClassificationChapter = { chapterFile ->
                    navController.navigate(OrthoDestinations.classificationChapterDetail(chapterFile))
                },
                onNavigateToPatientDetail = { patientId ->
                    navController.navigate(OrthoDestinations.patientDetail(patientId))
                },
                onNavigateToAddPatient = {
                    navController.navigate(OrthoDestinations.patientEdit())
                },
                onNavigateToSurgeonProfile = {
                    navController.navigate(OrthoDestinations.SURGEON_PROFILE)
                },
                onNavigateToDataTransfer = {
                    navController.navigate(OrthoDestinations.PATIENT_DATA_IMPORT_EXPORT)
                },
                onNavigateToQrScanner = {
                    navController.navigate(OrthoDestinations.QR_SCANNER)
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

        composable(
            route = OrthoDestinations.CLASSIFICATION_CHAPTER_DETAIL,
            arguments = listOf(
                navArgument("chapterFile") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val chapterFile = backStackEntry.arguments?.getString("chapterFile")?.let { Uri.decode(it) } ?: ""
            val classificationDetailViewModel: ClassificationViewModel = viewModel(
                key = "classification_detail_$chapterFile",
                factory = ClassificationViewModel.provideFactory(appModule.classificationRepository)
            )
            ClassificationChapterDetailRoute(
                viewModel = classificationDetailViewModel,
                repository = appModule.classificationRepository,
                chapterFile = chapterFile,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = OrthoDestinations.PATIENT_DETAIL,
            arguments = listOf(navArgument("patientId") { type = NavType.StringType })
        ) { backStackEntry ->
            val patientId = backStackEntry.arguments?.getString("patientId")?.let { Uri.decode(it) } ?: ""
            val detailViewModel: PatientDetailViewModel = viewModel(
                key = "patient_detail_$patientId",
                factory = PatientDetailViewModel.provideFactory(appModule.patientRepository, patientId)
            )
            val protocolContext = LocalContext.current
            val protocolViewModel: OperativeProtocolViewModel = viewModel(
                factory = OperativeProtocolViewModel.provideFactory(
                    appModule.patientRepository,
                    appModule.surgeonProfileRepository,
                    appModule.pdfOptionsRepository
                )
            )
            val savedSurgeryPdfOptions by protocolViewModel.savedSurgeryOptions.collectAsStateWithLifecycle()
            val savedChartPdfOptions by protocolViewModel.savedChartOptions.collectAsStateWithLifecycle()
            PatientDetailScreen(
                viewModel = detailViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { navController.navigate(OrthoDestinations.patientEdit(patientId)) },
                onNavigateToAddTimelineEvent = { navController.navigate(OrthoDestinations.timelineEventEdit(patientId)) },
                onNavigateToEditTimelineEvent = { eventId ->
                    navController.navigate(OrthoDestinations.timelineEventEdit(patientId, eventId))
                },
                onNavigateToAddSurgery = { navController.navigate(OrthoDestinations.surgeryEdit(patientId)) },
                onNavigateToEditSurgery = { surgeryId ->
                    navController.navigate(OrthoDestinations.surgeryEdit(patientId, surgeryId))
                },
                onNavigateToCapture = { navController.navigate(OrthoDestinations.mediaCapture(patientId)) },
                onNavigateToAnnotate = { imageId -> navController.navigate(OrthoDestinations.imageAnnotate(imageId)) },
                savedSurgeryPdfOptions = savedSurgeryPdfOptions,
                savedChartPdfOptions = savedChartPdfOptions,
                onGenerateProtocol = { surgeryId, language, anonymize, options ->
                    protocolViewModel.generateAndShare(protocolContext, patientId, surgeryId, language, anonymize, options)
                },
                onGenerateChartPdf = { anonymize, options ->
                    protocolViewModel.generatePatientChart(protocolContext, patientId, anonymize, options)
                }
            )
        }

        composable(
            route = OrthoDestinations.PATIENT_EDIT,
            arguments = listOf(
                navArgument("patientId") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val patientId = backStackEntry.arguments?.getString("patientId")?.let { Uri.decode(it) }
            val editViewModel: PatientEditViewModel = viewModel(
                key = "patient_edit_${patientId ?: "new"}",
                factory = PatientEditViewModel.provideFactory(appModule.patientRepository, appModule.suggestionRepository, patientId)
            )
            PatientEditScreen(
                viewModel = editViewModel,
                classificationRepository = appModule.classificationRepository,
                onNavigateBack = { navController.popBackStack() },
                onSaved = { savedId ->
                    navController.popBackStack()
                    if (patientId == null) {
                        // created from the list FAB: land straight on the new profile
                        navController.navigate(OrthoDestinations.patientDetail(savedId))
                    }
                }
            )
        }

        composable(
            route = OrthoDestinations.TIMELINE_EVENT_EDIT,
            arguments = listOf(
                navArgument("patientId") { type = NavType.StringType },
                navArgument("eventId") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val patientId = backStackEntry.arguments?.getString("patientId")?.let { Uri.decode(it) } ?: ""
            val eventId = backStackEntry.arguments?.getString("eventId")?.let { Uri.decode(it) }

            val existingEventState = androidx.compose.runtime.produceState<com.example.data.local.entity.TimelineEventEntity?>(
                initialValue = null,
                key1 = eventId
            ) {
                value = eventId?.let { appModule.patientRepository.getTimelineEvent(it) }
            }
            // Wait for the (possibly null) lookup so an edit doesn't briefly render as "new".
            if (eventId != null && existingEventState.value == null) return@composable

            val editViewModel: TimelineEventEditViewModel = viewModel(
                key = "timeline_edit_${eventId ?: "new"}_$patientId",
                factory = TimelineEventEditViewModel.provideFactory(appModule.patientRepository, patientId, existingEventState.value)
            )
            TimelineEventEditScreen(
                viewModel = editViewModel,
                onNavigateBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(
            route = OrthoDestinations.SURGERY_EDIT,
            arguments = listOf(
                navArgument("patientId") { type = NavType.StringType },
                navArgument("surgeryId") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val patientId = backStackEntry.arguments?.getString("patientId")?.let { Uri.decode(it) } ?: ""
            val surgeryId = backStackEntry.arguments?.getString("surgeryId")?.let { Uri.decode(it) }

            val existingSurgeryState = androidx.compose.runtime.produceState<com.example.data.local.entity.SurgeryEntity?>(
                initialValue = null,
                key1 = surgeryId
            ) {
                value = surgeryId?.let { appModule.patientRepository.getSurgery(it) }
            }
            if (surgeryId != null && existingSurgeryState.value == null) return@composable

            val editViewModel: SurgeryEditViewModel = viewModel(
                key = "surgery_edit_${surgeryId ?: "new"}_$patientId",
                factory = SurgeryEditViewModel.provideFactory(appModule.patientRepository, appModule.suggestionRepository, patientId, existingSurgeryState.value)
            )
            SurgeryEditScreen(
                viewModel = editViewModel,
                classificationRepository = appModule.classificationRepository,
                onNavigateBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(
            route = OrthoDestinations.MEDIA_CAPTURE,
            arguments = listOf(navArgument("patientId") { type = NavType.StringType })
        ) { backStackEntry ->
            val patientId = backStackEntry.arguments?.getString("patientId")?.let { Uri.decode(it) } ?: ""
            val context = LocalContext.current
            val captureViewModel: MediaCaptureViewModel = viewModel(
                key = "media_capture_$patientId",
                factory = MediaCaptureViewModel.provideFactory(appModule.patientRepository, patientId)
            )
            val captureState by captureViewModel.uiState.collectAsStateWithLifecycle()

            val launchScanner = rememberDocumentScannerLauncher { scannedUris ->
                captureViewModel.saveCapturedImages(context, scannedUris, ImageCategory.DOCUMENT_SCAN)
            }

            LaunchedEffect(captureState.isSaving, captureState.savedCount, captureState.totalToSave) {
                if (!captureState.isSaving && captureState.totalToSave > 0 && captureState.savedCount == captureState.totalToSave) {
                    navController.popBackStack()
                }
            }

            PatientCameraCaptureScreen(
                onNavigateBack = { navController.popBackStack() },
                onPhotoCaptured = { uri, category ->
                    captureViewModel.saveCapturedImages(context, listOf(uri), category)
                },
                onOpenScanner = launchScanner
            )
        }

        composable(
            route = OrthoDestinations.IMAGE_ANNOTATE,
            arguments = listOf(navArgument("imageId") { type = NavType.StringType })
        ) { backStackEntry ->
            val imageId = backStackEntry.arguments?.getString("imageId")?.let { Uri.decode(it) } ?: ""
            val annotationViewModel: ImageAnnotationViewModel = viewModel(
                key = "annotate_$imageId",
                factory = ImageAnnotationViewModel.provideFactory(appModule.patientRepository, imageId)
            )
            ImageAnnotationScreen(
                viewModel = annotationViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(OrthoDestinations.QR_SCANNER) {
            QrScannerScreen(
                onNavigateBack = { navController.popBackStack() },
                onPatientFound = { patientId ->
                    navController.navigate(OrthoDestinations.patientDetail(patientId)) {
                        popUpTo(OrthoDestinations.QR_SCANNER) { inclusive = true }
                    }
                }
            )
        }

        composable(OrthoDestinations.SURGEON_PROFILE) {
            val profileViewModel: SurgeonProfileViewModel = viewModel(
                factory = SurgeonProfileViewModel.provideFactory(appModule.surgeonProfileRepository)
            )
            SurgeonProfileScreen(
                viewModel = profileViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(OrthoDestinations.PATIENT_DATA_IMPORT_EXPORT) {
            val transferViewModel: PatientDataTransferViewModel = viewModel(
                factory = PatientDataTransferViewModel.provideFactory(appModule.patientRepository)
            )
            PatientDataTransferScreen(
                viewModel = transferViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}