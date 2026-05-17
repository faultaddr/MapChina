package com.mapchina.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    val phone: String,
    val nickname: String,
    val avatar: String? = null,
    val createdAt: Long
)
