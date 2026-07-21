package com.mapchina.ui.map

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface RegionFocusState {
    data object Idle : RegionFocusState
    data class Animating(
        val regionId: String,
        val requestId: Long
    ) : RegionFocusState
    data class Focused(val regionId: String) : RegionFocusState
}

internal fun regionFocusDurationMillis(reducedMotion: Boolean): Long =
    if (reducedMotion) 120L else 650L

internal class RegionFocusCoordinator {
    private var sequence = 0L
    private val mutableState =
        MutableStateFlow<RegionFocusState>(RegionFocusState.Idle)
    val state: StateFlow<RegionFocusState> = mutableState.asStateFlow()

    fun begin(regionId: String): Long {
        sequence += 1L
        mutableState.value = RegionFocusState.Animating(regionId, sequence)
        return sequence
    }

    fun complete(requestId: Long): Boolean {
        val current = mutableState.value as? RegionFocusState.Animating
            ?: return false
        if (current.requestId != requestId) return false
        mutableState.value = RegionFocusState.Focused(current.regionId)
        return true
    }

    fun cancel() {
        mutableState.value = RegionFocusState.Idle
    }
}
