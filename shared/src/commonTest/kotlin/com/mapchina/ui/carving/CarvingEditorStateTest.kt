package com.mapchina.ui.carving

import com.mapchina.ui.carving.v2.CarvingBrushType
import com.mapchina.ui.carving.v2.CarvingDocument
import com.mapchina.ui.carving.v2.CarvingPoint
import com.mapchina.ui.carving.v2.CarvingStroke
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CarvingEditorStateTest {

    private val firstStroke = CarvingStroke(
        brushType = CarvingBrushType.MONUMENTAL,
        sizeFraction = 0.08f,
        colorArgb = 0xFF1A1612.toInt(),
        points = listOf(
            CarvingPoint(0.1f, 0.1f, 0.5f, 0L),
            CarvingPoint(0.3f, 0.4f, 0.5f, 12L)
        )
    )

    @Test
    fun append_marksDocumentDirtyAndEnablesSave() {
        val state = CarvingEditorState(
            document = CarvingDocument(canvasAspectRatio = 0.62f, strokes = emptyList())
        )

        val appended = state.append(firstStroke)

        assertEquals(listOf(firstStroke), appended.document?.strokes)
        assertTrue(appended.isDirty)
        assertTrue(appended.canSave)
        assertEquals(null, appended.saveError)
    }

    @Test
    fun undo_removesOnlyLastStrokeAndRetainsEarlierStrokes() {
        val secondStroke = firstStroke.copy(colorArgb = 0xFF2A261F.toInt())
        val state = CarvingEditorState(
            document = CarvingDocument(
                canvasAspectRatio = 0.62f,
                strokes = listOf(firstStroke, secondStroke)
            )
        )

        val undone = state.undo()

        assertEquals(listOf(firstStroke), undone.document?.strokes)
        assertTrue(undone.isDirty)
    }

    @Test
    fun clear_removesAllStrokesButKeepsDocumentEditable() {
        val state = CarvingEditorState(
            document = CarvingDocument(canvasAspectRatio = 0.62f, strokes = listOf(firstStroke))
        )

        val cleared = state.clear()

        assertEquals(emptyList(), cleared.document?.strokes)
        assertTrue(cleared.isDirty)
        assertFalse(cleared.canSave)
    }
}
