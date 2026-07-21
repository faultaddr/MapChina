package com.mapchina.ui.community

import kotlin.time.Clock

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mapchina.data.remote.CommunityPostDto
import com.mapchina.ui.navigation.PostDetailScreen
import com.mapchina.ui.theme.UserAvatar
import com.mapchina.ui.theme.MapChinaColors
import kotlinx.coroutines.delay
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    viewModel: CommunityViewModel,
    onPostClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val feedUi by viewModel.feedUi.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        if (feedUi.posts.isEmpty()) {
            viewModel.loadFeed(refresh = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MapChinaColors.Background)
    ) {
        TopAppBar(
            title = { Text("游记", color = MapChinaColors.TextPrimary, fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MapChinaColors.Background)
        )

        if (feedUi.posts.isEmpty() && feedUi.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MapChinaColors.Primary)
            }
        } else if (feedUi.posts.isEmpty()) {
            CommunityEmptyState(
                onRefresh = { viewModel.loadFeed(refresh = true) },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
            ) {
                items(feedUi.posts, key = { it.id }) { post ->
                    PostCard(
                        post = post,
                        onLike = { viewModel.likePost(post.id) },
                        onClick = { onPostClick(post.id) }
                    )
                }
                if (feedUi.hasMore) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MapChinaColors.Primary, strokeWidth = 2.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityEmptyState(
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MapChinaColors.PrimaryLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AutoStories,
                    contentDescription = null,
                    tint = MapChinaColors.Primary,
                    modifier = Modifier.size(38.dp)
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "还没有旅行故事",
                color = MapChinaColors.TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "从景点详情写游记，或把你的足迹地图分享成第一条动态。",
                color = MapChinaColors.TextSecondary,
                fontSize = 14.sp,
                lineHeight = 21.sp
            )
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InspirationChip("足迹故事")
                InspirationChip("城市路线")
                InspirationChip("景点心得")
            }
            Spacer(Modifier.height(22.dp))
            Button(
                onClick = onRefresh,
                colors = ButtonDefaults.buttonColors(containerColor = MapChinaColors.Primary)
            ) {
                Text("刷新看看", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun InspirationChip(text: String) {
    Text(
        text,
        color = MapChinaColors.Primary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MapChinaColors.PrimaryLight)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

@Composable
private fun PostCard(
    post: CommunityPostDto,
    onLike: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MapChinaColors.SurfaceElevated),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(
                    name = post.nickname,
                    avatarUrl = post.avatarUrl,
                    size = 32.dp,
                    fontSize = 14.sp
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(post.nickname, color = MapChinaColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(formatTime(post.createdAt), color = MapChinaColors.TextTertiary, fontSize = 11.sp)
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(post.title, color = MapChinaColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)

            if (post.content.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(post.content, color = MapChinaColors.TextSecondary, fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }

            if (post.coverImage != null) {
                Spacer(Modifier.height(8.dp))
                AsyncImage(
                    model = post.coverImage,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }

            Spacer(Modifier.height(10.dp))

            var likeAnimTrigger by remember { mutableStateOf(false) }
            val likeScale by animateFloatAsState(
                targetValue = if (likeAnimTrigger) 1.3f else 1f,
                animationSpec = spring(dampingRatio = 0.3f, stiffness = Spring.StiffnessMedium),
                label = "likeScale"
            )
            LaunchedEffect(post.likedByMe) {
                if (post.likedByMe) {
                    likeAnimTrigger = true
                    delay(150)
                    likeAnimTrigger = false
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onLike, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (post.likedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "点赞",
                        tint = if (post.likedByMe) Color(0xFFE53935) else MapChinaColors.TextTertiary,
                        modifier = Modifier.size(18.dp).scale(likeScale)
                    )
                }
                Text(post.likeCount.toString(), color = MapChinaColors.TextTertiary, fontSize = 12.sp)
                Spacer(Modifier.width(16.dp))
                Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "评论", tint = MapChinaColors.TextTertiary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(post.commentCount.toString(), color = MapChinaColors.TextTertiary, fontSize = 12.sp)
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    return try {
        val local = Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(TimeZone.currentSystemDefault())
        val now = Clock.System.now().toEpochMilliseconds()
        val diff = now - timestamp
        when {
            diff < 60_000 -> "刚刚"
            diff < 3_600_000 -> "${diff / 60_000}分钟前"
            diff < 86_400_000 -> "${diff / 3_600_000}小时前"
            diff < 604_800_000 -> "${diff / 86_400_000}天前"
            else -> "${local.monthNumber}月${local.dayOfMonth}日"
        }
    } catch (_: Exception) {
        ""
    }
}
