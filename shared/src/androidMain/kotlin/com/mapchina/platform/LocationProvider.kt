package com.mapchina.platform

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import java.util.concurrent.atomic.AtomicReference

actual class LocationProvider {
    var context: Context? = null

    private val cachedLocation = AtomicReference<Pair<Double, Double>?>()

    actual fun getCurrentLocation(): Pair<Double, Double>? {
        val ctx = context ?: return null
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null

        // Return cached result from a previous async fetch if available
        cachedLocation.get()?.let { return it }

        // Fast path: check last known location from all providers
        val lastKnown = resolveLastKnownLocation(lm)
        if (lastKnown != null) {
            cachedLocation.set(lastKnown)
            return lastKnown
        }

        // No cached or last-known location available.
        // Kick off an async request and return null for now.
        // On subsequent calls the cached result should be populated.
        requestLocationAsync(ctx, lm)

        return null
    }

    actual fun isAvailable(): Boolean {
        val ctx = context ?: return false
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false

        val hasGps = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val hasNetwork = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        return (hasGps || hasNetwork) && hasLocationPermission(ctx)
    }

    // -- Internal helpers --

    private fun resolveLastKnownLocation(lm: LocationManager): Pair<Double, Double>? {
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )

        var best: Location? = null
        for (provider in providers) {
            try {
                val loc = lm.getLastKnownLocation(provider) ?: continue
                if (loc.latitude == 0.0 && loc.longitude == 0.0) continue
                if (best == null || loc.accuracy < best.accuracy) {
                    best = loc
                }
            } catch (_: SecurityException) {
                // Provider not permitted; skip
            }
        }

        return best?.let { Pair(it.latitude, it.longitude) }
    }

    private fun requestLocationAsync(ctx: Context, lm: LocationManager) {
        if (!hasLocationPermission(ctx)) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestCurrentLocationApiS(ctx, lm)
        } else {
            requestSingleUpdateLegacy(ctx, lm)
        }
    }

    private fun requestCurrentLocationApiS(ctx: Context, lm: LocationManager) {
        val providers = listOf(
            LocationManager.FUSED_PROVIDER,
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER
        )

        for (provider in providers) {
            try {
                lm.getCurrentLocation(provider, null, ctx.mainExecutor) { location ->
                    location?.let { loc ->
                        if (loc.latitude != 0.0 || loc.longitude != 0.0) {
                            cachedLocation.set(Pair(loc.latitude, loc.longitude))
                        }
                    }
                }
                // Successfully submitted request for this provider
                return
            } catch (_: Exception) {
                // Provider unavailable; try the next one
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun requestSingleUpdateLegacy(ctx: Context, lm: LocationManager) {
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER
        )

        for (provider in providers) {
            if (!lm.isProviderEnabled(provider)) continue
            try {
                lm.requestSingleUpdate(provider, object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        if (location.latitude != 0.0 || location.longitude != 0.0) {
                            cachedLocation.set(Pair(location.latitude, location.longitude))
                        }
                        try {
                            lm.removeUpdates(this)
                        } catch (_: Exception) {
                            // Already unregistered
                        }
                    }

                    override fun onProviderDisabled(provider: String) {
                        try {
                            lm.removeUpdates(this)
                        } catch (_: Exception) { /* no-op */ }
                    }

                    override fun onProviderEnabled(provider: String) { /* no-op */ }

                    @Deprecated("Required for API <29 compat")
                    override fun onStatusChanged(provider: String, status: Int, extras: Bundle?) {
                        /* no-op */
                    }
                }, null)
                // Successfully submitted request
                return
            } catch (_: SecurityException) {
                // Provider not permitted; try the next one
            } catch (_: Exception) {
                // Provider unavailable; try the next one
            }
        }
    }

    private fun hasLocationPermission(ctx: Context): Boolean {
        val fine = ctx.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ctx.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED ||
                coarse == PackageManager.PERMISSION_GRANTED
    }
}
