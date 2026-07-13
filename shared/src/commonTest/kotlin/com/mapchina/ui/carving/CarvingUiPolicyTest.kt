package com.mapchina.ui.carving

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class CarvingUiPolicyTest {

    @Test
    fun editorTitle_prefersAttractionThenRegion() {
        assertEquals("西湖", carvingPlaceTitle("浙江省", "西湖"))
        assertEquals("浙江省", carvingPlaceTitle("浙江省", null))
    }

    @Test
    fun invalidDocument_neverOffersSave() {
        assertFalse(CarvingEditorState(loadError = "bad").canSave)
    }

    @Test
    fun saveError_isPresentedWithoutDiscardingTheDocument() {
        val document = com.mapchina.ui.carving.v2.CarvingDocument(
            canvasAspectRatio = 0.62f,
            strokes = emptyList()
        )
        val state = CarvingEditorState(document = document, saveError = "保存失败，请重试")

        assertEquals("保存失败，请重试", editorSaveFeedback(state))
        assertEquals(document, state.document)
    }

    @Test
    fun galleryContext_fallsBackToChinaLandscape() {
        assertEquals("cn_landscape", carvingContextTarget("我的碑刻", null, null).regionId)
    }
}
