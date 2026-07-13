package com.mapchina.ui.carving

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mapchina.ui.carving.v2.CarvingBrushSpec
import com.mapchina.ui.carving.v2.CarvingStroke

@Composable
expect fun PlatformCarvingInputSurface(
    brush: CarvingBrushSpec,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onStrokeCommitted: (CarvingStroke) -> Unit
)
