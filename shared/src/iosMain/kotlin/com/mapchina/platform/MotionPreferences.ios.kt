package com.mapchina.platform

import androidx.compose.runtime.Composable
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled

@Composable
actual fun rememberReducedMotionEnabled(): Boolean =
    UIAccessibilityIsReduceMotionEnabled()
