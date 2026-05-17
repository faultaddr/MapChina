package com.mapchina.location

data class LocationResult(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float? = null,
    val timestamp: Long = 0L
)
