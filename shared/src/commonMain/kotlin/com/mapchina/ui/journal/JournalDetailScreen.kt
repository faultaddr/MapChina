package com.mapchina.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapchina.domain.model.JournalDetail
import com.mapchina.domain.model.JournalPhoto
import com.mapchina.ui.theme.MapChinaColors
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun JournalDetailScreen(
    journalId: String,
    viewModel: JournalViewModel,
    onBack: () -> Unit,
    onDelete: () -> Unit
) {
    val ui by viewModel.detailUi.collectAsState()

    LaunchedEffect(journalId) { viewModel.loadJournalDetail(journalId) }

    val detail = ui.detail

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MapChinaColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
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
                detail?.journal?.title ?: "游记详情",
                color = MapChinaColors.TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            if (detail != null) {
                IconButton(onClick = {
                    viewModel.deleteJournal(journalId)
                    onDelete()
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = MapChinaColors.FootprintDeep.copy(alpha = 0.7f))
                }
            }
        }

        if (ui.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MapChinaColors.Primary)
            }
        } else if (detail != null) {
            JournalDetailContent(detail)
        }
    }
}

@Composable
private fun JournalDetailContent(detail: JournalDetail) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF1A3040), MapChinaColors.CardBackground)
                    )
                )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    detail.journal.title,
                    color = MapChinaColors.TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                if (detail.journal.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        detail.journal.description,
                        color = MapChinaColors.TextSecondary,
                        fontSize = 15.sp,
                        lineHeight = 24.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MapChinaColors.Primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(formatDate(detail.journal.startTime), color = MapChinaColors.Primary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    detail.journal.endTime?.let { end ->
                        Text(" — ", color = MapChinaColors.TextTertiary, fontSize = 13.sp)
                        Text(formatDate(end), color = MapChinaColors.Primary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
                if (detail.regionName != null || detail.attractionName != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MapChinaColors.FootprintShortVisit
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val locationText = listOfNotNull(detail.regionName, detail.attractionName).joinToString(" · ")
                        Text(locationText, color = MapChinaColors.FootprintShortVisit, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        if (detail.photos.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            SectionHeader("照片", detail.photos.size)
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(detail.photos, key = { it.id }) { photo ->
                    PhotoThumbnail(photo)
                }
            }
        }

        if (detail.trackPoints.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MapChinaColors.CardBackground)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MapChinaColors.Primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Route, contentDescription = null, modifier = Modifier.size(20.dp), tint = MapChinaColors.Primary)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("行动轨迹", color = MapChinaColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("${detail.trackPoints.size} 个轨迹点", color = MapChinaColors.TextTertiary, fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = MapChinaColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.width(8.dp))
        Text("($count)", color = MapChinaColors.TextTertiary, fontSize = 13.sp)
    }
}

@Composable
private fun PhotoThumbnail(photo: JournalPhoto) {
    AsyncImage(
        model = photo.localPath,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(110.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MapChinaColors.CardBackgroundLight)
    )
}

private fun formatDate(timestamp: Long): String {
    return try {
        val local = Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(TimeZone.currentSystemDefault())
        "${local.year}年${local.monthNumber}月${local.dayOfMonth}日"
    } catch (_: Exception) {
        ""
    }
}
