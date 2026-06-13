package com.mapchina.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

@Composable
actual fun rememberHapticFeedback(): HapticFeedback {
    val view = LocalView.current
    return remember(view) { AndroidHapticFeedback(view) }
}
