package com.mapchina.ui.achievement

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapchina.domain.model.AtlasProgress
import com.mapchina.ui.animation.staggeredEntrance
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.border
import com.mapchina.ui.theme.MapChinaColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtlasScreen(
    viewModel: AtlasViewModel? = null,
    onAtlasClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val ui by (viewModel?.ui?.collectAsState() ?: remember { androidx.compose.runtime.mutableStateOf(AtlasUi()) })

    LaunchedEffect(viewModel) { viewModel?.refresh() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
    ) {
        TopAppBar(
            title = { Text("主题图鉴", color = Color.White) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A1A2E))
        )

        if (ui.totalAtlas > 0) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D44))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("图鉴总数", color = Color.Gray, fontSize = 13.sp)
                    Text("${ui.totalAtlas}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.width(24.dp))
                    Text("已完成", color = Color.Gray, fontSize = 13.sp)
                    Text("${ui.completedAtlas}", color = Color(0xFFFFD700), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(ui.atlasProgress, key = { _, item -> item.atlasId }) { index, atlas ->
                AtlasCard(atlas = atlas, onClick = { onAtlasClick?.invoke(atlas.atlasId) }, modifier = Modifier.staggeredEntrance(index))
            }
        }
    }
}

@Composable
private fun AtlasCard(atlas: AtlasProgress, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val progressColor = when {
        atlas.completionPercent >= 100 -> Color(0xFFFFD700)
        atlas.completionPercent >= 50 -> Color(0xFFFF6B6B)
        atlas.completionPercent > 0 -> MapChinaColors.Primary
        else -> Color(0xFF4A6FA5)
    }

    val animatedProgress by animateFloatAsState(
        targetValue = (atlas.completionPercent.toFloat() / 100f).coerceIn(0f, 1f),
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "atlasProgress"
    )

    // Gold glow pulse for completed atlas
    val glowAlpha = remember { Animatable(0.3f) }
    LaunchedEffect(atlas.completionPercent >= 100) {
        if (atlas.completionPercent >= 100) {
            while (true) {
                glowAlpha.animateTo(1f, tween(1000, easing = FastOutSlowInEasing))
                glowAlpha.animateTo(0.3f, tween(1000, easing = FastOutSlowInEasing))
            }
        }
    }
    val borderAlpha = if (atlas.completionPercent >= 100) glowAlpha.value else 0f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (atlas.completionPercent >= 100) {
                    Modifier.border(2.dp, Color(0xFFFFD700).copy(alpha = borderAlpha), RoundedCornerShape(12.dp))
                } else Modifier
            )
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D44))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(progressColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(progressColor)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(atlas.atlasName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(atlas.atlasDescription, color = Color.Gray, fontSize = 12.sp)
                }
                Text("${atlas.visitedItems}/${atlas.totalItems}", color = progressColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = progressColor,
                trackColor = Color(0xFF1A1A2E)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("${atlas.completionPercent}% 完成", color = Color.Gray, fontSize = 11.sp)
        }
    }
}
