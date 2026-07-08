package com.mapchina.ui.shanhe

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.mapchina.ui.theme.Copy
import com.mapchina.ui.theme.MapChinaColors
import com.mapchina.ui.theme.MapChinaTypography

@Composable
fun ShanheScreen(
    viewModel: ShanheViewModel,
    onNavigate: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MapChinaColors.Background)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Text(
            text = Copy.SHANHE_TITLE,
            color = MapChinaColors.TextPrimary,
            style = MapChinaTypography.Display
        )
        Text(
            text = Copy.SHANHE_SUBTITLE,
            color = MapChinaColors.TextSecondary,
            style = MapChinaTypography.Body,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
