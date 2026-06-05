package com.mapchina.ui.navigation

import kotlinx.serialization.Serializable

@Serializable sealed class Screen

@Serializable data object MapScreen : Screen()
@Serializable data object AttractionsScreen : Screen()
@Serializable data object BadgeWallScreen : Screen()
@Serializable data class BadgeDetailScreen(val achievementId: String) : Screen()
@Serializable data object ProvinceConquestScreen : Screen()
@Serializable data class ProvinceDetailScreen(val provinceCode: String) : Screen()
@Serializable data object ProfileScreen : Screen()
@Serializable data object LoginScreen : Screen()
@Serializable data class RegionDetailScreen(val regionId: String) : Screen()
@Serializable data class AttractionDetailScreen(val attractionId: String) : Screen()
@Serializable data object AtlasScreen : Screen()
@Serializable data class AtlasDetailScreen(val atlasId: String) : Screen()
@Serializable data object JournalListScreen : Screen()
@Serializable data class JournalDetailScreen(val journalId: String) : Screen()
@Serializable data class JournalCreateScreen(val regionId: String? = null, val attractionId: String? = null) : Screen()
@Serializable data class CarvingScreen(val regionId: String, val regionName: String, val attractionId: String? = null, val attractionName: String? = null) : Screen()
@Serializable data class CarvingListScreen(val regionId: String? = null, val regionName: String? = null, val attractionId: String? = null, val showAll: Boolean = false) : Screen()
@Serializable data class CustomAttractionScreen(val regionId: String, val latitude: String = "0.0", val longitude: String = "0.0") : Screen()
@Serializable data object CommunityScreen : Screen()
@Serializable data class PostDetailScreen(val postId: String) : Screen()
