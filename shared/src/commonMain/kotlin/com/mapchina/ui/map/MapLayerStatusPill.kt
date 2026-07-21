package com.mapchina.ui.map

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapchina.ui.theme.MapChinaColors

@Composable
fun MapLayerStatusPill(
    state: MapLayerLoadState,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (state) {
        MapLayerLoadState.Idle -> Unit
        is MapLayerLoadState.Loading -> Surface(
            modifier = modifier.semantics {
                contentDescription = "地图层级加载中"
            },
            shape = RoundedCornerShape(18.dp),
            color = MapChinaColors.SurfaceOverlay.copy(alpha = 0.92f),
            shadowElevation = 4.dp
        ) {
            Text(
                text = state.label,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                color = MapChinaColors.TextPrimary,
                fontSize = 13.sp
            )
        }

        is MapLayerLoadState.Error -> Surface(
            modifier = modifier.semantics {
                contentDescription = "地图层级加载失败"
            },
            shape = RoundedCornerShape(18.dp),
            color = MapChinaColors.SurfaceOverlay.copy(alpha = 0.96f),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(state.message, color = MapChinaColors.TextPrimary, fontSize = 13.sp)
                TextButton(onClick = onRetry) { Text("重试") }
                TextButton(onClick = onDismiss) { Text("关闭") }
            }
        }
    }
}
