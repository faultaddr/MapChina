package com.mapchina.ui.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapchina.ui.theme.MapChinaColors

data class BreadcrumbItem(val id: String, val name: String)

@Composable
fun BreadcrumbNav(
    path: List<BreadcrumbItem>,
    onNavigateUp: () -> Unit,
    onNavigateTo: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MapChinaColors.SurfaceOverlay,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (path.size > 1) {
                IconButton(onClick = onNavigateUp) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回上级",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Collapse middle segments when path > 3
            val displayItems = if (path.size > 3) {
                listOf(path.first(), BreadcrumbItem("", "..."), path.last())
            } else {
                path
            }

            displayItems.forEachIndexed { displayIndex, item ->
                if (displayIndex > 0) {
                    Text(
                        " › ",
                        color = MapChinaColors.TextTertiary.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }

                val isEllipsis = item.name == "..."
                val realIndex = if (path.size > 3) {
                    when (displayIndex) {
                        0 -> 0
                        displayItems.lastIndex -> path.lastIndex
                        else -> -1
                    }
                } else {
                    displayIndex
                }
                val isLast = displayIndex == displayItems.lastIndex

                when {
                    isEllipsis -> {
                        Text(
                            "...",
                            fontSize = 14.sp,
                            color = MapChinaColors.TextTertiary.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                    isLast || item.id.isEmpty() -> {
                        Text(
                            item.name,
                            fontSize = 14.sp,
                            fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                            color = if (isLast) MapChinaColors.Primary else MapChinaColors.TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                    else -> {
                        Text(
                            item.name,
                            fontSize = 14.sp,
                            color = MapChinaColors.Primary.copy(alpha = 0.7f),
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier
                                .clickable { onNavigateTo(item.id) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
