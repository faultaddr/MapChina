package com.mapchina.ui.carving

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapchina.domain.model.Carving
import com.mapchina.ui.carving.v2.CarvingDecodeResult
import com.mapchina.ui.carving.v2.CarvingDocumentCodec
import com.mapchina.ui.theme.MapChinaColors
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

fun carvingContextTarget(title: String, regionId: String?, attractionId: String?): CarvingPlaceTarget =
    CarvingPlaceTarget(
        regionId = regionId?.takeIf(String::isNotBlank) ?: "cn_landscape",
        regionName = title.substringAfter("·", title).trim().ifBlank { "中国山河" },
        attractionId = attractionId
    )

@Composable
fun CarvingListScreen(
    viewModel: CarvingViewModel,
    title: String = "碑刻",
    regionId: String? = null,
    attractionId: String? = null,
    showAll: Boolean = false,
    onCreateClick: (CarvingPlaceTarget) -> Unit,
    onEditClick: (Carving) -> Unit = {},
    onBack: () -> Unit
) {
    val carvings by viewModel.carvingList.collectAsState()
    var deleteTarget by remember { mutableStateOf<Carving?>(null) }
    var showPlacePicker by remember { mutableStateOf(false) }
    val contextTarget = remember(title, regionId, attractionId) {
        carvingContextTarget(title, regionId, attractionId)
    }

    LaunchedEffect(regionId, attractionId, showAll) {
        when {
            showAll -> viewModel.loadAllCarvings()
            attractionId != null -> viewModel.loadCarvingsByAttraction(attractionId)
            regionId != null -> viewModel.loadCarvingsByRegion(regionId)
        }
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
                }) { Text("删除", color = MapChinaColors.Error) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("取消") } }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(MapChinaColors.Background)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                com.mapchina.ui.common.BackButton(onClick = onBack)
                Text(title, color = MapChinaColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            if (carvings.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = MapChinaColors.TextTertiary, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("还没有碑刻", color = MapChinaColors.TextSecondary, fontWeight = FontWeight.Medium)
                    }
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalItemSpacing = 8.dp,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(carvings, key = { it.id }) { carving ->
                        CarvingGalleryCard(
                            carving = carving,
                            onClick = { onEditClick(carving) },
                            onLongPress = { deleteTarget = carving }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                if (showAll && regionId.isNullOrBlank() && attractionId.isNullOrBlank()) showPlacePicker = true
                else onCreateClick(contextTarget)
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            containerColor = MapChinaColors.Primary,
            contentColor = MapChinaColors.SurfaceElevated
        ) { Icon(Icons.Default.Add, contentDescription = "新碑刻") }

        if (showPlacePicker) {
            CarvingPlacePicker(
                onDismiss = { showPlacePicker = false },
                onSelect = { target -> showPlacePicker = false; onCreateClick(target) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CarvingGalleryCard(carving: Carving, onClick: () -> Unit, onLongPress: () -> Unit) {
    val decoded = remember(carving.strokeData, carving.previewAspectRatio) {
        CarvingDocumentCodec.decode(carving.strokeData.orEmpty(), carving.previewAspectRatio)
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MapChinaColors.SurfaceElevated,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).combinedClickable(onClick = onClick, onLongClick = onLongPress)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                val document = (decoded as? CarvingDecodeResult.Success)?.document
                if (document != null) {
                    CarvingArtwork(document = document, modifier = Modifier.fillMaxSize(), weatheredAlpha = 0.88f)
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(MapChinaColors.SurfaceOverlay), contentAlignment = Alignment.Center) {
                        Text("无法读取", color = MapChinaColors.TextTertiary, fontSize = 12.sp)
                    }
                }
            }
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(
                    carvingPlaceTitle(carving.regionName, carving.attractionName),
                    color = MapChinaColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(formatCarvingDate(carving.createdAt), color = MapChinaColors.TextTertiary, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun CarvingPlacePicker(onDismiss: () -> Unit, onSelect: (CarvingPlaceTarget) -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MapChinaColors.Background.copy(alpha = 0.62f))
                .clickable(onClick = onDismiss)
        )
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MapChinaColors.SurfaceElevated,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("选择留刻地点", color = MapChinaColors.TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("取消") }
                }
                recommendedCarvingPlaces.forEach { place ->
                    TextButton(onClick = { onSelect(place) }, modifier = Modifier.fillMaxWidth()) {
                        Text(place.attractionName ?: place.regionName, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private val recommendedCarvingPlaces = listOf(
    CarvingPlaceTarget("330100", "杭州市", "mct_1033", "杭州市杭州西湖风景区"),
    CarvingPlaceTarget("140000", "山西省", "mct_847", "大同市云冈石窟景区"),
    CarvingPlaceTarget("140000", "山西省", "mct_762", "忻州市雁门关景区"),
    CarvingPlaceTarget("cn_landscape", "中国山河")
)

private fun formatCarvingDate(timestamp: Long): String = try {
    val date = Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(TimeZone.currentSystemDefault())
    "${date.year}年${date.monthNumber}月${date.dayOfMonth}日"
} catch (_: Exception) {
    ""
}
