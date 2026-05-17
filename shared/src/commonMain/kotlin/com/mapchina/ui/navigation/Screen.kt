package com.mapchina.ui.navigation

sealed class Screen(val route: String) {
    data object Map : Screen("map")
    data object Attractions : Screen("attractions")
    data object Stats : Screen("stats")
    data object Profile : Screen("profile")
    data object Login : Screen("login")
    data object RegionDetail : Screen("region/{regionId}") {
        fun createRoute(regionId: String) = "region/$regionId"
    }
    data object AttractionDetail : Screen("attraction/{attractionId}") {
        fun createRoute(attractionId: String) = "attraction/$attractionId"
    }
}
