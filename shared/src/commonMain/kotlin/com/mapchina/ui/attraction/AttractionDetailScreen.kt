package com.mapchina.ui.attraction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.mapchina.data.remote.AttractionDetail
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.platform.ExternalNavigator
import com.mapchina.domain.model.Journal
import com.mapchina.ui.theme.MapChinaColors
import kotlinx.datetime.Instant
import org.koin.compose.koinInject
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttractionDetailScreen(
    navController: NavHostController,
    attraction: AttractionUi?,
    detail: AttractionDetail?,
    journals: List<Journal> = emptyList(),
    onMarkVisit: (FootprintLevel) -> Unit = {},
    onRemoveVisit: (() -> Unit)? = null,
    onWriteJournal: (() -> Unit)? = null,
    onJournalClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (attraction == null) {
        Box(
            modifier.fillMaxSize().background(MapChinaColors.Background),
            contentAlignment = Alignment.Center
        ) {
            Text("景点信息不可用", color = MapChinaColors.TextTertiary)
        }
        return
    }

    val imageUrls = detail?.imageUrls?.filter { it.isNotBlank() } ?: emptyList()

    Box(modifier = modifier.fillMaxSize().background(MapChinaColors.Background)) {
        Column(Modifier.fillMaxSize()) {
            // Hero image area
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                if (imageUrls.isNotEmpty()) {
                    ImageCarousel(imageUrls = imageUrls)
                } else {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MapChinaColors.SurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MapChinaColors.BorderMedium
                        )
                    }
                }

                // Bottom gradient fade
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MapChinaColors.Background
                                )
                            )
                        )
                )

                // Back button
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(start = 8.dp, top = 4.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.3f))
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Scrollable content
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 0.dp, bottom = 24.dp)
            ) {
                // Title row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val levelBadge = when (attraction.level) {
                        "A5" -> "5A"
                        "A4" -> "4A"
                        else -> attraction.level
                    }
                    if (levelBadge.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (attraction.level == "A5") MapChinaColors.AccentGold.copy(alpha = 0.15f) else MapChinaColors.AccentBlue.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = levelBadge,
                                color = if (attraction.level == "A5") MapChinaColors.AccentGold else MapChinaColors.AccentBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(
                        text = attraction.name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MapChinaColors.TextPrimary
                    )
                }

                // Rating
                detail?.rating?.let { rating ->
                    Spacer(Modifier.height(10.dp))
                    RatingRow(rating)
                }

                // Info card
                if (detail != null || attraction.description != null) {
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MapChinaColors.SurfaceElevated,
                        shadowElevation = 1.dp
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            detail?.let { d ->
                                InfoRows(attraction = attraction, detail = d)
                            }
                            if (detail == null && attraction.description != null) {
                                InfoRow(icon = Icons.Default.LocationOn, label = "地址", value = attraction.description)
                            }
                        }
                    }
                }

                // Navigate button
                val navigator: ExternalNavigator = koinInject()
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        navigator.navigateToAmap(attraction.latitude, attraction.longitude, attraction.name)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MapChinaColors.Primary
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MapChinaColors.Primary.copy(alpha = 0.3f))
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("高德导航", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }

                // Visit section
                Spacer(Modifier.height(20.dp))
                VisitSection(currentLevel = attraction.visitLevel, onMarkVisit = onMarkVisit, onRemoveVisit = onRemoveVisit)

                // Write journal button
                if (onWriteJournal != null) {
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { onWriteJournal.invoke() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MapChinaColors.Primary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Article, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("写游记", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Journals
                if (journals.isNotEmpty()) {
                    Spacer(Modifier.height(24.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(16.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MapChinaColors.Primary)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "相关游记",
                            color = MapChinaColors.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${journals.size} 篇",
                            color = MapChinaColors.TextTertiary,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    journals.forEach { journal ->
                        JournalCard(
                            journal = journal,
                            onClick = { onJournalClick?.invoke(journal.id) }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageCarousel(imageUrls: List<String>) {
    val pagerState = rememberPagerState(pageCount = { imageUrls.size })

    Box(Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            AsyncImage(
                model = imageUrls[page],
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (imageUrls.size > 1) {
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.3f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(imageUrls.size) { index ->
                    val selected = pagerState.currentPage == index
                    Box(
                        Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (selected) 8.dp else 5.dp)
                            .clip(CircleShape)
                            .background(if (selected) Color.White else Color.White.copy(alpha = 0.45f))
                    )
                }
            }
        }
    }
}

@Composable
private fun RatingRow(rating: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Star, contentDescription = null, tint = MapChinaColors.AccentGold, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(4.dp))
        Text(rating, color = MapChinaColors.AccentGold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(" / 5.0", color = MapChinaColors.TextTertiary, fontSize = 13.sp)
    }
}

@Composable
private fun InfoRows(attraction: AttractionUi, detail: AttractionDetail) {
    if (detail.openTime != null) {
        InfoRow(icon = Icons.Default.AccessTime, label = "营业", value = detail.openTime)
        Spacer(Modifier.height(10.dp))
    }
    if (detail.cost != null) {
        InfoRow(icon = Icons.Default.Payments, label = "门票", value = detail.cost)
        Spacer(Modifier.height(10.dp))
    }
    if (detail.tel != null) {
        InfoRow(icon = Icons.Default.Call, label = "电话", value = detail.tel)
        Spacer(Modifier.height(10.dp))
    }
    if (detail.website != null) {
        InfoRow(icon = Icons.Default.Language, label = "官网", value = detail.website)
        Spacer(Modifier.height(10.dp))
    }
    if (detail.appointmentUrl != null) {
        InfoRow(icon = Icons.Default.ConfirmationNumber, label = "预约", value = detail.appointmentUrl)
        Spacer(Modifier.height(10.dp))
    }
    if (attraction.description != null) {
        InfoRow(icon = Icons.Default.LocationOn, label = "地址", value = attraction.description)
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(MapChinaColors.Primary.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MapChinaColors.Primary, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = MapChinaColors.TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Text(value, color = MapChinaColors.TextPrimary, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun VisitSection(
    currentLevel: FootprintLevel?,
    onMarkVisit: (FootprintLevel) -> Unit,
    onRemoveVisit: (() -> Unit)? = null
) {
    val visitLabel = when (currentLevel) {
        FootprintLevel.DEEP -> "深度游览"
        FootprintLevel.SHORT_VISIT -> "短暂停留"
        FootprintLevel.PASS_BY -> "路过"
        null -> "标记到访"
    }
    val visitColor = when (currentLevel) {
        FootprintLevel.DEEP -> MapChinaColors.FootprintDeep
        FootprintLevel.SHORT_VISIT -> MapChinaColors.FootprintShortVisit
        FootprintLevel.PASS_BY -> MapChinaColors.FootprintPassBy
        null -> MapChinaColors.Primary
    }

    var showRemoveDialog by remember { mutableStateOf(false) }

    Column {
        if (currentLevel != null) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = visitColor.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(visitColor)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("当前: $visitLabel", color = visitColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (currentLevel == null) {
                VisitChip("路过", MapChinaColors.FootprintPassBy) { onMarkVisit(FootprintLevel.PASS_BY) }
                VisitChip("短玩", MapChinaColors.FootprintShortVisit) { onMarkVisit(FootprintLevel.SHORT_VISIT) }
                VisitChip("深度", MapChinaColors.FootprintDeep) { onMarkVisit(FootprintLevel.DEEP) }
            } else {
                if (currentLevel < FootprintLevel.SHORT_VISIT) {
                    VisitChip("升级短玩", MapChinaColors.FootprintShortVisit) { onMarkVisit(FootprintLevel.SHORT_VISIT) }
                }
                if (currentLevel < FootprintLevel.DEEP) {
                    VisitChip("升级深度", MapChinaColors.FootprintDeep) { onMarkVisit(FootprintLevel.DEEP) }
                }
                VisitChip("撤销", MapChinaColors.Error) { showRemoveDialog = true }
            }
        }
    }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("确认撤销", color = MapChinaColors.TextPrimary) },
            text = { Text("确定要撤销「$visitLabel」的到访记录吗？", color = MapChinaColors.TextTertiary) },
            confirmButton = {
                TextButton(onClick = {
                    showRemoveDialog = false
                    onRemoveVisit?.invoke()
                }) { Text("撤销", color = MapChinaColors.Error) }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) { Text("取消", color = MapChinaColors.TextTertiary) }
            },
            containerColor = MapChinaColors.SurfaceElevated
        )
    }
}

@Composable
private fun RowScope.VisitChip(label: String, color: Color, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.1f),
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(label, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun JournalCard(journal: Journal, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MapChinaColors.SurfaceElevated,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left accent bar
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MapChinaColors.Primary)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    journal.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MapChinaColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (journal.description.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        journal.description,
                        fontSize = 12.sp,
                        color = MapChinaColors.TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                formatJournalDate(journal.startTime),
                fontSize = 12.sp,
                color = MapChinaColors.TextTertiary
            )
        }
    }
}

private fun formatJournalDate(timestamp: Long): String {
    return try {
        val local = Instant.fromEpochMilliseconds(timestamp)
            .toLocalDateTime(TimeZone.currentSystemDefault())
        "${local.monthNumber}月${local.dayOfMonth}日"
    } catch (_: Exception) {
        ""
    }
}
