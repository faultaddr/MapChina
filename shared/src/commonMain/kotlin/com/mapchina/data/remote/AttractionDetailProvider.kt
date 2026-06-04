package com.mapchina.data.remote

data class AttractionDetail(
    val imageUrls: List<String>,
    val rating: String?,
    val cost: String?,
    val openTime: String?,
    val tel: String?,
    val website: String?,
    val appointmentUrl: String? = null
)

expect class AttractionDetailProvider {
    fun getAttractionDetail(attractionId: String): AttractionDetail?
}
