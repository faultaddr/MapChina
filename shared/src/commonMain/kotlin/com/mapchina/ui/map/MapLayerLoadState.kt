package com.mapchina.ui.map

sealed interface MapLayerLoadState {
    data object Idle : MapLayerLoadState

    data class Loading(
        val regionId: String,
        val label: String
    ) : MapLayerLoadState

    data class Error(
        val regionId: String,
        val message: String
    ) : MapLayerLoadState
}
