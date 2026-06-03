package com.mapchina.ui.carving

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.ink.authoring.compose.InProgressStrokes
import androidx.ink.brush.Brush
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.strokes.Stroke
import com.mapchina.ui.theme.MapChinaColors
import org.jetbrains.compose.resources.painterResource
import mapchina.shared.generated.resources.Res
import mapchina.shared.generated.resources.stone_wall

@OptIn(ExperimentalMaterial3Api::class, ExperimentalInkCustomBrushApi::class)
@Composable
fun CarvingScreen(
    regionId: String,
    regionName: String,
    viewModel: CarvingViewModel,
    onBack: () -> Unit
) {
    var finishedStrokes by remember { mutableStateOf(listOf<Stroke>()) }
    var brushColor by remember { mutableStateOf(Color(0xFF2C1810)) }
    var brushSize by remember { mutableStateOf(8f) }

    val carvingBrush = remember(brushColor, brushSize) {
        Brush.Builder()
            .setFamily(BrushFamily())
            .setColorIntArgb(brushColor.toArgb())
            .setSize(brushSize)
            .setEpsilon(0.1f)
            .build()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    "题刻 · $regionName",
                    color = MapChinaColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = MapChinaColors.TextPrimary)
                }
            },
            actions = {
                IconButton(onClick = {
                    val strokeData = finishedStrokes.joinToString("|") { it.inputs.toString() }
                    viewModel.saveCarving(regionId, regionName, strokeData)
                    onBack()
                }) {
                    Icon(Icons.Default.Save, "保存", tint = MapChinaColors.Primary)
                }
                if (finishedStrokes.isNotEmpty()) {
                    IconButton(onClick = { finishedStrokes = emptyList() }) {
                        Icon(Icons.Default.Delete, "清空", tint = MapChinaColors.Error)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MapChinaColors.SurfaceOverlay)
        )

        // Stone wall canvas area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            Image(
                painter = painterResource(Res.drawable.stone_wall),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            InProgressStrokes(
                defaultBrush = carvingBrush,
                onStrokesFinished = { newStrokes ->
                    finishedStrokes = finishedStrokes + newStrokes
                }
            )
        }

        // Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MapChinaColors.SurfaceOverlay)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val colors = listOf(
                Color(0xFF2C1810) to "深褐",
                Color(0xFF1A1A1A) to "墨黑",
                Color(0xFF8B0000) to "朱红"
            )
            colors.forEach { (color, _) ->
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(color)
                        .clickable { brushColor = color },
                    contentAlignment = Alignment.Center
                ) {
                    if (brushColor == color) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(color)
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
            }

            Spacer(Modifier.width(16.dp))

            val sizes = listOf(4f to "细", 8f to "中", 14f to "粗")
            sizes.forEach { (size, _) ->
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (brushSize == size) MapChinaColors.Primary.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { brushSize = size },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(size.dp)
                            .clip(RoundedCornerShape(50))
                            .background(brushColor)
                    )
                }
                Spacer(Modifier.width(4.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

private fun Color.toArgb(): Int {
    return ((alpha * 255).toInt() shl 24) or
            ((red * 255).toInt() shl 16) or
            ((green * 255).toInt() shl 8) or
            (blue * 255).toInt()
}
