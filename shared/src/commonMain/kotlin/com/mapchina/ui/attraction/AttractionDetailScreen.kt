package com.mapchina.ui.attraction

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import coil3.compose.AsyncImage
import com.mapchina.data.remote.AttractionDetail
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.platform.ExternalNavigator
import com.mapchina.platform.HapticType
import com.mapchina.platform.LocalHapticFeedback
import com.mapchina.domain.model.Journal
import com.mapchina.ui.theme.MapChinaColors
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Instant
import org.koin.compose.koinInject
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttractionDetailScreen(
    onNavigate: (NavKey) -> Unit,
    onBack: () -> Unit,
    attraction: AttractionUi?,
    detail: AttractionDetail?,
    journals: List<Journal> = emptyList(),
    onMarkVisit: (FootprintLevel) -> Unit = {},
    onRemoveVisit: (() -> Unit)? = null,
    onWriteJournal: (() -> Unit)? = null,
    onJournalClick: ((String) -> Unit)? = null,
    onOpenCarving: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
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
    var showFullscreen by remember { mutableStateOf(false) }
    var fullscreenStartPage by remember { mutableStateOf(0) }

    // Hero zoom-in animation
    val heroScale = remember { Animatable(0.82f) }
    val heroAlpha = remember { Animatable(0f) }
    val heroOffsetY = remember { Animatable(30f) }
    val contentAlpha = remember { Animatable(0f) }
    val backAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.coroutineScope {
            launch { heroAlpha.animateTo(1f, tween(350)) }
            launch { heroScale.animateTo(1f, spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)) }
            launch { heroOffsetY.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)) }
            launch { backAlpha.animateTo(1f, tween(250, delayMillis = 100)) }
            launch { contentAlpha.animateTo(1f, tween(400, delayMillis = 180)) }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(MapChinaColors.Background)) {
        Column(Modifier.fillMaxSize()) {
            // Hero image area - 标题叠加在图上
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(320.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = heroScale.value
                            scaleY = heroScale.value
                            translationY = heroOffsetY.value
                            alpha = heroAlpha.value
                        }
                ) {
                    if (imageUrls.isNotEmpty()) {
                        ImageCarousel(
                            imageUrls = imageUrls,
                            onImageClick = {
                                fullscreenStartPage = it
                                showFullscreen = true
                            }
                        )
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
                                tint = MapChinaColors.BorderSubtle
                            )
                        }
                    }
                }

                // Bottom gradient + title overlay
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.6f)
                                )
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Column {
                        // Title + badges
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val levelBadge = when (attraction.level) {
                                "A5" -> "5A"
                                "A4" -> "4A"
                                else -> attraction.level
                            }
                            if (levelBadge.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (attraction.level == "A5") MapChinaColors.AccentGold else MapChinaColors.AccentBlue
                                ) {
                                    Text(
                                        text = levelBadge,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(
                                text = attraction.name,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        // Rating
                        detail?.rating?.let { rating ->
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = MapChinaColors.AccentGold, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(rating, color = MapChinaColors.AccentGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(" / 5.0", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Back button
                com.mapchina.ui.common.BackButton(
                    onClick = onBack,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(start = 8.dp, top = 4.dp),
                    tint = Color.White.copy(alpha = backAlpha.value),
                    backgroundColor = Color.Black.copy(alpha = 0.3f * backAlpha.value)
                )
            }

            // Scrollable content
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp, bottom = 24.dp)
                    .graphicsLayer { alpha = contentAlpha.value }
            ) {
                // Info pills - 横向标签组
                if (detail != null) {
                    val infoPills = buildList {
                        detail.openTime?.let { add("🕐 $it") }
                        detail.cost?.let { add("🎫 $it") }
                    }
                    if (infoPills.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            infoPills.forEach { pill ->
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MapChinaColors.Primary.copy(alpha = 0.08f)
                                ) {
                                    Text(
                                        pill,
                                        fontSize = 12.sp,
                                        color = MapChinaColors.TextSecondary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 地址/电话/官网 - 紧凑行
                    CompactInfoRow(icon = Icons.Default.LocationOn, value = attraction.description)
                    CompactInfoRow(icon = Icons.Default.Call, value = detail.tel)
                    CompactInfoRow(icon = Icons.Default.Language, value = detail.website)
                }

                // Navigate + Write journal 并排按钮
                val navigator: ExternalNavigator = koinInject()
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            haptic.perform(HapticType.MEDIUM)
                            navigator.navigateToAmap(attraction.latitude, attraction.longitude, attraction.name)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MapChinaColors.Primary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("导航", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    if (onWriteJournal != null) {
                        OutlinedButton(
                            onClick = { haptic.perform(HapticType.LIGHT); onWriteJournal.invoke() },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MapChinaColors.Primary
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MapChinaColors.Primary.copy(alpha = 0.3f))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Article, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("写游记", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Appointment button
                if (detail?.appointmentUrl != null) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { navigator.openUrl(detail.appointmentUrl!!) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MapChinaColors.Primary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MapChinaColors.Primary.copy(alpha = 0.3f))
                    ) {
                        Icon(Icons.Default.ConfirmationNumber, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("在线预约", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Visit section - 独立卡片
                Spacer(Modifier.height(16.dp))
                VisitCard(currentLevel = attraction.visitLevel, onMarkVisit = { level -> haptic.perform(HapticType.SUCCESS); onMarkVisit(level) }, onRemoveVisit = { haptic.perform(HapticType.WARNING); onRemoveVisit?.invoke() })

                // Carving - 折叠入口
                if (onOpenCarving != null) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MapChinaColors.SurfaceElevated,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { haptic.perform(HapticType.LIGHT); onOpenCarving.invoke() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF8B7355), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("留碑刻", fontSize = 14.sp, color = MapChinaColors.TextSecondary)
                            Spacer(Modifier.weight(1f))
                            Text("›", fontSize = 16.sp, color = MapChinaColors.TextTertiary)
                        }
                    }
                }

                // Journals
                if (journals.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
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

    // Fullscreen image viewer
    if (showFullscreen) {
        FullscreenImageViewer(
            imageUrls = imageUrls,
            startIndex = fullscreenStartPage,
            onDismiss = { showFullscreen = false }
        )
    }
}

@Composable
private fun CompactInfoRow(icon: ImageVector, value: String?) {
    if (value == null) return
    Row(
        modifier = Modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MapChinaColors.TextTertiary, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(8.dp))
        Text(value, fontSize = 13.sp, color = MapChinaColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ImageCarousel(imageUrls: List<String>, onImageClick: (Int) -> Unit = {}) {
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
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { onImageClick(page) }
                    }
            )
        }

        if (imageUrls.size > 1) {
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 60.dp)
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
private fun FullscreenImageViewer(
    imageUrls: List<String>,
    startIndex: Int = 0,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = startIndex, pageCount = { imageUrls.size })
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 5f)
                    if (scale < 1.05f) {
                        scale = 1f; offsetX = 0f; offsetY = 0f
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1.05f) {
                            scale = 1f; offsetX = 0f; offsetY = 0f
                        } else {
                            scale = 2.5f
                        }
                    },
                    onTap = {
                        if (scale <= 1.05f) onDismiss()
                    }
                )
            }
            .statusBarsPadding()
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            AsyncImage(
                model = imageUrls[page],
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale; scaleY = scale
                        translationX = offsetX; translationY = offsetY
                    }
            )
        }

        com.mapchina.ui.common.BackButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            tint = Color.White,
            backgroundColor = Color.White.copy(alpha = 0.2f)
        )

        if (imageUrls.size > 1) {
            Text(
                "${pagerState.currentPage + 1} / ${imageUrls.size}",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            )
        }
    }
}

@Composable
private fun VisitCard(
    currentLevel: FootprintLevel?,
    onMarkVisit: (FootprintLevel) -> Unit,
    onRemoveVisit: (() -> Unit)? = null
) {
    var showRemoveDialog by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MapChinaColors.SurfaceElevated,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("到访状态", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MapChinaColors.TextTertiary)
            Spacer(Modifier.height(10.dp))

            if (currentLevel != null) {
                // 已标记 - 显示当前状态
                val visitColor = when (currentLevel) {
                    FootprintLevel.DEEP -> MapChinaColors.FootprintDeep
                    FootprintLevel.SHORT_VISIT -> MapChinaColors.FootprintShortVisit
                    FootprintLevel.PASS_BY -> MapChinaColors.FootprintPassBy
                }
                val visitLabel = when (currentLevel) {
                    FootprintLevel.DEEP -> "深游"
                    FootprintLevel.SHORT_VISIT -> "小驻"
                    FootprintLevel.PASS_BY -> "途经"
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(visitColor)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(visitLabel, color = visitColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    // 升级/撤销按钮
                    if (currentLevel < FootprintLevel.SHORT_VISIT) {
                        UpgradeChip("升级小驻", MapChinaColors.FootprintShortVisit) { onMarkVisit(FootprintLevel.SHORT_VISIT) }
                        Spacer(Modifier.width(6.dp))
                    }
                    if (currentLevel < FootprintLevel.DEEP) {
                        UpgradeChip("升级深游", MapChinaColors.FootprintDeep) { onMarkVisit(FootprintLevel.DEEP) }
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        "撤销",
                        color = MapChinaColors.Error,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable { showRemoveDialog = true }
                    )
                }
            } else {
                // 未标记 - Radio 圆点选择
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VisitRadio("路过", MapChinaColors.FootprintPassBy, modifier = Modifier.weight(1f)) { onMarkVisit(FootprintLevel.PASS_BY) }
                    VisitRadio("小驻", MapChinaColors.FootprintShortVisit, modifier = Modifier.weight(1f)) { onMarkVisit(FootprintLevel.SHORT_VISIT) }
                    VisitRadio("深游", MapChinaColors.FootprintDeep, modifier = Modifier.weight(1f)) { onMarkVisit(FootprintLevel.DEEP) }
                }
            }
        }
    }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("确认撤销", color = MapChinaColors.TextPrimary) },
            text = { Text("确定要撤销到访记录吗？", color = MapChinaColors.TextTertiary) },
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
private fun RowScope.VisitRadio(label: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.08f),
        modifier = modifier
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color.Transparent)
                    .clip(CircleShape)
                    .then(
                        Modifier.background(color.copy(alpha = 0.15f))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(label, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun UpgradeChip(label: String, color: Color, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.1f),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            label,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
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
