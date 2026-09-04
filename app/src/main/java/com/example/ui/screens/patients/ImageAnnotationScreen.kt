package com.example.ui.screens.patients

import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.utils.AnnotationShape
import com.example.utils.NormalizedPoint
import com.example.viewmodel.AnnotationTool
import com.example.viewmodel.ImageAnnotationViewModel
import kotlin.math.max
import kotlin.math.min

private val ANNOTATION_COLOR = 0xFFFF3B30

/** Which corner-drag mode is active — they share the same 4-point overlay UI. */
private enum class QuadMode { NONE, CROP, OCR }

/** Displayed bounds of the (letterboxed, `ContentScale.Fit`) image within the canvas Box. */
private data class FitRect(val left: Float, val top: Float, val width: Float, val height: Float)

private fun computeFitRect(canvasW: Float, canvasH: Float, imgW: Int, imgH: Int): FitRect {
    if (imgW <= 0 || imgH <= 0 || canvasW <= 0 || canvasH <= 0) {
        return FitRect(0f, 0f, canvasW, canvasH)
    }
    val scale = min(canvasW / imgW, canvasH / imgH)
    val displayedW = imgW * scale
    val displayedH = imgH * scale
    return FitRect(left = (canvasW - displayedW) / 2f, top = (canvasH - displayedH) / 2f, width = displayedW, height = displayedH)
}

/** Canvas-space point → source-bitmap pixel space, accounting for the Fit letterbox/scale. */
private fun canvasToBitmap(point: Offset, fit: FitRect, imgW: Int, imgH: Int): PointF {
    val fracX = if (fit.width > 0f) ((point.x - fit.left) / fit.width).coerceIn(0f, 1f) else 0f
    val fracY = if (fit.height > 0f) ((point.y - fit.top) / fit.height).coerceIn(0f, 1f) else 0f
    return PointF(fracX * imgW, fracY * imgH)
}

private fun quadCorners(fit: FitRect): List<Offset> = listOf(
    Offset(fit.left, fit.top),                          // top-left
    Offset(fit.left + fit.width, fit.top),               // top-right
    Offset(fit.left + fit.width, fit.top + fit.height),  // bottom-right
    Offset(fit.left, fit.top + fit.height)                // bottom-left
)

