package com.mapchina.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun createMapChinaHttpClient(engine: HttpClientEngine? = null): HttpClient {
    return if (engine == null) {
        HttpClient {
            configureMapChinaClient()
        }
    } else {
        HttpClient(engine) {
            configureMapChinaClient()
        }
    }
}

private fun HttpClientConfig<*>.configureMapChinaClient() {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
            }
        )
    }
}
