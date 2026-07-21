package com.mapchina.ui.carving

import com.mapchina.ui.carving.v2.CarvingDocument
import com.mapchina.ui.carving.v2.CarvingStroke

data class CarvingEditorState(
    val document: CarvingDocument? = null,
    val sourceVersion: Int? = null,
    val loadError: String? = null,
    val isDirty: Boolean = false,
    val isSaving: Boolean = false,
    val saveError: String? = null
) {
    val canSave: Boolean
        get() = loadError == null && document?.strokes?.isNotEmpty() == true
}

fun CarvingEditorState.append(stroke: CarvingStroke): CarvingEditorState {
    val currentDocument = document ?: return this
    return copy(
        document = currentDocument.copy(strokes = currentDocument.strokes + stroke),
        isDirty = true,
        saveError = null
    )
}

fun CarvingEditorState.undo(): CarvingEditorState {
    val currentDocument = document ?: return this
    if (currentDocument.strokes.isEmpty()) return this
    return copy(
        document = currentDocument.copy(strokes = currentDocument.strokes.dropLast(1)),
        isDirty = true,
        saveError = null
    )
}

fun CarvingEditorState.clear(): CarvingEditorState {
    val currentDocument = document ?: return this
    return copy(
        document = currentDocument.copy(strokes = emptyList()),
        isDirty = true,
        saveError = null
    )
}
