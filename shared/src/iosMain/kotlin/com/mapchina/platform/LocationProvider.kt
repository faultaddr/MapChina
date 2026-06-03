package com.mapchina.platform

actual class LocationProvider actual constructor() {
    actual fun getCurrentLocation(): Pair<Double, Double>? = null
    actual fun isAvailable(): Boolean = false
}
