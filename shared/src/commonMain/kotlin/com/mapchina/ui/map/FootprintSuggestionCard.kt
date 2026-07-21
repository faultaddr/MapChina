package com.mapchina.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.domain.model.FootprintSuggestion
import com.mapchina.ui.theme.MapChinaCard
import com.mapchina.ui.theme.MapChinaColors
import com.mapchina.ui.theme.MapChinaRadius
import com.mapchina.ui.theme.MapChinaTypography

@Composable
fun FootprintSuggestionCard(
    suggestion: FootprintSuggestion,
    onConfirm: (FootprintLevel) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MapChinaColors.SurfaceElevated.copy(alpha = 0.96f),
        shadowElevation = 8.dp,
        tonalElevation = 2.dp,
        border = MapChinaCard.border,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "发现可能足迹",
                color = MapChinaColors.Primary,
                style = MapChinaTypography.Caption,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = suggestion.parentPath,
                color = MapChinaColors.TextPrimary,
                style = MapChinaTypography.Title,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${suggestion.evidenceLabel} · 可信度${suggestion.confidenceLabel}",
                color = MapChinaColors.TextSecondary,
                style = MapChinaTypography.Body,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "确认后会同时将上级地区标记为途经",
                color = MapChinaColors.TextTertiary,
                style = MapChinaTypography.Caption
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SuggestionActionButton(
                    label = "途经",
                    color = MapChinaColors.FootprintPassBy,
                    modifier = Modifier.weight(1f),
                    onClick = { onConfirm(FootprintLevel.PASS_BY) }
                )
                SuggestionActionButton(
                    label = "小驻",
                    color = MapChinaColors.FootprintShortVisit,
                    modifier = Modifier.weight(1f),
                    onClick = { onConfirm(FootprintLevel.SHORT_VISIT) }
                )
                SuggestionActionButton(
                    label = "深游",
                    color = MapChinaColors.FootprintDeep,
                    modifier = Modifier.weight(1f),
                    onClick = { onConfirm(FootprintLevel.DEEP) }
                )
                Text(
                    text = "忽略",
                    color = MapChinaColors.TextTertiary,
                    style = MapChinaTypography.Body,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(),
                            onClick = onDismiss
                        )
                        .padding(horizontal = 4.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun SuggestionActionButton(
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.12f), MapChinaRadius.Medium)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = color,
            style = MapChinaTypography.Body,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}
