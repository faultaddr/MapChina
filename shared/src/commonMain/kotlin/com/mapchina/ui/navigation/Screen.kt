package com.mapchina.ui.navigation

import kotlinx.serialization.Serializable

@Serializable sealed class Screen

@Serializable data object MapScreen : Screen()
@Serializable data object AttractionsScreen : Screen()
@Serializable data object StatsScreen : Screen()
@Serializable data object AchievementScreen : Screen()
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
