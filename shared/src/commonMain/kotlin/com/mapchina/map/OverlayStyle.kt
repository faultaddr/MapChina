package com.mapchina.map

data class OverlayStyle(
    val fillColor: Long,
    val strokeColor: Long,
    val strokeWidth: Float = 2f,
    val alpha: Float = 0.6f
)
