package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.PatientRepository
import com.example.utils.AnnotationSerializer
import com.example.utils.AnnotationShape
import com.example.utils.ImageCompressor
import com.example.utils.OcrProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AnnotationTool { NONE, ARROW, TEXT, RECTANGLE, FREEHAND }

data class ImageAnnotationUiState(
    val filePath: String? = null,
    val shapes: List<AnnotationShape> = emptyList(),
    val activeTool: AnnotationTool = AnnotationTool.NONE,
    val notes: String = "",
    val ocrText: String = "",
    val isRunningOcr: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    /** Bumped whenever the underlying file is overwritten (crop/rotate) so AsyncImage re-reads it. */
    val fileRevision: Int = 0
)

class ImageAnnotationViewModel(
    private val repository: PatientRepository,
    private val imageId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImageAnnotationUiState())
    val uiState: StateFlow<ImageAnnotationUiState> = _uiState.asStateFlow()

    init {
        loadImage()
    }

    private fun loadImage() {
        viewModelScope.launch {
            val image = repository.getImageOrNull(imageId)
            if (image != null) {
                _uiState.value = _uiState.value.copy(
                    filePath = image.filePath,
                    shapes = AnnotationSerializer.deserialize(image.annotationsJson),
                    notes = image.notes,
                    ocrText = image.ocrText,
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun setActiveTool(tool: AnnotationTool) {
        _uiState.value = _uiState.value.copy(activeTool = if (_uiState.value.activeTool == tool) AnnotationTool.NONE else tool)
    }

    fun addShape(shape: AnnotationShape) {
        _uiState.value = _uiState.value.copy(shapes = _uiState.value.shapes + shape)
    }

    fun removeShape(shapeId: String) {
        _uiState.value = _uiState.value.copy(shapes = _uiState.value.shapes.filterNot { it.id == shapeId })
    }

    fun updateNotes(notes: String) {
        _uiState.value = _uiState.value.copy(notes = notes)
    }

    fun saveAnnotations() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val image = repository.getImageOrNull(imageId) ?: return@launch
            repository.saveImage(
                image.copy(
                    annotationsJson = AnnotationSerializer.serialize(_uiState.value.shapes),
                    notes = _uiState.value.notes,
                    ocrText = _uiState.value.ocrText
                )
            )
            _uiState.value = _uiState.value.copy(isSaving = false)
        }
    }

    /** Re-runs ML Kit Text Recognition against the current file and persists the result. */
    fun rerunOcr(context: android.content.Context) {
        viewModelScope.launch {
            val path = _uiState.value.filePath ?: return@launch
            _uiState.value = _uiState.value.copy(isRunningOcr = true)
            val text = OcrProcessor.extractText(context, java.io.File(path))
            _uiState.value = _uiState.value.copy(ocrText = text, isRunningOcr = false)
            val image = repository.getImageOrNull(imageId) ?: return@launch
            repository.saveImage(image.copy(ocrText = text))
        }
    }

    /** Rotates the underlying JPEG 90° clockwise and refreshes the thumbnail. */
    fun rotate90() {
        viewModelScope.launch {
            val image = repository.getImageOrNull(imageId) ?: return@launch
            withContext(Dispatchers.IO) {
                val file = java.io.File(image.filePath)
                ImageCompressor.rotateFile(file, 90f)
                image.thumbnailPath?.let { ImageCompressor.writeThumbnail(file, java.io.File(it)) }
            }
            _uiState.value = _uiState.value.copy(fileRevision = _uiState.value.fileRevision + 1)
        }
    }

    /** Crops to a normalized rect (0f..1f), refreshes the thumbnail, and re-runs OCR since the
     *  pixel content changed. Clears prior overlay shapes, since their coordinates were relative
     *  to the pre-crop frame. */
    fun crop(context: android.content.Context, leftFrac: Float, topFrac: Float, rightFrac: Float, bottomFrac: Float) {
        viewModelScope.launch {
            val image = repository.getImageOrNull(imageId) ?: return@launch
            withContext(Dispatchers.IO) {
                val file = java.io.File(image.filePath)
                ImageCompressor.cropFile(file, leftFrac, topFrac, rightFrac, bottomFrac)
                image.thumbnailPath?.let { ImageCompressor.writeThumbnail(file, java.io.File(it)) }
            }
            _uiState.value = _uiState.value.copy(shapes = emptyList(), fileRevision = _uiState.value.fileRevision + 1)
            rerunOcr(context)
        }
    }

    companion object {
        fun provideFactory(repository: PatientRepository, imageId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ImageAnnotationViewModel(repository, imageId) as T
                }
            }
    }
}
