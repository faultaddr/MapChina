package com.mapchina.ui.carving

import com.mapchina.data.repository.CarvingRepository
import com.mapchina.domain.model.Carving
import com.mapchina.ui.carving.v2.CURRENT_CARVING_VERSION
import com.mapchina.ui.carving.v2.CarvingDecodeResult
import com.mapchina.ui.carving.v2.CarvingDocument
import com.mapchina.ui.carving.v2.CarvingDocumentCodec
import com.mapchina.ui.carving.v2.CarvingStroke
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock

class CarvingViewModel(
    private val carvingRepository: CarvingRepository,
    private val userId: String = "",
    dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val vmScope = CoroutineScope(SupervisorJob() + dispatcher)

    private val _savedCarvings = MutableStateFlow<List<Carving>>(emptyList())
    val savedCarvings: StateFlow<List<Carving>> = _savedCarvings.asStateFlow()

    private val _currentCarving = MutableStateFlow<Carving?>(null)
    val currentCarving: StateFlow<Carving?> = _currentCarving.asStateFlow()

    private val _existingStrokeData = MutableStateFlow<String?>(null)
    @Deprecated("Use editorState instead. This legacy UI-facing API will be removed after shared UI migration.")
    val existingStrokeData: StateFlow<String?> = _existingStrokeData.asStateFlow()

    private val _editorState = MutableStateFlow(CarvingEditorState())
    val editorState: StateFlow<CarvingEditorState> = _editorState.asStateFlow()

    private val _carvingList = MutableStateFlow<List<Carving>>(emptyList())
    val carvingList: StateFlow<List<Carving>> = _carvingList.asStateFlow()

    private var editingCarvingId: String? = null

    fun loadCarvingsByRegion(regionId: String) {
        _carvingList.value = carvingRepository.getCarvingsByRegion(regionId)
    }

    fun loadCarvingsByAttraction(attractionId: String) {
        _carvingList.value = carvingRepository.getCarvingsByAttraction(attractionId)
    }

    fun loadAllCarvings() {
        _carvingList.value = carvingRepository.getAllCarvings()
    }

    fun loadCarvingForRegion(regionId: String) {
        val existing = carvingRepository.getCarvingsByRegion(regionId)
        _currentCarving.value = existing.firstOrNull()
        _existingStrokeData.value = null
    }

    fun loadCarvingForEdit(carvingId: String) {
        val carving = carvingRepository.getCarving(carvingId)
        editingCarvingId = carving?.id
        _currentCarving.value = carving
        _existingStrokeData.value = carving?.strokeData
        _saveComplete.value = false
        _editorState.value = carving?.toEditorState() ?: CarvingEditorState()
    }

    fun beginNew(aspectRatio: Float) {
        editingCarvingId = null
        _currentCarving.value = null
        _existingStrokeData.value = null
        _editorState.value = CarvingEditorState(
            document = CarvingDocument(
                canvasAspectRatio = aspectRatio.takeIf { it.isFinite() && it > 0f } ?: 1f,
                strokes = emptyList()
            ),
            sourceVersion = CURRENT_CARVING_VERSION
        )
        _saveComplete.value = false
    }

    fun appendStroke(stroke: CarvingStroke) {
        updateEditorDocument { it.append(stroke) }
    }

    fun undo() {
        updateEditorDocument { it.undo() }
    }

    fun clear() {
        updateEditorDocument { it.clear() }
    }

    private val _saveComplete = MutableStateFlow(false)
    val saveComplete: StateFlow<Boolean> = _saveComplete.asStateFlow()

    fun consumeSaveComplete() {
        _saveComplete.value = false
    }

    fun saveEditorDocument(
        regionId: String,
        regionName: String,
        attractionId: String? = null,
        attractionName: String? = null
    ): Boolean {
        val state = _editorState.value
        val document = state.document ?: return false
        if (!state.canSave || state.isSaving) return false

        val strokeData = try {
            CarvingDocumentCodec.encode(document)
        } catch (_: Exception) {
            _editorState.value = state.copy(saveError = SAVE_ERROR)
            return false
        }
        val normalizedDocument = (CarvingDocumentCodec.decode(strokeData) as? CarvingDecodeResult.Success)
            ?.document
            ?.takeIf { it.strokes.isNotEmpty() }
            ?: return false

        val existingCarving = editingCarvingId?.let(carvingRepository::getCarving)
        val now = Clock.System.now().toEpochMilliseconds()
        val carving = existingCarving?.copy(
            strokeData = strokeData,
            previewAspectRatio = normalizedDocument.canvasAspectRatio
        ) ?: Carving(
            id = "carving_${regionId}_${attractionId ?: "region"}_$now",
            userId = userId,
            regionId = regionId,
            regionName = regionName,
            imagePath = null,
            strokeData = strokeData,
            createdAt = now,
            attractionId = attractionId,
            attractionName = attractionName,
            previewAspectRatio = normalizedDocument.canvasAspectRatio
        )

        _editorState.value = state.copy(isSaving = true, saveError = null)
        vmScope.launch {
            try {
                if (existingCarving == null) {
                    carvingRepository.insertCarving(carving)
                } else {
                    carvingRepository.updateCarving(carving)
                }
                _currentCarving.value = carving
                _existingStrokeData.value = strokeData
                editingCarvingId = null
                _editorState.value = _editorState.value.copy(
                    document = normalizedDocument,
                    sourceVersion = CURRENT_CARVING_VERSION,
                    isDirty = false,
                    isSaving = false,
                    saveError = null
                )
                _saveComplete.value = true
            } catch (_: Exception) {
                _editorState.value = _editorState.value.copy(
                    isSaving = false,
                    saveError = SAVE_ERROR
                )
            }
        }
        return true
    }

    @Deprecated("Use saveEditorDocument with a complete V2 document instead.")
    fun saveCarving(
        regionId: String,
        regionName: String,
        strokeData: String,
        imagePath: String? = null,
        previewAspectRatio: Float? = null,
        attractionId: String? = null,
        attractionName: String? = null
    ) {
        val now = Clock.System.now().toEpochMilliseconds()
        val id = editingCarvingId ?: "carving_${regionId}_${attractionId ?: "region"}_$now"
        val existingCarving = editingCarvingId?.let { carvingRepository.getCarving(it) }
        val carving = Carving(
            id = id,
            userId = userId,
            regionId = regionId,
            regionName = regionName,
            imagePath = imagePath,
            strokeData = strokeData,
            createdAt = existingCarving?.createdAt ?: now,
            attractionId = attractionId,
            attractionName = attractionName,
            previewAspectRatio = previewAspectRatio
        )
        vmScope.launch {
            try {
                if (editingCarvingId != null) {
                    carvingRepository.updateCarving(carving)
                } else {
                    carvingRepository.insertCarving(carving)
                }
                _currentCarving.value = carving
                editingCarvingId = null
            } catch (_: Exception) {
                // If insert fails (e.g. duplicate id), try update
                carvingRepository.updateCarving(carving)
            }
            _saveComplete.value = true
        }
    }

    fun deleteCarving(id: String) {
        vmScope.launch {
            carvingRepository.deleteCarving(id)
            _currentCarving.value = null
        }
    }

    private fun Carving.toEditorState(): CarvingEditorState {
        return when (val result = CarvingDocumentCodec.decode(strokeData.orEmpty(), previewAspectRatio)) {
            is CarvingDecodeResult.Success -> CarvingEditorState(
                document = result.document,
                sourceVersion = result.sourceVersion
            )

            CarvingDecodeResult.Empty -> CarvingEditorState(
                document = CarvingDocument(
                    canvasAspectRatio = previewAspectRatio
                        ?.takeIf { it.isFinite() && it > 0f }
                        ?: 1f,
                    strokes = emptyList()
                )
            )

            is CarvingDecodeResult.Invalid -> CarvingEditorState(loadError = LOAD_ERROR)
        }
    }

    private fun updateEditorDocument(transform: (CarvingEditorState) -> CarvingEditorState) {
        val state = _editorState.value
        if (state.loadError != null || state.isSaving) return
        _editorState.value = transform(state)
    }

    private companion object {
        const val LOAD_ERROR = "这方碑刻暂时无法读取"
        const val SAVE_ERROR = "保存失败，请重试"
    }
}
