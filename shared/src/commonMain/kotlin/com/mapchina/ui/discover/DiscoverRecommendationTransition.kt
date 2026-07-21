package com.mapchina.ui.discover

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.mapchina.performance.RecompositionProbe
import com.mapchina.ui.theme.MapChinaColors
import com.mapchina.ui.theme.MapChinaTypography
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

data class RecommendationTransitionFrame(
    val bounds: Rect,
    val cornerRadiusDp: Float,
    val backgroundProgress: Float
)

fun recommendationTransitionFrame(
    start: Rect,
    viewportWidthPx: Float,
    heroHeightPx: Float,
    progress: Float
): RecommendationTransitionFrame {
    val fraction = progress.coerceIn(0f, 1f)
    return RecommendationTransitionFrame(
        bounds = Rect(
            left = transitionLerp(start.left, 0f, fraction),
            top = transitionLerp(start.top, 0f, fraction),
            right = transitionLerp(start.right, viewportWidthPx, fraction),
            bottom = transitionLerp(start.bottom, heroHeightPx, fraction)
        ),
        cornerRadiusDp = transitionLerp(8f, 0f, fraction),
        backgroundProgress = fraction
    )
}

private fun transitionLerp(start: Float, end: Float, fraction: Float): Float =
    start + (end - start) * fraction

data class RecommendationTransitionTarget(
    val recommendation: DiscoverRecommendation,
    val rank: Int,
    val startBounds: Rect
)

@Composable
fun RecommendationImageCard(
    recommendation: DiscoverRecommendation,
    rank: Int,
    onClick: (Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    RecompositionProbe("RecommendationImageCard")

    var bounds by remember(recommendation.id) { mutableStateOf(Rect.Zero) }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
        shadowElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(148.dp)
            .onGloballyPositioned { bounds = it.boundsInRoot() }
            .semantics { contentDescription = "打开景点详情：${recommendation.title}" }
            .clickable(enabled = bounds != Rect.Zero) { onClick(bounds) }
    ) {
        RecommendationImageLayer(
            recommendation = recommendation,
            rank = rank,
            transitionProgress = 0f,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun RecommendationTransitionOverlay(
    target: RecommendationTransitionTarget,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .zIndex(20f)
            .pointerInput(target.recommendation.id) {
                detectTapGestures(onTap = {})
            }
    ) {
        val viewportWidthPx = constraints.maxWidth.toFloat()
        val viewportHeightPx = constraints.maxHeight.toFloat()
        val heroHeightPx = with(density) { 320.dp.toPx() }
        val frame = recommendationTransitionFrame(
            start = target.startBounds,
            viewportWidthPx = viewportWidthPx,
            heroHeightPx = heroHeightPx,
            progress = progress
        )
        val origin = target.startBounds.center
        val horizontalRadius = max(origin.x, viewportWidthPx - origin.x)
        val verticalRadius = max(origin.y, viewportHeightPx - origin.y)
        val revealRadius = hypot(horizontalRadius, verticalRadius) * frame.backgroundProgress

        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = MapChinaColors.Background,
                radius = revealRadius,
                center = origin
            )
        }

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = frame.bounds.left.roundToInt(),
                        y = frame.bounds.top.roundToInt()
                    )
                }
                .size(
                    width = with(density) { frame.bounds.width.toDp() },
                    height = with(density) { frame.bounds.height.toDp() }
                )
                .clip(RoundedCornerShape(frame.cornerRadiusDp.dp))
        ) {
            RecommendationImageLayer(
                recommendation = target.recommendation,
                rank = target.rank,
                transitionProgress = progress,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun RecommendationImageLayer(
    recommendation: DiscoverRecommendation,
    rank: Int,
    transitionProgress: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(Color(0xFF173B35))
    ) {
        Icon(
            imageVector = Icons.Default.Landscape,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.54f),
            modifier = Modifier
                .align(Alignment.Center)
                .size(42.dp)
        )
        if (recommendation.imageUrl.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = "${recommendation.title}头图" }
            )
        } else {
            AsyncImage(
                model = recommendation.imageUrl,
                contentDescription = "${recommendation.title}头图",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.24f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.88f)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (transitionProgress < 0.72f) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.38f)
                ) {
                    Text(
                        text = rank.toString().padStart(2, '0'),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (recommendation.levelLabel == "5A") {
                    MapChinaColors.AccentGold.copy(alpha = 0.94f)
                } else {
                    MapChinaColors.AccentBlue.copy(alpha = 0.94f)
                }
            ) {
                Text(
                    text = recommendation.levelLabel,
                    color = Color.White,
                    style = MapChinaTypography.Caption,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = recommendation.title,
                color = Color.White,
                style = MapChinaTypography.Title,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = recommendation.subtitle,
                color = Color.White.copy(alpha = 0.78f),
                style = MapChinaTypography.Caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (transitionProgress < 0.84f) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = recommendation.reason,
                        color = Color.White.copy(alpha = 0.9f),
                        style = MapChinaTypography.Caption,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
