package com.mapchina.map

import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.DrawableResource
import mapchina.shared.generated.resources.Res
import mapchina.shared.generated.resources.bg_ink_wash
import mapchina.shared.generated.resources.bg_vintage_map
import mapchina.shared.generated.resources.bg_rice_paper
import mapchina.shared.generated.resources.bg_starry_night
import mapchina.shared.generated.resources.bg_mountain_mist

enum class MapTheme(
    val displayName: String,
    val oceanColor: Color,
    val backgroundRes: DrawableResource?
) {
    DEFAULT(
        displayName = "经典",
        oceanColor = Color(0xFFF2F5F3),
        backgroundRes = Res.drawable.bg_rice_paper
    ),
    INK_WASH(
        displayName = "水墨",
        oceanColor = Color(0xFFF5F0E6),
        backgroundRes = Res.drawable.bg_ink_wash
    ),
    VINTAGE_MAP(
        displayName = "古舆图",
        oceanColor = Color(0xFFEBE1C8),
        backgroundRes = Res.drawable.bg_vintage_map
    ),
    RICE_PAPER(
        displayName = "宣纸",
        oceanColor = Color(0xFFF8F4EB),
        backgroundRes = Res.drawable.bg_rice_paper
    ),
    STARRY_NIGHT(
        displayName = "星夜",
        oceanColor = Color(0xFF0F1428),
        backgroundRes = Res.drawable.bg_starry_night
    ),
    MOUNTAIN_MIST(
        displayName = "山水",
        oceanColor = Color(0xFFE6EBF0),
        backgroundRes = Res.drawable.bg_mountain_mist
    );

    companion object {
        fun fromName(name: String?): MapTheme =
            entries.find { it.name == name } ?: DEFAULT
    }
}

data class MapVisualStyle(
    val canvasTopColor: Color,
    val canvasBottomColor: Color,
    val regionSurfaceColor: Color,
    val labelColor: Color,
    val chromeColor: Color,
    val chromeContentColor: Color,
    val textureAlpha: Float,
    val isDark: Boolean = false
)

val MapTheme.visualStyle: MapVisualStyle
    get() = when (this) {
        MapTheme.DEFAULT -> MapVisualStyle(
            canvasTopColor = Color(0xFFF0F4F2),
            canvasBottomColor = Color(0xFFF7F8F5),
            regionSurfaceColor = Color(0xFFFAFCF9),
            labelColor = Color(0xFF394844),
            chromeColor = Color(0xFFF9FBF8),
            chromeContentColor = Color(0xFF17201E),
            textureAlpha = 0.07f
        )
        MapTheme.INK_WASH -> MapVisualStyle(
            canvasTopColor = Color(0xFFF1EFE8),
            canvasBottomColor = Color(0xFFE9EEEA),
            regionSurfaceColor = Color(0xFFF3F1EA),
            labelColor = Color(0xFF34443F),
            chromeColor = Color(0xFFF8F6F0),
            chromeContentColor = Color(0xFF1F2D29),
            textureAlpha = 0.24f
        )
        MapTheme.VINTAGE_MAP -> MapVisualStyle(
            canvasTopColor = Color(0xFFECE2CC),
            canvasBottomColor = Color(0xFFF4ECD9),
            regionSurfaceColor = Color(0xFFF0E6D1),
            labelColor = Color(0xFF51402F),
            chromeColor = Color(0xFFF8F0DD),
            chromeContentColor = Color(0xFF392C22),
            textureAlpha = 0.28f
        )
        MapTheme.RICE_PAPER -> MapVisualStyle(
            canvasTopColor = Color(0xFFF8F5ED),
            canvasBottomColor = Color(0xFFF0F3EF),
            regionSurfaceColor = Color(0xFFF8F7F1),
            labelColor = Color(0xFF3F504B),
            chromeColor = Color(0xFFFCFAF5),
            chromeContentColor = Color(0xFF17201E),
            textureAlpha = 0.18f
        )
        MapTheme.STARRY_NIGHT -> MapVisualStyle(
            canvasTopColor = Color(0xFF0F1428),
            canvasBottomColor = Color(0xFF111B2C),
            regionSurfaceColor = Color(0xFF18243A),
            labelColor = Color(0xFFF4F7F9),
            chromeColor = Color(0xFF172033),
            chromeContentColor = Color(0xFFF4F7F9),
            textureAlpha = 0.48f,
            isDark = true
        )
        MapTheme.MOUNTAIN_MIST -> MapVisualStyle(
            canvasTopColor = Color(0xFFE5EBEC),
            canvasBottomColor = Color(0xFFF2F5F3),
            regionSurfaceColor = Color(0xFFF0F4F2),
            labelColor = Color(0xFF364945),
            chromeColor = Color(0xFFF7FAF8),
            chromeContentColor = Color(0xFF182522),
            textureAlpha = 0.22f
        )
    }
