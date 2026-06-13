package com.mapchina.platform

import androidx.compose.runtime.compositionLocalOf

val LocalHapticFeedback = compositionLocalOf<HapticFeedback> {
    object : HapticFeedback {
        override fun perform(type: HapticType) {}
    }
}
