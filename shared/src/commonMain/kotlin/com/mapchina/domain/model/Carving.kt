package com.mapchina.domain.model

data class Carving(
    val id: String,
    val userId: String,
    val regionId: String,
    val regionName: String,
    val imagePath: String?,
    val strokeData: String?,
    val createdAt: Long,
    val attractionId: String? = null,
    val attractionName: String? = null
)
