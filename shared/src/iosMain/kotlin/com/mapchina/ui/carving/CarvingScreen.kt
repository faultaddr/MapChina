package com.mapchina.ui.carving

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mapchina.domain.model.Carving

@Composable
actual fun CarvingScreen(
    regionId: String,
    regionName: String,
    viewModel: CarvingViewModel,
    onBack: () -> Unit,
    attractionId: String?,
    attractionName: String?,
    carvingId: String?
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("雕刻功能暂不支持此平台")
    }
}

@Composable
actual fun CarvingListScreen(
    viewModel: CarvingViewModel,
    title: String,
    regionId: String?,
    attractionId: String?,
    showAll: Boolean,
    onCreateClick: () -> Unit,
    onEditClick: (Carving) -> Unit,
    onBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("雕刻功能暂不支持此平台")
    }
}
