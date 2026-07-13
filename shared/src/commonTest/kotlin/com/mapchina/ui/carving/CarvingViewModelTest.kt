package com.mapchina.ui.carving

import app.cash.sqldelight.db.SqlDriver
import com.mapchina.data.local.MapChinaDatabase
import com.mapchina.data.local.TestDatabaseDriverFactory
import com.mapchina.data.repository.CarvingRepository
import com.mapchina.domain.model.Carving
import com.mapchina.ui.carving.v2.CarvingBrushType
import com.mapchina.ui.carving.v2.CarvingDecodeResult
import com.mapchina.ui.carving.v2.CarvingDocument
import com.mapchina.ui.carving.v2.CarvingDocumentCodec
import com.mapchina.ui.carving.v2.CarvingPoint
import com.mapchina.ui.carving.v2.CarvingStroke
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CarvingViewModelTest {

    private lateinit var database: MapChinaDatabase
    private lateinit var driver: SqlDriver
    private lateinit var repository: CarvingRepository
    private lateinit var viewModel: CarvingViewModel

    private val thirdStroke = CarvingStroke(
        brushType = CarvingBrushType.MONUMENTAL,
        sizeFraction = 0.08f,
        colorArgb = 0xFF1A1612.toInt(),
        points = listOf(
            CarvingPoint(0.1f, 0.1f, 0.5f, 0L),
            CarvingPoint(0.3f, 0.4f, 0.5f, 12L)
        )
    )

    private val twoStrokeFixture = CarvingDocumentCodec.encode(
        CarvingDocument(canvasAspectRatio = 0.62f, strokes = listOf(thirdStroke, thirdStroke.copy()))
    )

    @BeforeTest
    fun setup() {
        driver = TestDatabaseDriverFactory().createDriver()
        database = MapChinaDatabase(driver)
        repository = CarvingRepository(database)
        viewModel = CarvingViewModel(repository, "test-user", UnconfinedTestDispatcher())
    }

    @Test
    fun appendAndSave_keepsExistingAndNewStrokesAndMetadata() = runTest {
        repository.insertCarving(existingCarving(twoStrokeFixture))
        viewModel.loadCarvingForEdit("carving-1")
        viewModel.appendStroke(thirdStroke)

        assertTrue(viewModel.saveEditorDocument("other-region", "Other place"))
        advanceUntilIdle()

        val saved = assertNotNull(repository.getCarving("carving-1"))
        val decoded = assertIs<CarvingDecodeResult.Success>(CarvingDocumentCodec.decode(saved.strokeData!!))
        assertEquals(3, decoded.document.strokes.size)
        assertEquals(1L, saved.createdAt)
        assertEquals("330000", saved.regionId)
        assertEquals("浙江省", saved.regionName)
        assertEquals("attraction-1", saved.attractionId)
        assertEquals("西湖", saved.attractionName)
        assertEquals(0.62f, saved.previewAspectRatio)
    }

    @Test
    fun invalidExistingData_disablesSaveAndPreservesRawData() = runTest {
        repository.insertCarving(existingCarving("broken"))

        viewModel.loadCarvingForEdit("carving-1")

        assertEquals("这方碑刻暂时无法读取", viewModel.editorState.value.loadError)
        assertFalse(viewModel.saveEditorDocument("330000", "浙江省"))
        assertEquals("broken", repository.getCarving("carving-1")!!.strokeData)
    }

    @Test
    fun v1Data_isOnlyUpgradedAfterExplicitSave() = runTest {
        val legacy = """[{"inputs":[{"x":10,"y":20,"pressure":0.5,"elapsedTimeMillis":0},{"x":30,"y":60,"pressure":0.5,"elapsedTimeMillis":10}],"brushSize":12,"brushColorArgb":-15000000,"brushType":"MONUMENTAL"}]"""
        repository.insertCarving(existingCarving(legacy))

        viewModel.loadCarvingForEdit("carving-1")

        assertEquals(1, viewModel.editorState.value.sourceVersion)
        assertEquals(legacy, repository.getCarving("carving-1")!!.strokeData)

        assertTrue(viewModel.saveEditorDocument("330000", "浙江省"))
        advanceUntilIdle()

        val saved = assertNotNull(repository.getCarving("carving-1"))
        val decoded = assertIs<CarvingDecodeResult.Success>(CarvingDocumentCodec.decode(saved.strokeData!!))
        assertEquals(2, decoded.sourceVersion)
    }

    @Test
    fun saveFailure_keepsEditableDocumentAndReportsError() = runTest {
        viewModel.beginNew(0.62f)
        viewModel.appendStroke(thirdStroke)
        driver.close()

        assertTrue(viewModel.saveEditorDocument("330000", "浙江省"))
        advanceUntilIdle()

        val state = viewModel.editorState.value
        assertEquals(listOf(thirdStroke), state.document?.strokes)
        assertTrue(state.isDirty)
        assertFalse(state.isSaving)
        assertEquals("保存失败，请重试", state.saveError)
        assertFalse(viewModel.saveComplete.value)
    }

    @Test
    fun saveComplete_isConsumedAndResetWhenStartingOrLoadingAnEditor() = runTest {
        viewModel.beginNew(0.62f)
        viewModel.appendStroke(thirdStroke)
        assertTrue(viewModel.saveEditorDocument("330000", "浙江省"))
        advanceUntilIdle()
        assertTrue(viewModel.saveComplete.value)

        val saved = assertNotNull(viewModel.currentCarving.value)

        viewModel.consumeSaveComplete()
        assertFalse(viewModel.saveComplete.value)

        viewModel.beginNew(0.62f)
        assertFalse(viewModel.saveComplete.value)

        viewModel.loadCarvingForEdit(saved.id)
        assertFalse(viewModel.saveComplete.value)
    }

    @Test
    fun loadCarvingForRegion_resetsSaveComplete() = runTest {
        val guardedViewModel = newViewModel(StandardTestDispatcher(testScheduler))
        guardedViewModel.beginNew(0.62f)
        guardedViewModel.appendStroke(thirdStroke)
        assertTrue(guardedViewModel.saveEditorDocument("330000", "浙江省"))
        advanceUntilIdle()
        assertTrue(guardedViewModel.saveComplete.value)

        guardedViewModel.loadCarvingForRegion("330000")

        assertFalse(guardedViewModel.saveComplete.value)
    }

    @Test
    fun inFlightSave_thenBeginNew_doesNotLetOldCompletionOverwriteNewSession() = runTest {
        val guardedViewModel = newViewModel(StandardTestDispatcher(testScheduler))
        guardedViewModel.beginNew(0.62f)
        guardedViewModel.appendStroke(thirdStroke)
        assertTrue(guardedViewModel.saveEditorDocument("330000", "浙江省"))

        guardedViewModel.beginNew(0.8f)
        advanceUntilIdle()

        assertNull(guardedViewModel.currentCarving.value)
        assertEquals(0.8f, guardedViewModel.editorState.value.document?.canvasAspectRatio)
        assertEquals(emptyList(), guardedViewModel.editorState.value.document?.strokes)
        assertFalse(guardedViewModel.saveComplete.value)
        assertEquals(1, repository.getCarvingsByRegion("330000").size)
    }

    @Test
    fun inFlightSave_thenLoadEdit_doesNotLetOldCompletionOverwriteLoadedSession() = runTest {
        repository.insertCarving(existingCarving(twoStrokeFixture, id = "carving-2"))
        val guardedViewModel = newViewModel(StandardTestDispatcher(testScheduler))
        guardedViewModel.beginNew(0.62f)
        guardedViewModel.appendStroke(thirdStroke)
        assertTrue(guardedViewModel.saveEditorDocument("330000", "浙江省"))

        guardedViewModel.loadCarvingForEdit("carving-2")
        advanceUntilIdle()

        assertEquals("carving-2", guardedViewModel.currentCarving.value?.id)
        assertEquals(2, guardedViewModel.editorState.value.document?.strokes?.size)
        assertFalse(guardedViewModel.saveComplete.value)

        guardedViewModel.appendStroke(thirdStroke)
        assertTrue(guardedViewModel.saveEditorDocument("other-region", "Other place"))
        advanceUntilIdle()

        val saved = assertNotNull(repository.getCarving("carving-2"))
        val decoded = assertIs<CarvingDecodeResult.Success>(CarvingDocumentCodec.decode(saved.strokeData!!))
        assertEquals(3, decoded.document.strokes.size)
    }

    @Test
    fun saveEditorDocument_whileSaving_returnsFalseAndWritesOnlyOnce() = runTest {
        val guardedViewModel = newViewModel(StandardTestDispatcher(testScheduler))
        guardedViewModel.beginNew(0.62f)
        guardedViewModel.appendStroke(thirdStroke)

        assertTrue(guardedViewModel.saveEditorDocument("330000", "浙江省"))
        assertFalse(guardedViewModel.saveEditorDocument("330000", "浙江省"))
        advanceUntilIdle()

        assertEquals(1, repository.getCarvingsByRegion("330000").size)
    }

    @Test
    fun normalizedEmptyDocument_rejectsSaveAndReportsErrorWithoutWriting() {
        val invalidStroke = thirdStroke.copy(points = listOf(thirdStroke.points.first()))
        viewModel.beginNew(0.62f)
        viewModel.appendStroke(invalidStroke)

        assertFalse(viewModel.saveEditorDocument("330000", "浙江省"))
        assertEquals("保存失败，请重试", viewModel.editorState.value.saveError)
        assertTrue(repository.getAllCarvings().isEmpty())
    }

    @Test
    fun missingCarvingId_entersReadOnlyLoadErrorState() {
        viewModel.loadCarvingForEdit("missing")

        assertEquals("这方碑刻暂时无法读取", viewModel.editorState.value.loadError)
        assertNull(viewModel.editorState.value.document)
        assertFalse(viewModel.saveEditorDocument("330000", "浙江省"))
    }

    private fun newViewModel(dispatcher: CoroutineDispatcher): CarvingViewModel {
        return CarvingViewModel(repository, "test-user", dispatcher)
    }

    private fun existingCarving(data: String, id: String = "carving-1") = Carving(
        id = id,
        userId = "test-user",
        regionId = "330000",
        regionName = "浙江省",
        imagePath = "rock.jpg",
        strokeData = data,
        createdAt = 1L,
        attractionId = "attraction-1",
        attractionName = "西湖",
        previewAspectRatio = 0.62f
    )
}
