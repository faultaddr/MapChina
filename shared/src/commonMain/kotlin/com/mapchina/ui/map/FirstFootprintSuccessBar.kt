package com.mapchina.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.ui.theme.Copy
import com.mapchina.ui.theme.MapChinaCard
import com.mapchina.ui.theme.MapChinaColors

@Composable
fun FirstFootprintSuccessBar(
    celebration: FirstFootprintCelebration,
    modifier: Modifier = Modifier
) {
    val levelLabel = when (celebration.level) {
        FootprintLevel.PASS_BY -> Copy.FOOTPRINT_PASS
        FootprintLevel.SHORT_VISIT -> Copy.FOOTPRINT_SHORT
        FootprintLevel.DEEP -> Copy.FOOTPRINT_DEEP
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MapChinaColors.SurfaceElevated.copy(alpha = 0.96f),
        shadowElevation = 8.dp,
        border = MapChinaCard.border,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MapChinaColors.Primary,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = "第一处已点亮",
                    color = MapChinaColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${celebration.regionName} · $levelLabel",
                    color = MapChinaColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
