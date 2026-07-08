package com.mapchina.ui

import com.mapchina.ui.navigation.DiscoverScreen
import com.mapchina.ui.navigation.MapScreen
import com.mapchina.ui.navigation.ProfileScreen
import com.mapchina.ui.navigation.ShanheScreen
import org.junit.Assert.assertEquals
import org.junit.Test

class AppNavigationTest {
    @Test
    fun bottomNavItems_useProductResetTabs() {
        assertEquals(
            listOf("足迹", "发现", "山河", "我的"),
            bottomNavItems.map { it.label }
        )
    }

    @Test
    fun navKeys_existForNewMainTabs() {
        assertEquals(
            listOf(MapScreen, DiscoverScreen, ShanheScreen, ProfileScreen),
            bottomNavItems.map { it.screen }
        )
    }
}
