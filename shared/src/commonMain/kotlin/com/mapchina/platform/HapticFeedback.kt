package com.mapchina.platform

enum class HapticType {
    LIGHT,
    MEDIUM,
    HEAVY,
    SELECTION,
    SUCCESS,
    WARNING,
    ERROR
}

interface HapticFeedback {
    fun perform(type: HapticType)
}