private fun nearestHandleIndex(points: List<Offset>, touch: Offset, maxDistancePx: Float): Int? {
    var bestIndex: Int? = null
    var bestDistance = Float.MAX_VALUE
    points.forEachIndexed { index, point ->
        val d = (point - touch).getDistance()
        if (d < bestDistance) {
            bestDistance = d
            bestIndex = index
        }
    }
    return if (bestDistance <= maxDistancePx) bestIndex else null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageAnnotationScreen(
    viewModel: ImageAnnotationViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var canvasSizePx by remember { mutableStateOf(Offset(1f, 1f)) }
    var imageIntrinsicSize by remember(uiState.fileRevision) { mutableStateOf<Pair<Int, Int>?>(null) }
    var pendingTextPosition by remember { mutableStateOf<NormalizedPoint?>(null) }
    var textInput by remember { mutableStateOf("") }
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var freehandPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var showOcrPanel by remember { mutableStateOf(false) }

    var quadMode by remember { mutableStateOf(QuadMode.NONE) }
    var quadPoints by remember { mutableStateOf<List<Offset>?>(null) }
    var activeHandleIndex by remember { mutableStateOf<Int?>(null) }
    var showQuadOcrDialog by remember { mutableStateOf(false) }

    fun normalize(offset: Offset) = NormalizedPoint(
        x = (offset.x / canvasSizePx.x).coerceIn(0f, 1f),
        y = (offset.y / canvasSizePx.y).coerceIn(0f, 1f)
    )

    fun startQuadMode(mode: QuadMode) {
        if (quadMode == mode) {
            quadMode = QuadMode.NONE
            quadPoints = null
            return
        }
        val (imgW, imgH) = imageIntrinsicSize ?: (0 to 0)
        quadMode = mode
        quadPoints = if (imgW > 0 && imgH > 0) {
            quadCorners(computeFitRect(canvasSizePx.x, canvasSizePx.y, imgW, imgH))
        } else {
            // Image size not resolved yet (rare race right after opening) — fall back to
            // the full canvas rather than blocking the tool.
            quadCorners(FitRect(0f, 0f, canvasSizePx.x, canvasSizePx.y))
        }
    }

    fun applyQuad() {
        val points = quadPoints
        val (imgW, imgH) = imageIntrinsicSize ?: (0 to 0)
        if (points != null && imgW > 0 && imgH > 0) {
            val fit = computeFitRect(canvasSizePx.x, canvasSizePx.y, imgW, imgH)
            val bitmapCorners = points.map { canvasToBitmap(it, fit, imgW, imgH) }
            when (quadMode) {
                QuadMode.CROP -> viewModel.cropQuad(context, bitmapCorners)
                QuadMode.OCR -> {
                    viewModel.scanQuadText(bitmapCorners)
                    showQuadOcrDialog = true
                }
                QuadMode.NONE -> {}
            }
        }
        quadMode = QuadMode.NONE
        quadPoints = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Annotate") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = {
                        uiState.filePath?.let { path ->
                            com.example.utils.ShareHelper.shareFile(
                                context = context,
                                file = java.io.File(path),
                                mimeType = "image/jpeg",
                                chooserTitle = "Share image"
                            )
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share image")
                    }
                    IconButton(onClick = { showOcrPanel = true }) {
                        Icon(Icons.Default.TextSnippet, contentDescription = "Scan whole image (OCR)")
                    }
                    IconButton(onClick = { startQuadMode(QuadMode.OCR) }) {
                        Icon(
                            Icons.Default.DocumentScanner,
                            contentDescription = "Scan text from a selected region",
                            tint = if (quadMode == QuadMode.OCR) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { viewModel.rotate90() }) {
                        Icon(Icons.Default.RotateRight, contentDescription = "Rotate")
                    }
                    IconButton(onClick = { startQuadMode(QuadMode.CROP) }) {
                        Icon(
                            Icons.Default.Crop,
                            contentDescription = "Crop",
                            tint = if (quadMode == QuadMode.CROP) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (uiState.hasBackup) {
                        IconButton(onClick = { viewModel.restoreOriginal(context) }) {
                            Icon(Icons.Default.Restore, contentDescription = "Restore original photo")
                        }
                    }
                    TextButton(onClick = { viewModel.saveAnnotations(); onNavigateBack() }) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Text("Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            if (quadMode == QuadMode.NONE) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    ToolButton(Icons.Default.ArrowForward, "Arrow", uiState.activeTool == AnnotationTool.ARROW) {
                        viewModel.setActiveTool(AnnotationTool.ARROW)
                    }
                    ToolButton(Icons.Default.TextFields, "Text", uiState.activeTool == AnnotationTool.TEXT) {
                        viewModel.setActiveTool(AnnotationTool.TEXT)
                    }
                    ToolButton(Icons.Default.CropFree, "Rectangle", uiState.activeTool == AnnotationTool.RECTANGLE) {
                        viewModel.setActiveTool(AnnotationTool.RECTANGLE)
                    }
                    ToolButton(Icons.Default.Gesture, "Lasso", uiState.activeTool == AnnotationTool.FREEHAND) {
                        viewModel.setActiveTool(AnnotationTool.FREEHAND)
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(
                        text = "Drag each corner onto the document's edges.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { quadMode = QuadMode.NONE; quadPoints = null }) { Text("Cancel") }
                        TextButton(onClick = { applyQuad() }, enabled = quadPoints != null) {
                            Text(if (quadMode == QuadMode.OCR) "Scan text" else "Apply crop")
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues).background(Color.Black)) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                return@Scaffold
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(canvasSizePx, uiState.activeTool, quadMode) {
                        if (quadMode != QuadMode.NONE) {
                            detectDragGestures(
                                onDragStart = { start ->
                                    activeHandleIndex = quadPoints?.let { nearestHandleIndex(it, start, maxDistancePx = 72f) }
                                },
                                onDrag = { change, _ ->
                                    val idx = activeHandleIndex ?: return@detectDragGestures
                                    val current = quadPoints ?: return@detectDragGestures
                                    val clamped = Offset(
                                        change.position.x.coerceIn(0f, canvasSizePx.x),
                                        change.position.y.coerceIn(0f, canvasSizePx.y)
                                    )
                                    quadPoints = current.toMutableList().also { it[idx] = clamped }
                                },
                                onDragEnd = { activeHandleIndex = null }
                            )
                        } else {
                            when (uiState.activeTool) {
                                AnnotationTool.ARROW -> detectDragGestures(
                                    onDragStart = { start -> dragStart = start; freehandPoints = listOf(start) },
                                    onDrag = { change, _ -> freehandPoints = listOf(freehandPoints.first(), change.position) },
                                    onDragEnd = {
                                        val start = dragStart
                                        val end = freehandPoints.lastOrNull()
                                        if (start != null && end != null) {
                                            viewModel.addShape(
                                                AnnotationShape.Arrow(
                                                    id = java.util.UUID.randomUUID().toString(),
                                                    colorArgb = ANNOTATION_COLOR,
                                                    start = normalize(start),
                                                    end = normalize(end)
                                                )
                                            )
                                        }
                                        dragStart = null
                                        freehandPoints = emptyList()
                                    }
                                )
                                AnnotationTool.FREEHAND -> detectDragGestures(
                                    onDragStart = { freehandPoints = listOf(it) },
                                    onDrag = { change, _ -> freehandPoints = freehandPoints + change.position },
                                    onDragEnd = {
                                        if (freehandPoints.size > 1) {
                                            viewModel.addShape(
                                                AnnotationShape.Freehand(
                                                    id = java.util.UUID.randomUUID().toString(),
                                                    colorArgb = ANNOTATION_COLOR,
                                                    points = freehandPoints.map { normalize(it) }
                                                )
                                            )
                                        }
                                        freehandPoints = emptyList()
                                    }
                                )
                                AnnotationTool.RECTANGLE -> detectDragGestures(
                                    onDragStart = { dragStart = it; freehandPoints = listOf(it, it) },
                                    onDrag = { change, _ -> dragStart?.let { start -> freehandPoints = listOf(start, change.position) } },
                                    onDragEnd = {
                                        freehandPoints.let { pts ->
                                            if (pts.size == 2) {
                                                val (start, end) = pts
                                                viewModel.addShape(
                                                    AnnotationShape.Rectangle(
                                                        id = java.util.UUID.randomUUID().toString(),
                                                        colorArgb = ANNOTATION_COLOR,
                                                        topLeft = normalize(Offset(min(start.x, end.x), min(start.y, end.y))),
                                                        bottomRight = normalize(Offset(max(start.x, end.x), max(start.y, end.y)))
                                                    )
                                                )
                                            }
                                        }
                                        dragStart = null
                                        freehandPoints = emptyList()
                                    }
                                )
                                else -> {}
                            }
                        }
                    }
                    .pointerInput(uiState.activeTool, quadMode) {
                        if (quadMode == QuadMode.NONE) {
                            detectTapGestures { tapOffset ->
                                when (uiState.activeTool) {
                                    AnnotationTool.TEXT -> pendingTextPosition = normalize(tapOffset)
                                    else -> {}
                                }
                            }
                        }
                    }
            ) {
                AsyncImage(
                    model = uiState.filePath?.let { "file://$it?rev=${uiState.fileRevision}" },
                    contentDescription = "Patient image",
                    contentScale = ContentScale.Fit,
                    onSuccess = { state ->
                        val drawable = state.result.drawable
                        imageIntrinsicSize = drawable.intrinsicWidth to drawable.intrinsicHeight
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { coordinates ->
                            canvasSizePx = Offset(coordinates.size.width.toFloat(), coordinates.size.height.toFloat())
                        }
                )

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    uiState.shapes.forEach { shape ->
                        val color = Color(shape.colorArgb)
                        when (shape) {
                            is AnnotationShape.Arrow -> drawArrow(
                                Offset(shape.start.x * w, shape.start.y * h),
                                Offset(shape.end.x * w, shape.end.y * h),
                                color
                            )
                            is AnnotationShape.Rectangle -> drawRect(
                                color = color,
                                topLeft = Offset(shape.topLeft.x * w, shape.topLeft.y * h),
                                size = androidx.compose.ui.geometry.Size(
                                    (shape.bottomRight.x - shape.topLeft.x) * w,
                                    (shape.bottomRight.y - shape.topLeft.y) * h
                                ),
                                style = Stroke(width = 4f)
                            )
                            is AnnotationShape.Freehand -> {
                                val path = androidx.compose.ui.graphics.Path()
                                shape.points.forEachIndexed { index, point ->
                                    val p = Offset(point.x * w, point.y * h)
                                    if (index == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
                                }
                                drawPath(path, color = color, style = Stroke(width = 4f))
                            }
                            is AnnotationShape.TextLabel -> {
                                drawContext.canvas.nativeCanvas.drawText(
                                    shape.text,
                                    shape.position.x * w,
                                    shape.position.y * h,
                                    android.graphics.Paint().apply {
                                        this.color = shape.colorArgb.toInt()
                                        textSize = 42f
                                        isFakeBoldText = true
                                    }
                                )
                            }
                        }
                    }

                    if (quadMode == QuadMode.NONE && freehandPoints.size > 1) {
                        when (uiState.activeTool) {
                            AnnotationTool.RECTANGLE -> {
                                val (start, end) = freehandPoints
                                drawRect(
                                    color = Color.White,
                                    topLeft = Offset(min(start.x, end.x), min(start.y, end.y)),
                                    size = androidx.compose.ui.geometry.Size(kotlin.math.abs(end.x - start.x), kotlin.math.abs(end.y - start.y)),
                                    style = Stroke(width = 3f)
                                )
                            }
                            else -> {
                                val path = androidx.compose.ui.graphics.Path()
                                freehandPoints.forEachIndexed { index, point ->
                                    if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
                                }
                                drawPath(path, color = Color(ANNOTATION_COLOR), style = Stroke(width = 4f))
                            }
                        }
                    }

                    quadPoints?.let { points ->
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(points[0].x, points[0].y)
                            lineTo(points[1].x, points[1].y)
                            lineTo(points[2].x, points[2].y)
                            lineTo(points[3].x, points[3].y)
                            close()
                        }
                        drawPath(path, color = Color.White, style = Stroke(width = 3f))
                        points.forEach { p ->
                            drawCircle(color = Color.Black, radius = 16f, center = p, alpha = 0.35f)
                            drawCircle(color = Color.White, radius = 14f, center = p, style = Stroke(width = 3f))
                            drawCircle(color = Color(0xFF4285F4), radius = 8f, center = p)
                        }
                    }
                }
            }
        }
    }

    pendingTextPosition?.let { position ->
        AlertDialog(
            onDismissRequest = { pendingTextPosition = null; textInput = "" },
            title = { Text("Add label") },
            text = {
                OutlinedTextField(value = textInput, onValueChange = { textInput = it }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    if (textInput.isNotBlank()) {
                        viewModel.addShape(
                            AnnotationShape.TextLabel(
                                id = java.util.UUID.randomUUID().toString(),
                                colorArgb = ANNOTATION_COLOR,
                                position = position,
                                text = textInput.trim()
                            )
                        )
                    }
                    pendingTextPosition = null
                    textInput = ""
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { pendingTextPosition = null; textInput = "" }) { Text("Cancel") }
            }
        )
    }

    if (showOcrPanel) {
        AlertDialog(
            onDismissRequest = { showOcrPanel = false },
            title = { Text("Extracted text (whole image)") },
            text = {
                Column {
                    if (uiState.isRunningOcr) {
                        CircularProgressIndicator(modifier = Modifier.padding(bottom = 8.dp))
                    }
                    Text(
                        text = uiState.ocrText.ifBlank { "No text detected yet." },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    clipboardManager.setText(AnnotatedString(uiState.ocrText))
                }) { Text("Copy") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { viewModel.rerunOcr(context) }, enabled = !uiState.isRunningOcr) {
                        Text("Re-scan")
                    }
                    TextButton(onClick = { showOcrPanel = false }) { Text("Close") }
                }
            }
        )
    }

    if (showQuadOcrDialog) {
        AlertDialog(
            onDismissRequest = { showQuadOcrDialog = false; viewModel.dismissQuadOcr() },
            title = { Text("Scanned text (selected region)") },
            text = {
                Column {
                    if (uiState.isRunningQuadOcr) {
                        CircularProgressIndicator(modifier = Modifier.padding(bottom = 8.dp))
                    }
                    Text(
                        text = uiState.quadOcrText?.ifBlank { "No text detected in that selection." } ?: "Scanning…",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { clipboardManager.setText(AnnotatedString(uiState.quadOcrText ?: "")) },
                    enabled = !uiState.quadOcrText.isNullOrBlank()
                ) { Text("Copy") }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = { viewModel.keepQuadOcrText(); showQuadOcrDialog = false },
                        enabled = !uiState.quadOcrText.isNullOrBlank()
                    ) { Text("Save to image") }
                    TextButton(onClick = { showQuadOcrDialog = false; viewModel.dismissQuadOcr() }) { Text("Close") }
                }
            }
        )
    }
}

@Composable
private fun ToolButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isActive: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
        )
    ) {
        Icon(icon, contentDescription = label, tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArrow(start: Offset, end: Offset, color: Color) {
    drawLine(color = color, start = start, end = end, strokeWidth = 5f)
    val angle = kotlin.math.atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
    val arrowLength = 24f
    val arrowAngle = Math.PI / 7
    val p1 = Offset(
        (end.x - arrowLength * kotlin.math.cos(angle - arrowAngle)).toFloat(),
        (end.y - arrowLength * kotlin.math.sin(angle - arrowAngle)).toFloat()
    )
    val p2 = Offset(
        (end.x - arrowLength * kotlin.math.cos(angle + arrowAngle)).toFloat(),
        (end.y - arrowLength * kotlin.math.sin(angle + arrowAngle)).toFloat()
    )
    drawLine(color = color, start = end, end = p1, strokeWidth = 5f)
    drawLine(color = color, start = end, end = p2, strokeWidth = 5f)
}
