package com.mapchina.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object MapChinaColors {
    // Primary - 碧玉 (jade green, deeper and more saturated)
    val Primary = Color(0xFF0D7377)
    val PrimaryVariant = Color(0xFF14A3A8)
    val PrimaryLight = Color(0xFFE0F5F5)

    // Semantic backgrounds - 冷灰白 (clean modern)
    val Background = Color(0xFFF7F8FA)
    val SurfaceElevated = Color(0xFFFFFFFF)
    val SurfaceOverlay = Color(0xE6FFFFFF)

    // Borders & dividers
    val BorderSubtle = Color(0xFFE5E7EB)
    val BorderMedium = Color(0xFFD1D5DB)

    // Text - 墨色 (ink)
    val TextPrimary = Color(0xFF111827)
    val TextSecondary = Color(0xFF4B5563)
    val TextTertiary = Color(0xFF9CA3AF)

    // Accent - 丹砂 (cinnabar) & 金 (gold)
    val AccentGold = Color(0xFFD4973B)
    val AccentBlue = Color(0xFF3B82F6)
    val BadgeRed = Color(0xFFEF4444)

    // Footprint levels - 碧绿色系明度阶梯
    val FootprintDeep = Color(0xFF0D7377)
    val FootprintShortVisit = Color(0xFF2CA8A8)
    val FootprintPassBy = Color(0xFF7DD3D3)

    // Achievement rarity - 更精致的稀有度色彩
    val RarityCommon = Color(0xFF3B82F6)
    val RarityRare = Color(0xFF10B981)
    val RarityEpic = Color(0xFF8B5CF6)
    val RarityLegendary = Color(0xFFD4973B)

    // Status
    val Error = Color(0xFFEF4444)
    val Success = Color(0xFF10B981)
    val Warning = Color(0xFFD4973B)

    // Gradients - 只用于点缀（进度条、FAB等）
    val GradientPrimaryStart = Color(0xFF0D7377)
    val GradientPrimaryEnd = Color(0xFF14A3A8)
    val GradientGoldStart = Color(0xFFD4973B)
    val GradientGoldEnd = Color(0xFFE8B84E)
    val GradientWarmStart = Color(0xFFF7F8FA)
    val GradientWarmEnd = Color(0xFFE5E7EB)

    // Legacy compat
    val CardBackground = SurfaceElevated
    val CardBackgroundLight = Color(0xFFF3F4F6)
    val FootprintUnvisited = Color(0xFFE5E7EB)
    val Gold = Color(0xFFD4973B)

    // Convenience gradient brushes
    val PrimaryGradient = Brush.horizontalGradient(listOf(GradientPrimaryStart, GradientPrimaryEnd))
    val GoldGradient = Brush.horizontalGradient(listOf(GradientGoldStart, GradientGoldEnd))
    val WarmGradient = Brush.verticalGradient(listOf(GradientWarmStart, GradientWarmEnd))
    val CardHeroGradient = Brush.verticalGradient(listOf(
        Color(0xFF0D7377),
        Color(0xFF14A3A8),
    ))
}
