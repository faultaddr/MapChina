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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapchina.domain.model.ProvinceConquestInfo
import com.mapchina.ui.theme.MapChinaColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProvinceConquestScreen(
    viewModel: ProvinceConquestViewModel? = null,
    onProvinceClick: ((String) -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val ui by (viewModel?.ui?.collectAsState() ?: remember { androidx.compose.runtime.mutableStateOf(ProvinceConquestUi()) })

    LaunchedEffect(viewModel) { viewModel?.refresh() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F1923))
    ) {
        TopAppBar(
            title = { Text("省份征服", color = Color.White) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F1923))
        )

        // 总览卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2C3D))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("已到访省份", color = Color.Gray, fontSize = 13.sp)
                    Text("${ui.visitedProvinceCount}/${ui.totalProvinceCount}", color = MapChinaColors.Primary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("已完成省份", color = Color.Gray, fontSize = 13.sp)
                    Text("${ui.completedProvinceCount}", color = Color(0xFFFFD700), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 省份列表
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(ui.provinces, key = { it.provinceId }) { province ->
                ProvinceConquestRow(
                    info = province,
                    onClick = { onProvinceClick?.invoke(province.provinceId.substring(0, 2)) }
                )
            }
        }
    }
}

@Composable
private fun ProvinceConquestRow(
    info: ProvinceConquestInfo,
    onClick: () -> Unit
) {
    val progressColor = when (info.colorLevel) {
        0 -> Color(0xFF213647)
        1 -> Color(0xFF4A6FA5)
        2 -> MapChinaColors.Primary
        3 -> Color(0xFFFF6B6B)
        4 -> Color(0xFFFFD700)
        else -> Color(0xFF213647)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2C3D))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(progressColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    info.provinceName,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                if (info.hasCompleteBadge) {
                    Text("已完成", color = Color(0xFFFFD700), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                } else if (info.hasVisitBadge) {
                    Text("已到访", color = MapChinaColors.Primary, fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("${info.completionPercent}%", color = progressColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (info.completionPercent.toFloat() / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = progressColor,
                trackColor = Color(0xFF0F1923)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("城市 ${info.visitedCities}/${info.totalCities}", color = Color.Gray, fontSize = 11.sp)
                Text("景点 ${info.visitedAttractions}/${info.totalAttractions}", color = Color.Gray, fontSize = 11.sp)
            }
        }
    }
}
