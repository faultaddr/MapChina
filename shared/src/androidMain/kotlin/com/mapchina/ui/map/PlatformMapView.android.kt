package com.mapchina.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.LatLng
import com.mapchina.map.MapController

@Composable
actual fun PlatformMapView(controller: MapController, modifier: Modifier) {
    val androidController = remember { controller }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            MapView(context).apply {
                onCreate(null)
                val aMap = map
                androidController.bindMap(aMap, context)
            }
        },
        update = { mapView ->
            mapView.onResume()
        },
        onRelease = { mapView ->
            mapView.onPause()
            mapView.onDestroy()
            androidController.unbindMap()
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            androidController.dispose()
        }
    }
}
