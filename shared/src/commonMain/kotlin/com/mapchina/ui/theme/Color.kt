package com.mapchina.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object MapChinaColors {
    // Primary - 碧玉 (jade green, deeper and more saturated)
    val Primary = Color(0xFF0D7377)
    val PrimaryVariant = Color(0xFF14A3A8)
    val PrimaryLight = Color(0xFFE0F5F5)

    // Semantic backgrounds - 宣纸 (rice paper)
    val Background = Color(0xFFF8F6F1)
    val SurfaceElevated = Color(0xFFFFFFFF)
    val SurfaceOverlay = Color(0xE6FFFFFF)

    // Borders & dividers
    val BorderSubtle = Color(0xFFE8E5DD)
    val BorderMedium = Color(0xFFD5D1C8)

    // Text - 墨色 (ink)
    val TextPrimary = Color(0xFF1C1C1E)
    val TextSecondary = Color(0xFF4A4A4F)
    val TextTertiary = Color(0xFF8E8E93)

    // Accent - 丹砂 (cinnabar) & 金 (gold)
    val AccentGold = Color(0xFFC8963E)
    val AccentBlue = Color(0xFF2E7BD6)
    val BadgeRed = Color(0xFFD94040)

    // Footprint levels - 碧绿色系明度阶梯
    val FootprintDeep = Color(0xFF1A7A70)
    val FootprintShortVisit = Color(0xFF3EA396)
    val FootprintPassBy = Color(0xFF80C4BA)

    // Achievement rarity - 更精致的稀有度色彩
    val RarityCommon = Color(0xFF2E7BD6)
    val RarityRare = Color(0xFF1A9E5C)
    val RarityEpic = Color(0xFF8B4DB8)
    val RarityLegendary = Color(0xFFC8963E)

    // Status
    val Error = Color(0xFFDC3545)
    val Success = Color(0xFF1A9E5C)
    val Warning = Color(0xFFC8A040)

    // Gradients
    val GradientPrimaryStart = Color(0xFF0D7377)
    val GradientPrimaryEnd = Color(0xFF14A3A8)
    val GradientGoldStart = Color(0xFFC8963E)
    val GradientGoldEnd = Color(0xFFE8B84E)
    val GradientWarmStart = Color(0xFFF8F6F1)
    val GradientWarmEnd = Color(0xFFEDE8DD)

    // Legacy compat
    val CardBackground = SurfaceElevated
    val CardBackgroundLight = Color(0xFFF0EDE5)
    val FootprintUnvisited = Color(0xFFE8E5DD)
    val Gold = Color(0xFFC8963E)

    // Convenience gradient brushes
    val PrimaryGradient = Brush.horizontalGradient(listOf(GradientPrimaryStart, GradientPrimaryEnd))
    val GoldGradient = Brush.horizontalGradient(listOf(GradientGoldStart, GradientGoldEnd))
    val WarmGradient = Brush.verticalGradient(listOf(GradientWarmStart, GradientWarmEnd))
    val CardHeroGradient = Brush.verticalGradient(listOf(
        Color(0xFF0D7377),
        Color(0xFF14A3A8),
    ))
}
