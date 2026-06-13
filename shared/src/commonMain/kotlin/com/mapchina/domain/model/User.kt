package com.mapchina.domain.model

import kotlin.time.Instant

data class User(
    val id: String,
    val phone: String,
    val nickname: String,
    val avatar: String? = null,
    val createdAt: Instant
)
