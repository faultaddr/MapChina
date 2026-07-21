package com.mapchina.performance

import platform.Foundation.NSLog
import platform.Foundation.NSProcessInfo

actual fun performanceTracingEnabled(): Boolean =
    NSProcessInfo.processInfo.environment["MAPCHINA_PERF_TRACE"] == "1"

actual fun performanceLog(message: String) {
    NSLog(message)
}
