package com.mapchina.ui.carving

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush as ComposeBrush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.ink.authoring.compose.InProgressStrokes
import androidx.ink.brush.Brush
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.strokes.Stroke
import com.mapchina.platform.HapticType
import com.mapchina.platform.LocalHapticFeedback
import com.mapchina.ui.theme.MapChinaColors
import org.jetbrains.compose.resources.painterResource
import mapchina.shared.generated.resources.Res
import mapchina.shared.generated.resources.cliff_face

enum class CarvingBrushType(val label: String) {
    IRON_CHISEL("铁錾"),
    MONUMENTAL("榜书"),
    WEATHERED("风化")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalInkCustomBrushApi::class)
@Composable
actual fun CarvingScreen(
    regionId: String,
    regionName: String,
    viewModel: CarvingViewModel,
    onBack: () -> Unit,
    attractionId: String?,
    attractionName: String?,
    carvingId: String?
) {
    val haptic = LocalHapticFeedback.current
    var finishedStrokes by remember { mutableStateOf(listOf<Stroke>()) }
    var brushType by remember { mutableStateOf(CarvingBrushType.IRON_CHISEL) }
    var brushColor by remember { mutableStateOf(Color(0xFF1A1612)) }
    var brushSize by remember { mutableStateOf(14f) }
    var emptySaveHintVisible by remember { mutableStateOf(false) }

    LaunchedEffect(carvingId, regionId) {
        if (carvingId != null) {
            viewModel.loadCarvingForEdit(carvingId)
        } else {
            viewModel.loadCarvingForRegion(regionId)
        }
    }
    val existingStrokeData by viewModel.existingStrokeData.collectAsState()
    val existingStrokes by remember { derivedStateOf { existingStrokeData?.let { deserializeStrokes(it) } ?: emptyList() } }

    val saveComplete by viewModel.saveComplete.collectAsState()
    LaunchedEffect(saveComplete) {
        if (saveComplete) onBack()
    }

    val titleName = attractionName?.takeIf { it.isNotBlank() }
        ?: regionName.takeIf { it.isNotBlank() }
        ?: "中国山河"
    val carvingBrush = rememberCarvingBrush(brushType, brushColor, brushSize)

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    "摩崖留刻",
                    color = MapChinaColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                com.mapchina.ui.common.BackButton(onClick = onBack)
            },
            actions = {
                IconButton(onClick = {
                    if (shouldBlockEmptyCarvingSave(finishedStrokes.size, existingStrokes.size)) {
                        haptic.perform(HapticType.WARNING)
                        emptySaveHintVisible = true
                        return@IconButton
                    }
                    val strokeData = try {
                        if (finishedStrokes.isEmpty() && existingStrokeData != null) {
                            existingStrokeData ?: "[]"
                        } else {
                            serializeStrokes(finishedStrokes, brushType, brushColor.toArgb())
                        }
                    } catch (_: Exception) {
                        "[]"
                    }
                    viewModel.saveCarving(
                        regionId, regionName,
                        strokeData = strokeData,
                        attractionId = attractionId,
                        attractionName = attractionName
                    )
                }) {
                    Text("保存", color = MapChinaColors.Primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                if (finishedStrokes.isNotEmpty()) {
                    IconButton(onClick = { finishedStrokes = finishedStrokes.dropLast(1) }) {
                        Text("撤销", color = MapChinaColors.TextSecondary, fontSize = 13.sp)
                    }
                    IconButton(onClick = { finishedStrokes = emptyList() }) {
                        Text("清空", color = MapChinaColors.Error, fontSize = 13.sp)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MapChinaColors.SurfaceOverlay)
        )

        // Cliff face canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Layer 1: Base cliff texture
            Image(
                painter = painterResource(Res.drawable.cliff_face),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Layer 2: Warm centre glow + cool edge
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    brush = ComposeBrush.radialGradient(
                        colors = listOf(
                            Color(0x22D8C19A),
                            Color.Transparent,
                            Color(0x161A2020)
                        ),
                        center = Offset(size.width * 0.42f, size.height * 0.34f),
                        radius = size.width * 0.68f
                    )
                )
            }

            // Layer 2b: Site atmosphere — cracks, moss traces, and a subtle working area
            Canvas(modifier = Modifier.fillMaxSize()) {
                val crackColor = Color(0xFF211B17).copy(alpha = 0.24f)
                val mossColor = Color(0xFF2F4B2F).copy(alpha = 0.20f)
                val chalkDust = Color(0xFFF4E8D4).copy(alpha = 0.13f)

                val leftCrack = Path().apply {
                    moveTo(size.width * 0.12f, size.height * 0.18f)
                    quadraticBezierTo(size.width * 0.20f, size.height * 0.28f, size.width * 0.16f, size.height * 0.42f)
                    quadraticBezierTo(size.width * 0.12f, size.height * 0.56f, size.width * 0.21f, size.height * 0.70f)
                }
                drawPath(
                    path = leftCrack,
                    color = crackColor,
                    style = DrawStroke(width = 2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                val upperCrack = Path().apply {
                    moveTo(size.width * 0.56f, size.height * 0.12f)
                    quadraticBezierTo(size.width * 0.66f, size.height * 0.18f, size.width * 0.63f, size.height * 0.30f)
                    quadraticBezierTo(size.width * 0.72f, size.height * 0.34f, size.width * 0.76f, size.height * 0.46f)
                }
                drawPath(
                    path = upperCrack,
                    color = crackColor.copy(alpha = 0.18f),
                    style = DrawStroke(width = 1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                drawCircle(mossColor, radius = size.minDimension * 0.08f, center = Offset(size.width * 0.06f, size.height * 0.25f))
                drawCircle(mossColor.copy(alpha = 0.16f), radius = size.minDimension * 0.06f, center = Offset(size.width * 0.90f, size.height * 0.20f))
                drawCircle(mossColor.copy(alpha = 0.14f), radius = size.minDimension * 0.05f, center = Offset(size.width * 0.12f, size.height * 0.82f))

                drawRect(
                    brush = ComposeBrush.radialGradient(
                        colors = listOf(chalkDust, Color.Transparent),
                        center = Offset(size.width * 0.50f, size.height * 0.52f),
                        radius = size.width * 0.38f
                    )
                )
            }

            // Layer 3: Vignette
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    brush = ComposeBrush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Color(0x30000000)
                        ),
                        center = Offset(size.width / 2, size.height / 2),
                        radius = size.width * 0.7f
                    )
                )
            }

            CarvingFieldSiteHeader(
                titleName = titleName,
                emptySaveHintVisible = emptySaveHintVisible,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            )

            // Layer 4: Finished strokes — realistic cliff carving
            if (finishedStrokes.isNotEmpty()) {
                // 4a: Rock deformation shadow — wide dark halo simulating rock displaced by chisel
                Canvas(modifier = Modifier.fillMaxSize()) {
                    for (stroke in finishedStrokes) {
                        val path = strokeToPath(stroke)
                        drawPath(
                            path = path,
                            color = Color(0x181A1612),
                            style = DrawStroke(
                                width = stroke.brush.size + 20f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }

                // 4b: V-groove deep shadow (offset +5,+5, wide, very dark)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    for (stroke in finishedStrokes) {
                        val path = strokeToPath(stroke, offsetDx = 5f, offsetDy = 5f)
                        drawPath(
                            path = path,
                            color = Color(0x90000000),
                            style = DrawStroke(
                                width = stroke.brush.size + 12f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Miter,
                                miter = 3f
                            )
                        )
                    }
                }

                // 4c: V-groove inner shadow (offset +2,+2, medium width)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    for (stroke in finishedStrokes) {
                        val path = strokeToPath(stroke, offsetDx = 2f, offsetDy = 2f)
                        drawPath(
                            path = path,
                            color = Color(0x60000000),
                            style = DrawStroke(
                                width = stroke.brush.size + 6f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Miter,
                                miter = 3f
                            )
                        )
                    }
                }

                // 4d: Groove floor — the actual carved channel
                Canvas(modifier = Modifier.fillMaxSize()) {
                    for ((index, stroke) in finishedStrokes.withIndex()) {
                        val path = strokeToPath(stroke)
                        val width = stroke.brush.size
                        val color = stroke.brush.colorIntArgb.toComposeColor()

                        val pathEffect = when (brushType) {
                            CarvingBrushType.IRON_CHISEL -> PathEffect.dashPathEffect(floatArrayOf(6f, 3f))
                            else -> null
                        }

                        // Spalling: random alpha breaks simulate chipped rock
                        val rng = kotlin.random.Random(
                            if (stroke.inputs.size > 0) stroke.inputs.get(0).x.toInt() * 31 + index else index
                        )
                        val spallAlpha = if (rng.nextFloat() < 0.2f) 0.4f + rng.nextFloat() * 0.3f else 1f

                        drawPath(
                            path = path,
                            color = color.copy(alpha = spallAlpha),
                            style = DrawStroke(
                                width = width + rng.nextFloat() * 3f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Miter,
                                miter = 3f,
                                pathEffect = pathEffect
                            )
                        )
                    }
                }

                // 4e: Upper-rim highlight (offset -2,-2, thin, warm white) — light catching the V-groove edge
                Canvas(modifier = Modifier.fillMaxSize()) {
                    for (stroke in finishedStrokes) {
                        val path = strokeToPath(stroke, offsetDx = -2f, offsetDy = -2f)
                        drawPath(
                            path = path,
                            color = Color(0x55FFF0D0),
                            style = DrawStroke(
                                width = stroke.brush.size * 0.25f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Miter,
                                miter = 3f
                            )
                        )
                    }
                }

                // 4f: Chisel impact craters — small dots at stroke input points
                Canvas(modifier = Modifier.fillMaxSize()) {
                    for ((index, stroke) in finishedStrokes.withIndex()) {
                        val rng = kotlin.random.Random(
                            if (stroke.inputs.size > 0) stroke.inputs.get(0).x.toInt() * 17 + index else index
                        )
                        // Sample every few input points for impact marks
                        val step = maxOf(1, stroke.inputs.size / 8)
                        var i = 0
                        while (i < stroke.inputs.size) {
                            val input = stroke.inputs[i]
                            // Small crater: dark dot with slight offset
                            val craterRadius = 1.5f + rng.nextFloat() * 2f
                            drawCircle(
                                color = Color(0x40000000),
                                radius = craterRadius + 1f,
                                center = Offset(input.x + 1f, input.y + 1f)
                            )
                            drawCircle(
                                color = Color(0x701A1612),
                                radius = craterRadius,
                                center = Offset(input.x, input.y)
                            )
                            i += step
                        }
                    }
                }

                // 4g: Rock debris / splatter particles
                Canvas(modifier = Modifier.fillMaxSize()) {
                    for ((index, stroke) in finishedStrokes.withIndex()) {
                        val rng = kotlin.random.Random(
                            if (stroke.inputs.size > 0) stroke.inputs.get(0).x.toInt() * 53 + index else index
                        )
                        // Scatter debris along the stroke
                        val debrisCount = minOf(20, stroke.inputs.size / 3)
                        for (d in 0 until debrisCount) {
                            val inputIdx = rng.nextInt(stroke.inputs.size)
                            val input = stroke.inputs[inputIdx]
                            val dx = (rng.nextFloat() - 0.5f) * (stroke.brush.size + 16f)
                            val dy = (rng.nextFloat() - 0.5f) * (stroke.brush.size + 16f)
                            val debrisRadius = 0.5f + rng.nextFloat() * 1.5f
                            val debrisAlpha = 0.15f + rng.nextFloat() * 0.25f

                            // Small rock fragment
                            drawCircle(
                                color = Color(0xFF3D3529).copy(alpha = debrisAlpha),
                                radius = debrisRadius,
                                center = Offset(input.x + dx, input.y + dy)
                            )
                        }
                    }
                }

                // 4h: Lichen patches
                Canvas(modifier = Modifier.fillMaxSize()) {
                    for ((index, stroke) in finishedStrokes.withIndex()) {
                        val rng = kotlin.random.Random(
                            if (stroke.inputs.size > 0) stroke.inputs.get(0).x.toInt() * 31 + index + 7 else index + 7
                        )
                        if (rng.nextFloat() < 0.25f) {
                            if (stroke.inputs.size == 0) continue
                            val lastInput = stroke.inputs.get(stroke.inputs.size - 1)
                            drawCircle(
                                color = Color(0xFF2D4A2D).copy(alpha = 0.2f + rng.nextFloat() * 0.15f),
                                radius = 3f + rng.nextFloat() * 5f,
                                center = Offset(lastInput.x + rng.nextFloat() * 10f - 5f, lastInput.y + rng.nextFloat() * 10f - 5f)
                            )
                        }
                    }
                }
            }

            // Layer 5: Existing carvings (weathered, faded)
            if (existingStrokes.isNotEmpty()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = 0.55f }
                ) {
                    for (stroke in existingStrokes) {
                        val path = strokeToPath(stroke, offsetDx = 4f, offsetDy = 4f)
                        drawPath(
                            path = path,
                            color = Color(0x55000000),
                            style = DrawStroke(
                                width = stroke.brush.size + 10f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Miter,
                                miter = 3f
                            )
                        )
                    }
                    for (stroke in existingStrokes) {
                        val path = strokeToPath(stroke, offsetDx = 2f, offsetDy = 2f)
                        drawPath(
                            path = path,
                            color = Color(0x40000000),
                            style = DrawStroke(
                                width = stroke.brush.size + 5f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Miter,
                                miter = 3f
                            )
                        )
                    }
                    for (stroke in existingStrokes) {
                        val path = strokeToPath(stroke)
                        drawPath(
                            path = path,
                            color = stroke.brush.colorIntArgb.toComposeColor().copy(alpha = 0.55f),
                            style = DrawStroke(
                                width = stroke.brush.size,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Miter,
                                miter = 3f
                            )
                        )
                    }
                    for (stroke in existingStrokes) {
                        val path = strokeToPath(stroke, offsetDx = -2f, offsetDy = -2f)
                        drawPath(
                            path = path,
                            color = Color(0x25FFF0D0),
                            style = DrawStroke(
                                width = stroke.brush.size * 0.2f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Miter,
                                miter = 3f
                            )
                        )
                    }
                }
            }

            // Layer 6: In-progress strokes with haptic feedback
            key(carvingBrush) {
                InProgressStrokes(
                    defaultBrush = carvingBrush,
                    onStrokesFinished = { newStrokes ->
                        // Trigger heavy haptic on each finished stroke — simulates chisel strike
                        repeat(newStrokes.size) {
                            haptic.perform(HapticType.HEAVY)
                        }
                        emptySaveHintVisible = false
                        finishedStrokes = finishedStrokes + newStrokes
                    }
                )
            }
        }

        // Bottom toolbar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xDD1C1C1E),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Brush type selector
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    CarvingBrushType.entries.forEach { type ->
                        BrushTypeButton(
                            type = type,
                            selected = brushType == type,
                            onClick = {
                                haptic.perform(HapticType.SELECTION)
                                brushType = type
                            }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(28.dp)
                        .background(Color(0x33FFFFFF))
                )

                // Colour selector — cliff palette
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val palette = listOf(
                        Color(0xFF1A1612) to "崖壁墨",
                        Color(0xFF4A5568) to "青石灰",
                        Color(0xFF8B2500) to "朱砂",
                        Color(0xFF9CA3AF) to "风化石"
                    )
                    palette.forEach { (color, _) ->
                        ColorDot(
                            color = color,
                            selected = brushColor == color,
                            onClick = {
                                haptic.perform(HapticType.LIGHT)
                                brushColor = color
                            }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(28.dp)
                        .background(Color(0x33FFFFFF))
                )

                // Size selector
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf(8f to "细", 14f to "中", 24f to "粗").forEach { (size, label) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                haptic.perform(HapticType.LIGHT)
                                brushSize = size
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (brushSize == size) 28.dp else 24.dp)
                                    .background(
                                        if (brushSize == size) Color(0x33FFFFFF) else Color.Transparent,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(size.dp)
                                        .background(brushColor, CircleShape)
                                )
                            }
                            Text(
                                label,
                                color = if (brushSize == size) Color.White else Color(0xFF888888),
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}

fun shouldBlockEmptyCarvingSave(finishedStrokeCount: Int, existingStrokeCount: Int): Boolean {
    return finishedStrokeCount == 0 && existingStrokeCount == 0
}

@Composable
fun CarvingFieldSiteHeader(
    titleName: String,
    emptySaveHintVisible: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xD8F7F0E6),
        shadowElevation = 5.dp,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                titleName,
                color = Color(0xFF1F211E),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "在这面山石上刻下今日足迹",
                color = Color(0xFF6B5B45),
                fontSize = 12.sp,
                maxLines = 1
            )
            if (emptySaveHintVisible) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "先刻下一笔，再落成碑刻",
                    color = MapChinaColors.Error,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

