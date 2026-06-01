package com.mapchina.domain.model

data class AtlasDefinition(
    val id: String,
    val name: String,
    val description: String,
    val coverImage: String
)

data class AtlasItem(
    val atlasId: String,
    val attractionId: String,
    val itemName: String,
    val province: String,
    val city: String
)

data class AtlasProgress(
    val atlasId: String,
    val atlasName: String,
    val atlasDescription: String,
    val totalItems: Int,
    val visitedItems: Int,
    val completionPercent: Int
) {
    val isCompleted: Boolean get() = totalItems > 0 && visitedItems >= totalItems
}
