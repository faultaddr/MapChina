package com.mapchina.map

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapThemeVisualStyleTest {
    @Test
    fun paperThemesKeepTextureBehindTheMap() {
        assertTrue(MapTheme.RICE_PAPER.visualStyle.textureAlpha < 0.25f)
        assertTrue(MapTheme.INK_WASH.visualStyle.textureAlpha < 0.3f)
    }

    @Test
    fun darkThemeUsesLightLabelsAndChrome() {
        val style = MapTheme.STARRY_NIGHT.visualStyle

        assertTrue(style.isDark)
        assertEquals(Color(0xFFF4F7F9), style.labelColor)
        assertEquals(Color(0xFF172033), style.chromeColor)
    }
}
