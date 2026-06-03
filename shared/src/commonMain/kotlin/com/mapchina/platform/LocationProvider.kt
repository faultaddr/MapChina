package com.mapchina.platform

expect class LocationProvider() {
    fun getCurrentLocation(): Pair<Double, Double>?
    fun isAvailable(): Boolean
}
