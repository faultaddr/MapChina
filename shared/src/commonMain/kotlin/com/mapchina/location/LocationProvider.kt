package com.mapchina.location

expect class LocationProvider() {
    fun startLocationUpdates(callback: (LocationResult) -> Unit)
    fun stopLocationUpdates()
    fun getLastKnownLocation(): LocationResult?
}
