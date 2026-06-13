package com.mapchina.platform

expect class ExternalNavigator {
    fun navigateToAmap(latitude: Double, longitude: Double, name: String)
    fun openUrl(url: String)
}
