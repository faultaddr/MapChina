package com.mapchina.ui.map

sealed class BottomPanel {
    data object None : BottomPanel()
    data class Region(val regionId: String) : BottomPanel()
    data class AttractionPreview(val attractionId: String) : BottomPanel()
}
