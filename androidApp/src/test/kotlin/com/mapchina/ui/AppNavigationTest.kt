package com.mapchina.ui

import com.mapchina.ui.navigation.DiscoverScreen
import com.mapchina.ui.navigation.MapScreen
import com.mapchina.ui.navigation.ShanheScreen
import com.mapchina.ui.navigation.Screen
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
        val screens: List<Screen> = listOf(MapScreen, DiscoverScreen, ShanheScreen)
        assertEquals(3, screens.size)
    }
}
