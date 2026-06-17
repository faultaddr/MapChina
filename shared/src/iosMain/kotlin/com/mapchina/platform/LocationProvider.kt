package com.mapchina.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLLocationAccuracyHundredMeters
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
actual class LocationProvider {
    private val manager = CLLocationManager()
    private var cachedLocation: Pair<Double, Double>? = null
    private var authorized = false

    private val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
        override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
            val loc = didUpdateLocations.lastOrNull() as? CLLocation ?: return
            loc.coordinate.useContents {
                if (latitude != 0.0 || longitude != 0.0) {
                    cachedLocation = Pair(latitude, longitude)
                }
            }
        }

        override fun locationManager(manager: CLLocationManager, didChangeAuthorizationStatus: CLAuthorizationStatus) {
            authorized = didChangeAuthorizationStatus == kCLAuthorizationStatusAuthorizedWhenInUse ||
                    didChangeAuthorizationStatus == kCLAuthorizationStatusAuthorizedAlways
            if (authorized) {
                manager.requestLocation()
            }
        }
    }

    init {
        manager.delegate = delegate
        manager.desiredAccuracy = kCLLocationAccuracyHundredMeters
        manager.requestWhenInUseAuthorization()
    }

    actual fun getCurrentLocation(): Pair<Double, Double>? {
        if (!authorized) return null
        if (cachedLocation == null) {
            manager.requestLocation()
        }
        return cachedLocation
    }

    actual fun isAvailable(): Boolean {
        return CLLocationManager.locationServicesEnabled()
    }
}
