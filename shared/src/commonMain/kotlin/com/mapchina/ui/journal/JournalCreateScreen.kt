package com.mapchina.ui.journal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapchina.ui.theme.MapChinaColors
import coil3.compose.AsyncImage
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun JournalCreateScreen(
    viewModel: JournalViewModel,
    regionId: String? = null,
    attractionId: String? = null,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val createUi by viewModel.createUi.collectAsState()
    var showLocationSearch by remember { mutableStateOf(false) }

    LaunchedEffect(attractionId) {
        attractionId?.let { viewModel.setInitialAttraction(it) }
    }

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
                "写游记",
                color = MapChinaColors.TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            SectionLabel("标题")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("给这次旅行取个名字", color = MapChinaColors.TextTertiary) },
                modifier = Modifier.fillMaxWidth(),
                colors = outLinedTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            SectionLabel("描述")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("记录旅途中的故事…", color = MapChinaColors.TextTertiary) },
                modifier = Modifier.fillMaxWidth().height(110.dp),
                colors = outLinedTextFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            SectionLabel("目的地")
            Spacer(modifier = Modifier.height(8.dp))

            if (createUi.selectedLocation != null) {
                SelectedLocationChip(
                    item = createUi.selectedLocation!!,
                    onRemove = { viewModel.clearLocation() }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MapChinaColors.CardBackground)
                        .clickable { showLocationSearch = !showLocationSearch }
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MapChinaColors.TextTertiary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("搜索省/市/县/景点", color = MapChinaColors.TextTertiary, fontSize = 15.sp)
                    }
                }
            }

            AnimatedVisibility(visible = showLocationSearch && createUi.selectedLocation == null) {
                LocationSearchPanel(
                    query = createUi.searchQuery,
                    results = createUi.searchResults,
                    onQueryChange = { viewModel.searchLocations(it) },
                    onSelect = { item ->
                        viewModel.selectLocation(item)
                        showLocationSearch = false
                    }
                )
            }

            if (viewModel.canPickPhotos()) {
                Spacer(modifier = Modifier.height(16.dp))
                SectionLabel("照片")
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(createUi.selectedPhotoPaths) { path ->
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = path,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(88.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MapChinaColors.CardBackgroundLight)
                            )
                            IconButton(
                                onClick = { viewModel.removeSelectedPhoto(path) },
                                modifier = Modifier
                                    .size(22.dp)
                                    .align(Alignment.TopEnd)
                                    .padding(3.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "移除",
                                    modifier = Modifier.size(14.dp),
                                    tint = MapChinaColors.TextPrimary.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                    item {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MapChinaColors.CardBackground)
                                .clickable { viewModel.pickPhotos() },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "添加照片",
                                    modifier = Modifier.size(24.dp),
                                    tint = MapChinaColors.Primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("添加", color = MapChinaColors.Primary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MapChinaColors.TextTertiary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(formatDate(Clock.System.now().toEpochMilliseconds()), color = MapChinaColors.TextTertiary, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        viewModel.createJournal(
                            title = title,
                            description = description,
                            startTime = Clock.System.now().toEpochMilliseconds()
                        )
                        onSave()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MapChinaColors.Primary,
                    disabledContainerColor = MapChinaColors.Primary.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(14.dp),
                enabled = title.isNotBlank()
            ) {
                Text(
                    if (title.isBlank()) "请输入标题" else "保存游记",
                    color = MapChinaColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = MapChinaColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
}

@Composable
private fun outLinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MapChinaColors.Primary,
    unfocusedBorderColor = MapChinaColors.BorderSubtle,
    focusedLabelColor = MapChinaColors.Primary,
    unfocusedLabelColor = MapChinaColors.TextTertiary,
    cursorColor = MapChinaColors.Primary,
    focusedTextColor = MapChinaColors.TextPrimary,
    unfocusedTextColor = MapChinaColors.TextSecondary,
    focusedContainerColor = MapChinaColors.Background,
    unfocusedContainerColor = MapChinaColors.Background
)

@Composable
private fun SelectedLocationChip(item: LocationItem, onRemove: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF1A3040), MapChinaColors.CardBackground)
                )
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(locationTypeColor(item.type).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    locationTypeIcon(item.type),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = locationTypeColor(item.type)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, color = MapChinaColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(locationTypeLabel(item), color = MapChinaColors.TextTertiary, fontSize = 12.sp)
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, contentDescription = "移除", modifier = Modifier.size(16.dp), tint = MapChinaColors.TextTertiary)
            }
        }
    }
}

@Composable
private fun LocationSearchPanel(
    query: String,
    results: List<LocationItem>,
    onQueryChange: (String) -> Unit,
    onSelect: (LocationItem) -> Unit
) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("输入省/市/县/景点名称", color = MapChinaColors.TextTertiary) },
            modifier = Modifier.fillMaxWidth(),
            colors = outLinedTextFieldColors(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = MapChinaColors.TextTertiary, modifier = Modifier.size(20.dp))
            }
        )
        if (results.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MapChinaColors.CardBackground),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(results, key = { "${it.type}_${it.id}" }) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(item) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            locationTypeIcon(item.type),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = locationTypeColor(item.type).copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, color = MapChinaColors.TextPrimary, fontSize = 14.sp)
                            Text(locationTypeLabel(item), color = MapChinaColors.TextTertiary, fontSize = 12.sp)
                        }
                    }
                    if (item != results.last()) {
                        HorizontalDivider(color = MapChinaColors.BorderSubtle, thickness = 0.5.dp)
                    }
                }
            }
        } else if (query.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text("未找到匹配结果", color = MapChinaColors.TextTertiary, fontSize = 14.sp, modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun locationTypeIcon(type: LocationType): ImageVector = when (type) {
    LocationType.PROVINCE -> Icons.Default.LocationOn
    LocationType.CITY -> Icons.Default.LocationOn
    LocationType.DISTRICT -> Icons.Default.LocationOn
    LocationType.ATTRACTION -> Icons.Default.LocationOn
}

private fun locationTypeColor(type: LocationType): Color = when (type) {
    LocationType.PROVINCE -> MapChinaColors.Primary
    LocationType.CITY -> MapChinaColors.FootprintShortVisit
    LocationType.DISTRICT -> MapChinaColors.FootprintPassBy
    LocationType.ATTRACTION -> MapChinaColors.FootprintDeep
}

private fun locationTypeLabel(item: LocationItem): String = when (item.type) {
    LocationType.PROVINCE -> "省份"
    LocationType.CITY -> "城市"
    LocationType.DISTRICT -> "县/区"
    LocationType.ATTRACTION -> item.subtitle
}

private fun formatDate(timestamp: Long): String {
    return try {
        val local = Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(TimeZone.currentSystemDefault())
        "${local.year}年${local.monthNumber}月${local.dayOfMonth}日"
    } catch (_: Exception) {
        ""
    }
}
