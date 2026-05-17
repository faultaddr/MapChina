package com.mapchina.data.model

import kotlinx.serialization.Serializable

@Serializable
sealed class ApiResponse<T> {
    abstract fun isSuccess(): Boolean

    @Serializable
    data class Success<T>(
        val data: T,
        val total: Long? = null,
        val hasMore: Boolean? = null
    ) : ApiResponse<T>() {
        override fun isSuccess() = true
    }

    @Serializable
    data class Error<T>(
        val code: String,
        val message: String
    ) : ApiResponse<T>() {
        override fun isSuccess() = false
    }
}