internal fun strokeToPath(stroke: Stroke, offsetDx: Float = 0f, offsetDy: Float = 0f): Path {
    val inputs = stroke.inputs
    val path = Path()
    if (inputs.isEmpty()) return path

    val first = inputs[0]
    path.moveTo(first.x + offsetDx, first.y + offsetDy)

    if (inputs.size == 1) return path

    for (i in 1 until inputs.size) {
        val curr = inputs[i]
        val cx = curr.x + offsetDx
        val cy = curr.y + offsetDy
        if (i < inputs.size - 1) {
            val next = inputs[i + 1]
            val midX = (cx + next.x + offsetDx) / 2f
            val midY = (cy + next.y + offsetDy) / 2f
            path.quadraticBezierTo(cx, cy, midX, midY)
        } else {
            path.lineTo(cx, cy)
        }
    }
    return path
}

internal fun Int.toComposeColor(): Color {
    val a = (this shr 24) and 0xFF
    val r = (this shr 16) and 0xFF
    val g = (this shr 8) and 0xFF
    val b = this and 0xFF
    return Color(r / 255f, g / 255f, b / 255f, a / 255f)
}

@Composable
private fun BrushTypeButton(
    type: CarvingBrushType,
    selected: Boolean,
    onClick: () -> Unit
) {
    val icon = when (type) {
        CarvingBrushType.IRON_CHISEL -> Icons.Default.Edit
        CarvingBrushType.MONUMENTAL -> Icons.Default.Brush
        CarvingBrushType.WEATHERED -> Icons.Default.Gesture
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .background(if (selected) Color(0x33FFFFFF) else Color.Transparent)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(
            icon,
            contentDescription = type.label,
            tint = if (selected) Color.White else Color(0xFF888888),
            modifier = Modifier.size(20.dp)
        )
        Text(
            type.label,
            color = if (selected) Color.White else Color(0xFF888888),
            fontSize = 9.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun ColorDot(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(if (selected) 28.dp else 24.dp)
            .background(if (selected) Color(0x33FFFFFF) else Color.Transparent, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(if (selected) 18.dp else 14.dp)
                .background(color, CircleShape)
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
            )
        }
    }
}

@OptIn(ExperimentalInkCustomBrushApi::class)
@Composable
private fun rememberCarvingBrush(
    brushType: CarvingBrushType,
    color: Color,
    size: Float
): Brush {
    return remember(brushType, color, size) {
        val family = BrushFamily()
        val (epsilon, adjustedSize) = when (brushType) {
            CarvingBrushType.IRON_CHISEL -> 0.15f to size * 1.8f
            CarvingBrushType.MONUMENTAL -> 0.05f to size * 2.2f
            CarvingBrushType.WEATHERED -> 0.3f to size * 1.5f
        }
        Brush.Builder()
            .setFamily(family)
            .setColorIntArgb(color.toArgb())
            .setSize(adjustedSize)
            .setEpsilon(epsilon)
            .build()
    }
}

private fun Color.toArgb(): Int {
    return ((alpha * 255).toInt() shl 24) or
            ((red * 255).toInt() shl 16) or
            ((green * 255).toInt() shl 8) or
            (blue * 255).toInt()
}
