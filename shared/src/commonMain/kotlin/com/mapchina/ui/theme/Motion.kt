package com.mapchina.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

object MapChinaMotion {
    const val Instant = 100
    const val Quick = 200
    const val Normal = 300
    const val Slow = 500

    val SpringDefault = spring<Float>(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy)
    val SpringSnappy = spring<Float>(stiffness = Spring.StiffnessHigh, dampingRatio = Spring.DampingRatioLowBouncy)
}
