package com.mapchina.ui.attraction

import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.mapchina.data.remote.AttractionDetail
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.ui.theme.MapChinaColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttractionDetailScreen(
    navController: NavHostController,
    attraction: AttractionUi?,
    detail: AttractionDetail?,
    onMarkVisit: (FootprintLevel) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
    ) {
        TopAppBar(
            title = { Text(attraction?.name ?: "景点详情", color = Color.White) },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A1A2E))
        )

        if (attraction == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("景点信息不可用", color = Color.Gray)
            }
            return
        }

        val imageUrls = detail?.imageUrls?.filter { it.isNotBlank() } ?: emptyList()

        if (imageUrls.isNotEmpty()) {
            ImageCarousel(imageUrls = imageUrls)
        } else {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color(0xFF2D2D44)),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无图片", color = Color.Gray)
            }
        }

        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val levelBadge = when (attraction.level) {
                    "A5" -> "5A"
                    "A4" -> "4A"
                    else -> attraction.level
                }
                Text(
                    text = levelBadge,
                    color = if (attraction.level == "A5") Color(0xFFFFD700) else Color(0xFF90CAF9),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(
                            if (attraction.level == "A5") Color(0xFF332200) else Color(0xFF0D2744),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = attraction.name,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            detail?.rating?.let { rating ->
                Spacer(Modifier.height(12.dp))
                RatingRow(rating)
            }

            Spacer(Modifier.height(16.dp))

            detail?.let { d ->
                InfoRows(attraction = attraction, detail = d)
            }

            if (detail == null && attraction.description != null) {
                InfoRow(icon = { Icon(Icons.Default.LocationOn, null, tint = Color(0xFF90CAF9), modifier = Modifier.size(18.dp)) }, label = "地址", value = attraction.description)
            }

            Spacer(Modifier.height(24.dp))

            VisitButton(currentLevel = attraction.visitLevel, onMarkVisit = onMarkVisit)
        }
    }
}

@Composable
private fun ImageCarousel(imageUrls: List<String>) {
    val pagerState = rememberPagerState(pageCount = { imageUrls.size })

    Box(
        Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            SubcomposeAsyncImage(
                model = imageUrls[page],
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            ) {
                val painterState = painter.state.value
                when (painterState) {
                    is coil3.compose.AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                    is coil3.compose.AsyncImagePainter.State.Loading -> {
                        Box(Modifier.fillMaxSize().background(Color(0xFF2D2D44)), contentAlignment = Alignment.Center) {
                            androidx.compose.material3.CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        }
                    }
                    else -> {
                        Box(Modifier.fillMaxSize().background(Color(0xFF2D2D44)), contentAlignment = Alignment.Center) {
                            Text("加载失败", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        if (imageUrls.size > 1) {
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(imageUrls.size) { index ->
                    val selected = pagerState.currentPage == index
                    Box(
                        Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (selected) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (selected) Color.White else Color.White.copy(alpha = 0.4f))
                    )
                }
            }
        }
    }
}

@Composable
private fun RatingRow(rating: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(4.dp))
        Text(rating, color = Color(0xFFFFD700), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(" / 5.0", color = Color.Gray, fontSize = 14.sp)
    }
}

@Composable
private fun InfoRows(attraction: AttractionUi, detail: AttractionDetail) {
    if (detail.openTime != null) {
        InfoRow(
            icon = { Icon(Icons.Default.AccessTime, null, tint = Color(0xFF90CAF9), modifier = Modifier.size(18.dp)) },
            label = "营业",
            value = detail.openTime
        )
        Spacer(Modifier.height(8.dp))
    }
    if (detail.cost != null) {
        InfoRow(
            icon = { Icon(Icons.Default.Payments, null, tint = Color(0xFF90CAF9), modifier = Modifier.size(18.dp)) },
            label = "门票",
            value = detail.cost
        )
        Spacer(Modifier.height(8.dp))
    }
    if (detail.tel != null) {
        InfoRow(
            icon = { Icon(Icons.Default.Call, null, tint = Color(0xFF90CAF9), modifier = Modifier.size(18.dp)) },
            label = "电话",
            value = detail.tel
        )
        Spacer(Modifier.height(8.dp))
    }
    if (detail.website != null) {
        InfoRow(
            icon = { Icon(Icons.Default.Language, null, tint = Color(0xFF90CAF9), modifier = Modifier.size(18.dp)) },
            label = "官网",
            value = detail.website
        )
        Spacer(Modifier.height(8.dp))
    }
    if (attraction.description != null) {
        InfoRow(
            icon = { Icon(Icons.Default.LocationOn, null, tint = Color(0xFF90CAF9), modifier = Modifier.size(18.dp)) },
            label = "地址",
            value = attraction.description
        )
    }
}

@Composable
private fun InfoRow(icon: @Composable () -> Unit, label: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        icon()
        Spacer(Modifier.width(8.dp))
        Text("$label: ", color = Color.Gray, fontSize = 14.sp)
        Text(value, color = Color.White, fontSize = 14.sp)
    }
}

@Composable
private fun VisitButton(currentLevel: FootprintLevel?, onMarkVisit: (FootprintLevel) -> Unit) {
    val visitLabel = when (currentLevel) {
        FootprintLevel.DEEP -> "已深度游览"
        FootprintLevel.SHORT_VISIT -> "已短时游玩"
        FootprintLevel.PASS_BY -> "已路过"
        null -> "标记到访"
    }
    val visitColor = when (currentLevel) {
        FootprintLevel.DEEP -> MapChinaColors.FootprintDeep
        FootprintLevel.SHORT_VISIT -> MapChinaColors.FootprintShortVisit
        FootprintLevel.PASS_BY -> MapChinaColors.FootprintPassBy
        null -> MapChinaColors.Primary
    }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (currentLevel == null) {
            OutlinedButton(
                onClick = { onMarkVisit(FootprintLevel.PASS_BY) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("路过", color = MapChinaColors.FootprintPassBy)
            }
            OutlinedButton(
                onClick = { onMarkVisit(FootprintLevel.SHORT_VISIT) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("短玩", color = MapChinaColors.FootprintShortVisit)
            }
            OutlinedButton(
                onClick = { onMarkVisit(FootprintLevel.DEEP) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("深度", color = MapChinaColors.FootprintDeep)
            }
        } else {
            OutlinedButton(
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                enabled = false
            ) {
                Text(visitLabel, color = visitColor)
            }
        }
    }
}
