package com.mapchina.ui.theme

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TabPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

/**
 * Branded ink-tab indicator with a gradient sweep and animated width.
 * Drop-in replacement for TabRowDefaults.SecondaryIndicator.
 */
@Composable
fun InkTabIndicator(
    currentTabPosition: TabPosition,
    modifier: Modifier = Modifier
) {
    val animatedWidth by animateDpAsState(
        targetValue = currentTabPosition.width,
        animationSpec = tween(250),
        label = "tabIndicatorWidth"
    )
    val animatedOffset by animateDpAsState(
        targetValue = currentTabPosition.left,
        animationSpec = tween(250),
        label = "tabIndicatorOffset"
    )

    Box(
        modifier
            .fillMaxSize()
            .wrapContentSize(align = Alignment.BottomStart)
            .offset(x = animatedOffset)
            .width(animatedWidth)
            .height(3.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        MapChinaColors.Primary,
                        MapChinaColors.PrimaryVariant
                    )
                ),
                shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
            )
    )
}
