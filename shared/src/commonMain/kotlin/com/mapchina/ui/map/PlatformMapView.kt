package com.mapchina.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mapchina.map.MapController

@Composable
expect fun PlatformMapView(controller: MapController, modifier: Modifier = Modifier)
