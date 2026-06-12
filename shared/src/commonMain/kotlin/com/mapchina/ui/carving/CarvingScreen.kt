package com.mapchina.ui.carving

import androidx.compose.runtime.Composable
import com.mapchina.domain.model.Carving

@Composable
expect fun CarvingScreen(
    regionId: String,
    regionName: String,
    viewModel: CarvingViewModel,
    onBack: () -> Unit,
    attractionId: String? = null,
    attractionName: String? = null,
    carvingId: String? = null
)

@Composable
expect fun CarvingListScreen(
    viewModel: CarvingViewModel,
    title: String = "碑刻",
    regionId: String? = null,
    attractionId: String? = null,
    showAll: Boolean = false,
    onCreateClick: () -> Unit,
    onEditClick: (Carving) -> Unit = {},
    onBack: () -> Unit
)
