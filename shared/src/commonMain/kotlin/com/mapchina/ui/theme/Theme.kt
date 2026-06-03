package com.mapchina.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = MapChinaColors.Primary,
    onPrimary = MapChinaColors.TextPrimary,
    surface = MapChinaColors.SurfaceElevated,
    onSurface = MapChinaColors.TextSecondary,
    error = MapChinaColors.Error,
    background = MapChinaColors.Background,
    onBackground = MapChinaColors.TextSecondary,
)

@Composable
fun MapChinaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
