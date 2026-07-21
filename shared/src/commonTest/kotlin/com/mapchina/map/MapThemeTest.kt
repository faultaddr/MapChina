package com.mapchina.map

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MapThemeTest {

    @Test
    fun defaultTheme_usesMorningJadePalette() {
        val style = MapTheme.DEFAULT.visualStyle
        assertEquals(Color(0xFFEAF6F0), MapTheme.DEFAULT.oceanColor)
        assertEquals(Color(0xFFF8FBF6), style.canvasTopColor)
        assertEquals(Color(0xFFDFF0EC), style.canvasBottomColor)
        assertEquals(Color(0xFFF8FCF8), style.regionSurfaceColor)
        assertEquals(Color(0xFF294943), style.labelColor)
        assertEquals(Color(0xFFFCFFFC), style.chromeColor)
        assertEquals(Color(0xFF173D36), style.chromeContentColor)
        assertEquals(0.05f, style.textureAlpha)
    }

    @Test
    fun defaultTheme_usesSubtleExistingPaperTexture() {
        assertNotNull(MapTheme.DEFAULT.backgroundRes)
        assertTrue(MapTheme.DEFAULT.visualStyle.textureAlpha in 0.04f..0.10f)
    }

    @Test
    fun namedThemes_keepTheirExistingIdentity() {
        assertTrue(MapTheme.STARRY_NIGHT.visualStyle.isDark)
        assertTrue(MapTheme.VINTAGE_MAP.visualStyle.textureAlpha >= 0.20f)
    }
}
