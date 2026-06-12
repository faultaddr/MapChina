package com.mapchina.ui.community

import kotlin.time.Clock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mapchina.data.remote.CommentDto
import com.mapchina.data.remote.CommunityPostDto
import com.mapchina.ui.theme.UserAvatar
import com.mapchina.ui.theme.MapChinaColors
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: String,
    viewModel: CommunityViewModel,
    onBack: () -> Unit
) {
    val detailUi by viewModel.detailUi.collectAsState()
    val post = detailUi.post

    Column(Modifier.fillMaxSize().background(MapChinaColors.Background)) {
        TopAppBar(
            title = { Text("帖子详情", color = MapChinaColors.TextPrimary, fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = MapChinaColors.TextPrimary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MapChinaColors.Background)
        )

        if (post == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("加载中…", color = MapChinaColors.TextTertiary)
            }
            return
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item { PostContent(post = post, onLike = { viewModel.likePost(postId) }) }
            item {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MapChinaColors.BorderSubtle, thickness = 0.5.dp)
                Spacer(Modifier.height(8.dp))
                Text("评论 (${detailUi.comments.size})", color = MapChinaColors.TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(8.dp))
            }
            items(detailUi.comments, key = { it.id }) { comment ->
                CommentItem(comment)
            }
        }

        CommentInput(
            text = detailUi.commentText,
            onTextChange = { viewModel.updateCommentText(it) },
            onSend = { viewModel.submitComment(postId) }
        )
    }
}

@Composable
private fun PostContent(post: CommunityPostDto, onLike: () -> Unit) {
    Column(Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            UserAvatar(
                    name = post.nickname,
                    avatarUrl = post.avatarUrl,
                    size = 36.dp,
                    fontSize = 16.sp
                )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(post.nickname, color = MapChinaColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(formatTime(post.createdAt), color = MapChinaColors.TextTertiary, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(post.title, color = MapChinaColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)

        if (post.content.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(post.content, color = MapChinaColors.TextSecondary, fontSize = 15.sp, lineHeight = 22.sp)
        }

        if (post.coverImage != null) {
            Spacer(Modifier.height(12.dp))
            AsyncImage(
                model = post.coverImage,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(12.dp))
            )
        }

        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onLike) {
                Icon(
                    if (post.likedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "点赞",
                    tint = if (post.likedByMe) Color(0xFFE53935) else MapChinaColors.TextTertiary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(post.likeCount.toString(), color = MapChinaColors.TextTertiary, fontSize = 14.sp)
            Spacer(Modifier.width(20.dp))
            Text("${post.commentCount} 条评论", color = MapChinaColors.TextTertiary, fontSize = 14.sp)
        }
    }
}

@Composable
private fun CommentItem(comment: CommentDto) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            UserAvatar(
                    name = comment.nickname,
                    avatarUrl = comment.avatarUrl,
                    size = 28.dp,
                    fontSize = 12.sp
                )
            Spacer(Modifier.width(8.dp))
            Text(comment.nickname, color = MapChinaColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(8.dp))
            Text(formatTime(comment.createdAt), color = MapChinaColors.TextTertiary, fontSize = 11.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(comment.content, color = MapChinaColors.TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(start = 36.dp))
    }
    HorizontalDivider(color = MapChinaColors.BorderSubtle.copy(alpha = 0.3f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun CommentInput(text: String, onTextChange: (String) -> Unit, onSend: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MapChinaColors.SurfaceOverlay)
            .imePadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text("写评论…", color = MapChinaColors.TextTertiary, fontSize = 14.sp) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(20.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MapChinaColors.Primary,
                unfocusedBorderColor = MapChinaColors.BorderSubtle,
                cursorColor = MapChinaColors.Primary,
                focusedTextColor = MapChinaColors.TextPrimary,
                unfocusedTextColor = MapChinaColors.TextSecondary,
                focusedContainerColor = MapChinaColors.Background,
                unfocusedContainerColor = MapChinaColors.Background
            )
        )
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onSend, enabled = text.isNotBlank()) {
            Icon(Icons.Default.Send, contentDescription = "发送", tint = if (text.isNotBlank()) MapChinaColors.Primary else MapChinaColors.TextTertiary)
        }
    }
}

private fun formatTime(timestamp: Long): String {
    return try {
        val now = Clock.System.now().toEpochMilliseconds()
        val diff = now - timestamp
        when {
            diff < 60_000 -> "刚刚"
            diff < 3_600_000 -> "${diff / 60_000}分钟前"
            diff < 86_400_000 -> "${diff / 3_600_000}小时前"
            diff < 604_800_000 -> "${diff / 86_400_000}天前"
            else -> {
                val local = kotlinx.datetime.Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
                "${local.monthNumber}月${local.dayOfMonth}日"
            }
        }
    } catch (_: Exception) {
        ""
    }
}
