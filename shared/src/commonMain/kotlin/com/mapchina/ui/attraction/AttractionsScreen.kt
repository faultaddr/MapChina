package com.mapchina.ui.attraction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.ui.navigation.AttractionDetailScreen
import com.mapchina.ui.theme.MapChinaColors

enum class AttractionFilter(val label: String) {
    ALL("全部"),
    A5("5A"),
    A4("4A"),
    VISITED("已到访"),
    UNVISITED("未到访")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AttractionsScreen(
    navController: NavHostController,
    viewModel: AttractionViewModel? = null,
    modifier: Modifier = Modifier
) {
    val attractions by (viewModel?.attractions?.collectAsState() ?: remember { mutableStateOf(emptyList<AttractionUi>()) })
    val searchQuery by (viewModel?.searchQuery?.collectAsState() ?: remember { mutableStateOf("") })
    var selectedFilter by remember { mutableStateOf(AttractionFilter.ALL) }

    val filteredAttractions = remember(attractions, selectedFilter) {
        when (selectedFilter) {
            AttractionFilter.ALL -> attractions
            AttractionFilter.A5 -> attractions.filter { it.level == "A5" }
            AttractionFilter.A4 -> attractions.filter { it.level == "A4" }
            AttractionFilter.VISITED -> attractions.filter { it.visitLevel != null }
            AttractionFilter.UNVISITED -> attractions.filter { it.visitLevel == null }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F1923))
    ) {
        Text(
            "景点",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel?.searchAttractions(it) },
            label = { Text("搜索景点", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = MapChinaColors.Primary,
                unfocusedBorderColor = Color(0xFF213647),
                cursorColor = MapChinaColors.Primary
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AttractionFilter.values().forEach { filter ->
                val isSelected = selectedFilter == filter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) MapChinaColors.Primary else Color(0xFF1A2C3D))
                        .clickable { selectedFilter = filter }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        filter.label,
                        color = if (isSelected) Color.White else Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredAttractions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (searchQuery.isBlank() && selectedFilter == AttractionFilter.ALL) "输入关键词搜索景点" else "未找到匹配景点",
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredAttractions, key = { it.id }) { attraction ->
                    AttractionCard(
                        attraction = attraction,
                        onClick = {
                            navController.navigate(AttractionDetailScreen(attraction.id))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AttractionCard(
    attraction: AttractionUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isVisited = attraction.visitLevel != null
    val levelBadge = when (attraction.level) {
        "A5" -> "5A"
        "A4" -> "4A"
        else -> attraction.level
    }
    val bgColor = when (attraction.visitLevel) {
        FootprintLevel.DEEP -> MapChinaColors.FootprintDeep.copy(alpha = 0.15f)
        FootprintLevel.SHORT_VISIT -> MapChinaColors.FootprintShortVisit.copy(alpha = 0.15f)
        FootprintLevel.PASS_BY -> MapChinaColors.FootprintPassBy.copy(alpha = 0.15f)
        null -> Color(0xFF1A2C3D)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (attraction.imageUrl != null) {
                SubcomposeAsyncImage(
                    model = attraction.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    val painterState = painter.state.value
                    when (painterState) {
                        is coil3.compose.AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                        else -> {
                            Box(
                                Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF213647)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(attraction.name.take(1), color = Color.Gray, fontSize = 20.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = levelBadge,
                        color = if (attraction.level == "A5") Color(0xFFFFD700) else Color(0xFF90CAF9),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(
                                if (attraction.level == "A5") Color(0xFF332E00) else Color(0xFF0F3347),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 6.dp))
                    Text(
                        text = attraction.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (attraction.description != null) {
                    Text(
                        text = attraction.description,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Text(
                text = when (attraction.visitLevel) {
                    FootprintLevel.DEEP -> "深度"
                    FootprintLevel.SHORT_VISIT -> "短玩"
                    FootprintLevel.PASS_BY -> "路过"
                    null -> "未到访"
                },
                fontSize = 12.sp,
                color = when (attraction.visitLevel) {
                    FootprintLevel.DEEP -> MapChinaColors.FootprintDeep
                    FootprintLevel.SHORT_VISIT -> MapChinaColors.FootprintShortVisit
                    FootprintLevel.PASS_BY -> MapChinaColors.FootprintPassBy
                    null -> Color.Gray
                }
            )
        }
    }
}
