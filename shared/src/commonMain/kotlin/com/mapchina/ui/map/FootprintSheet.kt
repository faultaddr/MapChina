package com.mapchina.ui.map

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapchina.domain.model.FootprintLevel
import com.mapchina.ui.theme.MapChinaColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FootprintSheet(
    region: RegionFootprintUi,
    onMarkFootprint: (String, FootprintLevel) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = region.name,
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            val statusText = when (region.footprintLevel) {
                FootprintLevel.DEEP -> "深度游览"
                FootprintLevel.SHORT_VISIT -> "短暂停留"
                FootprintLevel.PASS_BY -> "路过"
                null -> "未到访"
            }
            Text(
                text = "当前状态: $statusText",
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FootprintButton(
                    label = "路过",
                    color = MapChinaColors.FootprintPassBy,
                    onClick = { onMarkFootprint(region.regionId, FootprintLevel.PASS_BY) },
                    enabled = region.footprintLevel == null,
                    staggerIndex = 0
                )
                FootprintButton(
                    label = "短玩",
                    color = MapChinaColors.FootprintShortVisit,
                    onClick = { onMarkFootprint(region.regionId, FootprintLevel.SHORT_VISIT) },
                    enabled = region.footprintLevel?.let { it < FootprintLevel.SHORT_VISIT } ?: true,
                    staggerIndex = 1
                )
                FootprintButton(
                    label = "深度",
                    color = MapChinaColors.FootprintDeep,
                    onClick = { onMarkFootprint(region.regionId, FootprintLevel.DEEP) },
                    enabled = region.footprintLevel?.let { it < FootprintLevel.DEEP } ?: true,
                    staggerIndex = 2
                )
            }
        }
    }
}

@Composable
private fun FootprintButton(
    label: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    enabled: Boolean,
    staggerIndex: Int = 0,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = remember { Animatable(1f) }
    val entranceAlpha = remember { Animatable(0f) }
    val entranceY = remember { Animatable(20f) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay((staggerIndex * 80).toLong())
        launch {
            entranceAlpha.animateTo(1f, tween(200, easing = LinearOutSlowInEasing))
        }
        entranceY.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 150f))
    }

    LaunchedEffect(isPressed) {
        if (enabled) {
            if (isPressed) scale.animateTo(0.97f, spring(dampingRatio = 0.5f, stiffness = 500f))
            else scale.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 150f))
        }
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        modifier = modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
            alpha = entranceAlpha.value
            translationY = entranceY.value
        }
    ) {
        Text(label)
    }
}
