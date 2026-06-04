package com.mapchina.ui.journal

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapchina.domain.model.Journal
import com.mapchina.ui.theme.MapChinaColors
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun JournalListScreen(
    viewModel: JournalViewModel,
    onJournalClick: (String) -> Unit,
    onCreateClick: () -> Unit,
    onBack: () -> Unit
) {
    val ui by viewModel.listUi.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadJournals() }

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
                    "我的游记",
                    color = MapChinaColors.TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }

            if (ui.journals.isEmpty() && !ui.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(MapChinaColors.Primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Book,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MapChinaColors.Primary.copy(alpha = 0.5f)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("还没有游记", color = MapChinaColors.TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("点击右下角 + 开始记录旅途", color = MapChinaColors.TextTertiary, fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(ui.journals, key = { it.id }) { journal ->
                        JournalCard(
                            journal = journal,
                            onClick = { onJournalClick(journal.id) }
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
            containerColor = MapChinaColors.Primary,
            contentColor = MapChinaColors.TextPrimary,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "新建游记", modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun JournalCard(journal: Journal, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(MapChinaColors.CardBackground, Color(0xFF1E3040))
                )
            )
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            Text(
                journal.title,
                color = MapChinaColors.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (journal.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    journal.description,
                    color = MapChinaColors.TextSecondary,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = MapChinaColors.Primary.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(formatDate(journal.startTime), color = MapChinaColors.Primary.copy(alpha = 0.85f), fontSize = 12.sp)
                journal.regionId?.let {
                    Spacer(modifier = Modifier.width(14.dp))
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MapChinaColors.FootprintShortVisit.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("已关联区域", color = MapChinaColors.FootprintShortVisit.copy(alpha = 0.7f), fontSize = 12.sp)
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
