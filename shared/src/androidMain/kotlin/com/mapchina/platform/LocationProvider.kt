package com.mapchina.platform

import android.content.Context
import android.location.LocationManager
import android.os.Build

actual class LocationProvider {
    var context: Context? = null

    actual fun getCurrentLocation(): Pair<Double, Double>? {
        val ctx = context ?: return null
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null

        val location = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                lm.getCurrentLocation(LocationManager.FUSED_PROVIDER, null, ctx.mainExecutor) { }
                null // Fallback to last known
            } else null
        } catch (_: Exception) { null }

        val lastKnown = try {
            lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
        } catch (_: SecurityException) { null }

        val loc = lastKnown ?: return null
        if (loc.latitude == 0.0 && loc.longitude == 0.0) return null
        return Pair(loc.latitude, loc.longitude)
    }

    actual fun isAvailable(): Boolean = context != null
}
