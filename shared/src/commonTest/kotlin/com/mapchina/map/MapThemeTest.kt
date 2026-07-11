package com.mapchina.map

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MapThemeTest {

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
