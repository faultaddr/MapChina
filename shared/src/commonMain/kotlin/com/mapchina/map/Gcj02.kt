package com.mapchina.map

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

object Gcj02 {

    private const val A = 6378245.0
    private const val EE = 0.00669342162296594323
    private const val PI = 3.1415926535897932384626

    fun wgs84ToGcj02(lat: Double, lng: Double): Pair<Double, Double> {
        if (isOutOfChina(lat, lng)) return Pair(lat, lng)

        val dLat = transformLat(lng - 105.0, lat - 35.0)
        val dLng = transformLng(lng - 105.0, lat - 35.0)

        val radLat = lat / 180.0 * PI
        var magic = sin(radLat)
        magic = 1 - EE * magic * magic
        val sqrtMagic = sqrt(magic)

        val adjustedLat = (dLat * 180.0) / ((A * (1 - EE)) / (magic * sqrtMagic) * PI)
        val adjustedLng = (dLng * 180.0) / (A / sqrtMagic * cos(radLat) * PI)

        return Pair(lat + adjustedLat, lng + adjustedLng)
    }

    fun gcj02ToWgs84(lat: Double, lng: Double): Pair<Double, Double> {
        val (gcjLat, gcjLng) = wgs84ToGcj02(lat, lng)
        return Pair(lat * 2 - gcjLat, lng * 2 - gcjLng)
    }

    private fun isOutOfChina(lat: Double, lng: Double): Boolean {
        return lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271
    }

    private fun transformLat(x: Double, y: Double): Double {
        var ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * abs(x)
        ret += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
        ret += (20.0 * sin(y * PI) + 40.0 * sin(y / 3.0 * PI)) * 2.0 / 3.0
        ret += (160.0 * sin(y / 12.0 * PI) + 320.0 * sin(y * PI / 30.0)) * 2.0 / 3.0
        return ret
    }

    private fun transformLng(x: Double, y: Double): Double {
        var ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * abs(x)
        ret += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
        ret += (20.0 * sin(x * PI) + 40.0 * sin(x / 3.0 * PI)) * 2.0 / 3.0
        ret += (150.0 * sin(x / 12.0 * PI) + 300.0 * sin(x / 30.0 * PI)) * 2.0 / 3.0
        return ret
    }

    private fun cos(x: Double): Double = kotlin.math.cos(x)
}
