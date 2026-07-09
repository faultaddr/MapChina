package com.mapchina.ui.carving

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.ink.strokes.Stroke as InkStroke
import com.mapchina.domain.model.Carving
import com.mapchina.ui.theme.MapChinaColors
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import mapchina.shared.generated.resources.Res
import mapchina.shared.generated.resources.cliff_face
import org.jetbrains.compose.resources.painterResource

@Composable
actual fun CarvingListScreen(
    viewModel: CarvingViewModel,
    title: String,
    regionId: String?,
    attractionId: String?,
    showAll: Boolean,
    onCreateClick: (CarvingPlaceTarget) -> Unit,
    onEditClick: (Carving) -> Unit,
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
    var showPlacePicker by remember { mutableStateOf(false) }
    val contextTarget = remember(title, regionId, attractionId) {
        CarvingPlaceTarget(
            regionId = regionId?.takeIf { it.isNotBlank() } ?: "cn_landscape",
            regionName = title.regionNameFromTitle().ifBlank { "中国山河" },
            attractionId = attractionId,
            attractionName = null
        )
    }
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
                com.mapchina.ui.common.BackButton(onClick = onBack)
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
            onClick = {
                if (showAll && regionId.isNullOrBlank() && attractionId.isNullOrBlank()) {
                    showPlacePicker = true
                } else {
                    onCreateClick(contextTarget)
                }
            },
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

        if (showPlacePicker) {
            CarvingPlacePickerSheet(
                onDismiss = { showPlacePicker = false },
                onPlaceClick = { target ->
                    showPlacePicker = false
                    onCreateClick(target)
                }
            )
        }
    }
}

private val recommendedCarvingPlaces = listOf(
    CarvingPlaceTarget(
        regionId = "330100",
        regionName = "杭州市",
        attractionId = "mct_1033",
        attractionName = "杭州市杭州西湖风景区"
    ),
    CarvingPlaceTarget(
        regionId = "140000",
        regionName = "山西省",
        attractionId = "mct_847",
        attractionName = "大同市云冈石窟景区"
    ),
    CarvingPlaceTarget(
        regionId = "140000",
        regionName = "山西省",
        attractionId = "mct_762",
        attractionName = "忻州市雁门关景区"
    ),
    CarvingPlaceTarget(
        regionId = "cn_landscape",
        regionName = "中国山河",
        attractionId = null,
        attractionName = null
    )
)

@Composable
private fun CarvingPlacePickerSheet(
    onDismiss: () -> Unit,
    onPlaceClick: (CarvingPlaceTarget) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x66000000))
                .clickable(onClick = onDismiss)
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 12.dp, vertical = 16.dp)
                .navigationBarsPadding(),
            shape = RoundedCornerShape(24.dp),
            color = MapChinaColors.SurfaceElevated,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "选择留刻地点",
                    color = MapChinaColors.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "先选一处真实山河，再把这一笔刻上去",
                    color = MapChinaColors.TextSecondary,
                    fontSize = 13.sp
                )
                recommendedCarvingPlaces.forEach { place ->
                    CarvingPlaceRow(place = place, onClick = { onPlaceClick(place) })
                }
            }
        }
    }
}

@Composable
private fun CarvingPlaceRow(place: CarvingPlaceTarget, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF8B7355).copy(alpha = 0.10f),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(Color(0xFF8B7355).copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    tint = Color(0xFF8B7355),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    place.attractionName ?: place.regionName,
                    color = MapChinaColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    place.regionName,
                    color = MapChinaColors.TextSecondary,
                    fontSize = 12.sp
                )
            }
            Text("去留刻", color = Color(0xFF8B7355), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun String.regionNameFromTitle(): String {
    return substringAfter("·", missingDelimiterValue = this).trim()
}

@OptIn(ExperimentalFoundationApi::class)
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
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
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
