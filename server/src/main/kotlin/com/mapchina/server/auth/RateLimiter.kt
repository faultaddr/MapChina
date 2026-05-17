package com.mapchina.server.auth

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

data class RateLimitEntry(
    val count: AtomicLong,
    val windowStart: AtomicLong
)

class RateLimiter(
    private val maxRequests: Int,
    private val windowMs: Long
) {
    private val entries = ConcurrentHashMap<String, RateLimitEntry>()

    fun tryAcquire(key: String): Boolean {
        val entry = entries.computeIfAbsent(key) {
            RateLimitEntry(AtomicLong(0), AtomicLong(System.currentTimeMillis()))
        }
        val now = System.currentTimeMillis()
        val windowStart = entry.windowStart.get()

        if (now - windowStart >= windowMs) {
            entry.windowStart.set(now)
            entry.count.set(0)
        }

        return entry.count.incrementAndGet() <= maxRequests
    }

    fun cleanup(maxAgeMs: Long = windowMs * 2) {
        val now = System.currentTimeMillis()
        entries.entries.removeIf { now - it.value.windowStart.get() > maxAgeMs }
    }
}

fun Application.installRateLimiting() {
    val authLimiter = RateLimiter(maxRequests = 10, windowMs = 60_000)
    val apiLimiter = RateLimiter(maxRequests = 100, windowMs = 60_000)

    intercept(ApplicationCallPipeline.Plugins) {
        val path = call.request.path()

        val limiter = when {
            path.startsWith("/auth") -> authLimiter
            else -> apiLimiter
        }

        val clientKey = call.request.origin.remoteHost

        if (!limiter.tryAcquire(clientKey)) {
            call.respondText(
                """{"code":"RATE_LIMITED","message":"请求过于频繁，请稍后再试"}""",
                status = HttpStatusCode.TooManyRequests
            )
            finish()
        }
    }

    CoroutineScope(Dispatchers.IO).launch {
        while (true) {
            delay(300_000)
            authLimiter.cleanup()
            apiLimiter.cleanup()
        }
    }
}
