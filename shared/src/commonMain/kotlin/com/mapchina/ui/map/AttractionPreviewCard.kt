package com.mapchina.ui.map

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Attractions
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.ui.theme.Copy
import com.mapchina.ui.theme.MapChinaColors
import com.mapchina.ui.theme.MapChinaRadius
import com.mapchina.ui.theme.MapChinaTypography

@Composable
fun AttractionPreviewCard(
    attraction: AttractionUi,
    onViewDetail: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(MapChinaRadius.Large)
            .clickable(onClick = onViewDetail),
        shape = MapChinaRadius.Large,
        color = MapChinaColors.SurfaceElevated,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(MapChinaRadius.Medium)
                    .background(MapChinaColors.BorderSubtle),
                contentAlignment = Alignment.Center
            ) {
                if (attraction.imageUrl != null) {
                    AsyncImage(
                        model = attraction.imageUrl,
                        contentDescription = attraction.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Attractions,
                        contentDescription = null,
                        tint = MapChinaColors.TextTertiary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val levelBadge = when (attraction.level) {
                        "A5" -> "5A"
                        "A4" -> "4A"
                        else -> attraction.level
                    }
                    Text(
                        levelBadge,
                        color = if (attraction.level == "A5") MapChinaColors.AccentGold else MapChinaColors.AccentBlue,
                        style = MapChinaTypography.Caption,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(
                                if (attraction.level == "A5") MapChinaColors.AccentGold.copy(alpha = 0.2f) else MapChinaColors.AccentBlue.copy(alpha = 0.2f),
                                MapChinaRadius.Small
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        attraction.name,
                        style = MapChinaTypography.Title,
                        color = MapChinaColors.TextPrimary,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                val statusText = when (attraction.visitLevel) {
                    FootprintLevel.DEEP -> Copy.FOOTPRINT_DEEP
                    FootprintLevel.SHORT_VISIT -> Copy.FOOTPRINT_SHORT
                    FootprintLevel.PASS_BY -> Copy.FOOTPRINT_PASS
                    null -> Copy.FOOTPRINT_NONE
                }
                Text(
                    statusText,
                    color = if (attraction.visitLevel != null) MapChinaColors.FootprintDeep else MapChinaColors.TextTertiary,
                    style = MapChinaTypography.Caption,
                    modifier = Modifier
                        .clip(MapChinaRadius.Small)
                        .background(if (attraction.visitLevel != null) MapChinaColors.Primary.copy(alpha = 0.2f) else MapChinaColors.BorderSubtle)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    attraction.regionId,
                    color = MapChinaColors.TextTertiary,
                    style = MapChinaTypography.Caption
                )
            }

            IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = MapChinaColors.TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
