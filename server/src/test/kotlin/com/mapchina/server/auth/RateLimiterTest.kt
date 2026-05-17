package com.mapchina.server.auth

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RateLimiterTest {

    @Test
    fun `allows requests_within_limit`() {
        val limiter = RateLimiter(maxRequests = 5, windowMs = 60_000)
        repeat(5) {
            assertTrue(limiter.tryAcquire("client1"))
        }
    }

    @Test
    fun `blocks_requests_exceeding_limit`() {
        val limiter = RateLimiter(maxRequests = 3, windowMs = 60_000)
        repeat(3) { limiter.tryAcquire("client1") }
        assertFalse(limiter.tryAcquire("client1"))
    }

    @Test
    fun `different_clients_have_independent_limits`() {
        val limiter = RateLimiter(maxRequests = 2, windowMs = 60_000)
        assertTrue(limiter.tryAcquire("client1"))
        assertTrue(limiter.tryAcquire("client1"))
        assertFalse(limiter.tryAcquire("client1"))

        assertTrue(limiter.tryAcquire("client2"))
    }

    @Test
    fun `cleanup_removes_stale_entries`() {
        val limiter = RateLimiter(maxRequests = 10, windowMs = 1)
        limiter.tryAcquire("old-client")

        Thread.sleep(5)
        limiter.cleanup(maxAgeMs = 2)

        assertTrue(limiter.tryAcquire("old-client"))
    }
}
