package com.mapchina.ui.carving

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.ink.strokes.Stroke as InkStroke
import com.mapchina.domain.model.Carving
import com.mapchina.ui.theme.MapChinaColors
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import mapchina.shared.generated.resources.Res
import mapchina.shared.generated.resources.cliff_face
import org.jetbrains.compose.resources.painterResource

@Composable
fun CarvingListScreen(
    viewModel: CarvingViewModel,
    title: String = "碑刻",
    regionId: String? = null,
    attractionId: String? = null,
    showAll: Boolean = false,
    onCreateClick: () -> Unit,
    onEditClick: (Carving) -> Unit = {},
    onBack: () -> Unit
) {
    val carvings by viewModel.carvingList.collectAsState()

    LaunchedEffect(regionId, attractionId, showAll) {
        when {
            showAll -> viewModel.loadAllCarvings()
            attractionId != null -> viewModel.loadCarvingsByAttraction(attractionId)
            regionId != null -> viewModel.loadCarvingsByRegion(regionId)
        }
    }

    var deleteTarget by remember { mutableStateOf<Carving?>(null) }
    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除碑刻") },
            text = { Text("确定要删除这条碑刻吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCarving(deleteTarget!!.id)
                    deleteTarget = null
                    when {
                        showAll -> viewModel.loadAllCarvings()
                        attractionId != null -> viewModel.loadCarvingsByAttraction(attractionId)
                        regionId != null -> viewModel.loadCarvingsByRegion(regionId)
                    }
                }) { Text("删除", color = MapChinaColors.Error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(MapChinaColors.Background)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = MapChinaColors.TextPrimary)
                }
                Text(
                    title,
                    color = MapChinaColors.TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }

            if (carvings.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF8B7355).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = Color(0xFF8B7355).copy(alpha = 0.5f)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("还没有碑刻", color = MapChinaColors.TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("点击右下角 + 开始刻字", color = MapChinaColors.TextTertiary, fontSize = 14.sp)
                    }
                }
            } else {
                @OptIn(ExperimentalFoundationApi::class)
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalItemSpacing = 8.dp,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(carvings, key = { it.id }) { carving ->
                        CarvingImageCard(
                            carving = carving,
                            onClick = { onEditClick(carving) },
                            onLongPress = { deleteTarget = carving }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(88.dp)) }
                }
            }
        }

        FloatingActionButton(
            onClick = onCreateClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp)
                .navigationBarsPadding(),
            containerColor = Color(0xFF8B7355),
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "新碑刻", modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun CarvingImageCard(
    carving: Carving,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val strokes: List<InkStroke> = remember(carving.strokeData) {
        carving.strokeData?.let { deserializeStrokes(it) } ?: emptyList()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongPress() }
                )
            }
    ) {
        Image(
            painter = painterResource(Res.drawable.cliff_face),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (strokes.isEmpty()) 120.dp else 180.dp),
            contentScale = ContentScale.Crop
        )

        if (strokes.isNotEmpty()) {
            // Shadow layer
            Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                for (stroke in strokes) {
                    val path = strokeToPath(stroke, offsetDx = 2f, offsetDy = 2f)
                    drawPath(
                        path = path,
                        color = Color(0x60000000),
                        style = Stroke(
                            width = stroke.brush.size + 6f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Miter
                        )
                    )
                }
            }
            // Groove colour layer
            Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                for (stroke in strokes) {
                    val path = strokeToPath(stroke)
                    drawPath(
                        path = path,
                        color = stroke.brush.colorIntArgb.toComposeColor(),
                        style = Stroke(
                            width = stroke.brush.size,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Miter,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 3f))
                        )
                    )
                }
            }
            // Highlight layer
            Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                for (stroke in strokes) {
                    val path = strokeToPath(stroke, offsetDx = -1f, offsetDy = -1f)
                    drawPath(
                        path = path,
                        color = Color(0x30FFF8E1),
                        style = Stroke(
                            width = stroke.brush.size * 0.15f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Miter
                        )
                    )
                }
            }
        }

        // Bottom gradient overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xCC000000))
                    )
                )
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Column {
                Text(
                    carving.attractionName ?: carving.regionName,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    formatDate(carving.createdAt),
                    color = Color(0xAAFFFFFF),
                    fontSize = 11.sp
                )
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    return try {
        val local = Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(TimeZone.currentSystemDefault())
        "${local.year}年${local.monthNumber}月${local.dayOfMonth}日"
    } catch (_: Exception) {
        ""
    }
}
