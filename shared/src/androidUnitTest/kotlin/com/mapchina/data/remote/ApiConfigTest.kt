package com.mapchina.data.remote

import kotlin.test.Test
import kotlin.test.assertEquals

class ApiConfigTest {

    @Test
    fun defaultApiBaseUrl_usesAndroidEmulatorHost() {
        assertEquals("http://10.0.2.2:8080", defaultApiBaseUrl())
    }
}
