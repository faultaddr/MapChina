package com.mapchina.ui.carving

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapchina.domain.model.Carving
import com.mapchina.ui.theme.MapChinaColors
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun CarvingListScreen(
    viewModel: CarvingViewModel,
    title: String = "碑刻",
    regionId: String? = null,
    attractionId: String? = null,
    showAll: Boolean = false,
    onCreateClick: () -> Unit,
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(carvings, key = { it.id }) { carving ->
                        CarvingCard(
                            carving = carving,
                            onDelete = { deleteTarget = carving }
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
private fun CarvingCard(carving: Carving, onDelete: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(MapChinaColors.CardBackground, Color(0xFF2A2218))
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    carving.attractionName ?: carving.regionName,
                    color = MapChinaColors.TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MapChinaColors.TextTertiary.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = Color(0xFF8B7355).copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(formatDate(carving.createdAt), color = Color(0xFF8B7355).copy(alpha = 0.85f), fontSize = 12.sp)
                if (carving.attractionName != null) {
                    Spacer(modifier = Modifier.width(14.dp))
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MapChinaColors.FootprintShortVisit.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(carving.regionName, color = MapChinaColors.FootprintShortVisit.copy(alpha = 0.7f), fontSize = 12.sp)
                }
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
