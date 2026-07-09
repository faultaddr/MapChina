package com.mapchina.ui.shanhe

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ShanheUi(
    val levelTitle: String = "山河初识",
    val scoreLabel: String = "0 山河值",
    val targetTitle: String = "今日目标",
    val targetBody: String = "确认 1 条可能足迹，点亮下一块版图"
)

class ShanheViewModel {
    private val _ui = MutableStateFlow(ShanheUi())
    val ui: StateFlow<ShanheUi> = _ui.asStateFlow()
}
