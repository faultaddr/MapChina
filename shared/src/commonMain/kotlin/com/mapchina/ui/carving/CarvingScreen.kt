package com.mapchina.ui.carving

import androidx.compose.runtime.Composable
import com.mapchina.domain.model.Carving

data class CarvingPlaceTarget(
    val regionId: String,
    val regionName: String,
    val attractionId: String? = null,
    val attractionName: String? = null
)

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
    onCreateClick: (CarvingPlaceTarget) -> Unit,
    onEditClick: (Carving) -> Unit = {},
    onBack: () -> Unit
)
