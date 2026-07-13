package com.mapchina.ui.carving

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapchina.platform.HapticType
import com.mapchina.platform.LocalHapticFeedback
import com.mapchina.ui.carving.v2.CarvingBrushSpec
import com.mapchina.ui.carving.v2.CarvingBrushType
import com.mapchina.ui.carving.v2.CarvingDocument
import com.mapchina.ui.theme.MapChinaColors

data class CarvingPlaceTarget(
    val regionId: String,
    val regionName: String,
    val attractionId: String? = null,
    val attractionName: String? = null
)

fun carvingPlaceTitle(regionName: String, attractionName: String?): String =
    attractionName?.takeIf(String::isNotBlank)
        ?: regionName.takeIf(String::isNotBlank)
        ?: "中国山河"

fun editorSaveFeedback(state: CarvingEditorState): String? = state.saveError

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CarvingScreen(
    regionId: String,
    regionName: String,
    viewModel: CarvingViewModel,
    onBack: () -> Unit,
    attractionId: String? = null,
    attractionName: String? = null,
    carvingId: String? = null
) {
    val haptic = LocalHapticFeedback.current
    val state by viewModel.editorState.collectAsState()
    val saveComplete by viewModel.saveComplete.collectAsState()
    var selectedBrush by remember {
        mutableStateOf(
            CarvingBrushSpec(
                type = CarvingBrushType.MONUMENTAL,
                sizeFraction = 0.09f,
                colorArgb = 0xFF1A1612.toInt()
            )
        )
    }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var newEditorStarted by remember(carvingId, regionId, attractionId) { mutableStateOf(false) }
    var emptySaveHintVisible by remember(carvingId, regionId, attractionId) { mutableStateOf(false) }

    LaunchedEffect(carvingId, regionId, attractionId) {
        emptySaveHintVisible = false
        if (carvingId != null) viewModel.loadCarvingForEdit(carvingId)
    }
    LaunchedEffect(carvingId, regionId, attractionId, canvasSize) {
        if (carvingId == null && !newEditorStarted && canvasSize.width > 0 && canvasSize.height > 0) {
            newEditorStarted = true
            viewModel.beginNew(canvasSize.width.toFloat() / canvasSize.height.toFloat())
        }
    }
    LaunchedEffect(saveComplete) {
        if (saveComplete) {
            viewModel.consumeSaveComplete()
            onBack()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MapChinaColors.Background)) {
        TopAppBar(
            title = {
                Column {
                    Text("摩崖留刻", color = MapChinaColors.TextPrimary, fontWeight = FontWeight.Bold)
                    Text(
                        carvingPlaceTitle(regionName, attractionName),
                        color = MapChinaColors.TextSecondary,
                        fontSize = 12.sp
                    )
                }
            },
            navigationIcon = { com.mapchina.ui.common.BackButton(onClick = onBack) },
            actions = {
                if (state.loadError == null) {
                    IconButton(
                        onClick = {
                            if (!state.canSave) {
                                haptic.perform(HapticType.WARNING)
                                emptySaveHintVisible = true
                            } else {
                                viewModel.saveEditorDocument(regionId, regionName, attractionId, attractionName)
                            }
                        },
                        modifier = Modifier.testTag("carving-save")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "保存碑刻", tint = MapChinaColors.Primary)
                    }
                    IconButton(
                        onClick = viewModel::undo,
                        enabled = state.document?.strokes?.isNotEmpty() == true,
                        modifier = Modifier.testTag("carving-undo")
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = "撤销", tint = MapChinaColors.TextSecondary)
                    }
                    IconButton(
                        onClick = viewModel::clear,
                        enabled = state.document?.strokes?.isNotEmpty() == true,
                        modifier = Modifier.testTag("carving-clear")
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "清空", tint = MapChinaColors.Error)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MapChinaColors.SurfaceOverlay)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .onSizeChanged { canvasSize = it }
        ) {
            CarvingArtwork(
                document = state.document ?: CarvingDocument(canvasAspectRatio = 1f, strokes = emptyList()),
                modifier = Modifier.fillMaxSize()
            )
            if (state.loadError == null) {
                PlatformCarvingInputSurface(
                    brush = selectedBrush,
                    enabled = !state.isSaving && state.document != null,
                    modifier = Modifier.fillMaxSize().testTag("carving-canvas"),
                    onStrokeCommitted = { stroke ->
                        emptySaveHintVisible = false
                        viewModel.appendStroke(stroke)
                    }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("这方碑刻暂时无法读取", color = MapChinaColors.TextPrimary, fontWeight = FontWeight.Medium)
                }
            }
            Column(
                modifier = Modifier.align(Alignment.TopCenter).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (emptySaveHintVisible && state.loadError == null) {
                    Surface(
                        color = MapChinaColors.SurfaceElevated.copy(alpha = 0.94f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            "先刻下一笔，再落成碑刻",
                            color = MapChinaColors.TextPrimary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
                editorSaveFeedback(state)?.let { error ->
                    Surface(
                        color = MapChinaColors.SurfaceElevated.copy(alpha = 0.96f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(error, color = MapChinaColors.Error, fontSize = 13.sp)
                            TextButton(
                                onClick = {
                                    viewModel.saveEditorDocument(regionId, regionName, attractionId, attractionName)
                                },
                                enabled = state.canSave && !state.isSaving
                            ) { Text("重试") }
                        }
                    }
                }
            }
        }

        if (state.loadError == null) {
            CarvingToolBar(
                selectedBrush = selectedBrush,
                onBrushSelected = { selectedBrush = it }
            )
        }
    }
}

@Composable
private fun CarvingToolBar(
    selectedBrush: CarvingBrushSpec,
    onBrushSelected: (CarvingBrushSpec) -> Unit
) {
    Surface(color = MapChinaColors.SurfaceOverlay) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CarvingBrushType.entries.forEach { type ->
                TextButton(
                    onClick = { onBrushSelected(selectedBrush.copy(type = type)) },
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        type.label,
                        color = if (selectedBrush.type == type) MapChinaColors.Primary else MapChinaColors.TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
            Spacer(Modifier.width(2.dp))
            listOf(0.055f to "小", 0.09f to "中", 0.13f to "大").forEach { (sizeFraction, label) ->
                val isSelected = selectedBrush.sizeFraction == sizeFraction
                IconButton(
                    onClick = { onBrushSelected(selectedBrush.copy(sizeFraction = sizeFraction)) },
                    modifier = Modifier.semantics {
                        contentDescription = "笔刷大小：$label"
                        role = Role.RadioButton
                        selected = isSelected
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size((sizeFraction * 220f).dp.coerceIn(12.dp, 28.dp))
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(
                                if (isSelected) MapChinaColors.Primary
                                else MapChinaColors.TextTertiary
                            )
                    )
                }
            }
            listOf(
                0xFF1A1612.toInt() to "崖壁墨",
                0xFF4A5568.toInt() to "青石灰",
                0xFF8B2500.toInt() to "朱砂"
            ).forEach { (colorArgb, label) ->
                val isSelected = selectedBrush.colorArgb == colorArgb
                IconButton(
                    onClick = { onBrushSelected(selectedBrush.copy(colorArgb = colorArgb)) },
                    modifier = Modifier.semantics {
                        contentDescription = "笔刷颜色：$label"
                        role = Role.RadioButton
                        selected = isSelected
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(androidx.compose.ui.graphics.Color(colorArgb))
                    )
                }
            }
        }
    }
}
