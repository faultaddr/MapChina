package com.mapchina.performance

import android.util.Log

private const val PerformanceLogTag = "MapChinaPerf"

actual fun performanceTracingEnabled(): Boolean =
    System.getenv("MAPCHINA_PERF_TRACE") == "1"

actual fun performanceLog(message: String) {
    Log.d(PerformanceLogTag, message)
}
