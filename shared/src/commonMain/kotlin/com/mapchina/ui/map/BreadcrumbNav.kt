package com.mapchina.ui.map

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class BreadcrumbItem(val id: String, val name: String)

@Composable
fun BreadcrumbNav(
    path: List<BreadcrumbItem>,
    onNavigateUp: () -> Unit,
    onNavigateTo: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
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
        path.forEachIndexed { index, item ->
            if (index > 0) {
                Text(" > ", color = Color.Gray, fontSize = 14.sp)
            }
            TextButton(onClick = { onNavigateTo(item.id) }) {
                Text(item.name, fontSize = 14.sp)
            }
        }
    }
}
