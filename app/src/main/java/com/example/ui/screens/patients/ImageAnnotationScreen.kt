package com.example.ui.screens.patients

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
import androidx.compose.material.icons.filled.Gesture
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageAnnotationScreen(
    viewModel: ImageAnnotationViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    var canvasSizePx by remember { mutableStateOf(Offset(1f, 1f)) }
    var pendingTextPosition by remember { mutableStateOf<NormalizedPoint?>(null) }
    var textInput by remember { mutableStateOf("") }
    var cropRect by remember { mutableStateOf<Pair<Offset, Offset>?>(null) }
    var isCropping by remember { mutableStateOf(false) }
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var freehandPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var showOcrPanel by remember { mutableStateOf(false) }

    fun normalize(offset: Offset) = NormalizedPoint(
        x = (offset.x / canvasSizePx.x).coerceIn(0f, 1f),
        y = (offset.y / canvasSizePx.y).coerceIn(0f, 1f)
    )

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
                        Icon(Icons.Default.TextSnippet, contentDescription = "OCR text")
                    }
                    IconButton(onClick = { viewModel.rotate90() }) {
                        Icon(Icons.Default.RotateRight, contentDescription = "Rotate")
                    }
                    IconButton(onClick = { isCropping = !isCropping; cropRect = null }) {
                        Icon(Icons.Default.Crop, contentDescription = "Crop", tint = if (isCropping) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
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
            if (!isCropping) {
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
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    TextButton(onClick = { isCropping = false; cropRect = null }) { Text("Cancel") }
                    TextButton(
                        onClick = {
                            cropRect?.let { (start, end) ->
                                val left = min(start.x, end.x) / canvasSizePx.x
                                val top = min(start.y, end.y) / canvasSizePx.y
                                val right = max(start.x, end.x) / canvasSizePx.x
                                val bottom = max(start.y, end.y) / canvasSizePx.y
                                viewModel.crop(context, left, top, right, bottom)
                            }
                            isCropping = false
                            cropRect = null
                        },
                        enabled = cropRect != null
                    ) { Text("Apply crop") }
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
                    .pointerInput(canvasSizePx, uiState.activeTool, isCropping) {
                        if (isCropping) {
                            detectDragGestures(
                                onDragStart = { dragStart = it; cropRect = it to it },
                                onDrag = { change, _ ->
                                    dragStart?.let { start -> cropRect = start to change.position }
                                },
                                onDragEnd = { dragStart = null }
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
                                    onDragStart = { dragStart = it; cropRect = it to it },
                                    onDrag = { change, _ -> dragStart?.let { start -> cropRect = start to change.position } },
                                    onDragEnd = {
                                        cropRect?.let { (start, end) ->
                                            viewModel.addShape(
                                                AnnotationShape.Rectangle(
                                                    id = java.util.UUID.randomUUID().toString(),
                                                    colorArgb = ANNOTATION_COLOR,
                                                    topLeft = normalize(Offset(min(start.x, end.x), min(start.y, end.y))),
                                                    bottomRight = normalize(Offset(max(start.x, end.x), max(start.y, end.y)))
                                                )
                                            )
                                        }
                                        dragStart = null
                                        cropRect = null
                                    }
                                )
                                else -> {}
                            }
                        }
                    }
                    .pointerInput(uiState.activeTool) {
                        detectTapGestures { tapOffset ->
                            when (uiState.activeTool) {
                                AnnotationTool.TEXT -> pendingTextPosition = normalize(tapOffset)
                                else -> {}
                            }
                        }
                    }
            ) {
                AsyncImage(
                    model = uiState.filePath?.let { "file://$it?rev=${uiState.fileRevision}" },
                    contentDescription = "Patient image",
                    contentScale = ContentScale.Fit,
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

                    if (freehandPoints.size > 1) {
                        val path = androidx.compose.ui.graphics.Path()
                        freehandPoints.forEachIndexed { index, point ->
                            if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
                        }
                        drawPath(path, color = Color(ANNOTATION_COLOR), style = Stroke(width = 4f))
                    }

                    if (isCropping) {
                        cropRect?.let { (start, end) ->
                            drawRect(
                                color = Color.White,
                                topLeft = Offset(min(start.x, end.x), min(start.y, end.y)),
                                size = androidx.compose.ui.geometry.Size(kotlin.math.abs(end.x - start.x), kotlin.math.abs(end.y - start.y)),
                                style = Stroke(width = 3f)
                            )
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
        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
        AlertDialog(
            onDismissRequest = { showOcrPanel = false },
            title = { Text("Extracted text (OCR)") },
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
                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(uiState.ocrText))
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
