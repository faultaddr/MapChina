package com.mapchina.ui.carving

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mapchina.ui.carving.v2.CarvingBrushSpec
import com.mapchina.ui.carving.v2.CarvingStroke

/** Adds the terminal pointer sample, retaining the existing coalescing contract. */
fun finalizePlatformPoints(
    activePoints: List<PlatformPoint>,
    finalPoint: PlatformPoint
): List<PlatformPoint> = coalescePlatformPoints(activePoints + finalPoint)

@Composable
expect fun PlatformCarvingInputSurface(
    brush: CarvingBrushSpec,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onStrokeCommitted: (CarvingStroke) -> Unit
)
