package com.mapchina.location

actual class LocationProvider actual constructor() {
    private var callback: ((LocationResult) -> Unit)? = null
    private var lastLocation: LocationResult? = null

    actual fun startLocationUpdates(callback: (LocationResult) -> Unit) {
        this.callback = callback
        // TODO: Integrate with Android FusedLocationProviderClient
    }

    actual fun stopLocationUpdates() {
        callback = null
    }

    actual fun getLastKnownLocation(): LocationResult? = lastLocation
}
