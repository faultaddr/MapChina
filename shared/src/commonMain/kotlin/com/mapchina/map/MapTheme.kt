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
        oceanColor = Color(0xFFE8F4F8),
        backgroundRes = null
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
