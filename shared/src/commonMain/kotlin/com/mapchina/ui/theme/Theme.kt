package com.mapchina.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = MapChinaColors.Primary,
    onPrimary = MapChinaColors.TextPrimary,
    surface = MapChinaColors.SurfaceElevated,
    onSurface = MapChinaColors.TextSecondary,
    error = MapChinaColors.Error,
    background = MapChinaColors.Background,
    onBackground = MapChinaColors.TextSecondary,
)

private val DarkColorScheme = darkColorScheme(
    primary = MapChinaColors.PrimaryVariant,
    onPrimary = Color.White,
    surface = Color(0xFF1C1C1E),
    onSurface = Color(0xFFE8E5DD),
    error = MapChinaColors.Error,
    background = Color(0xFF0F0F12),
    onBackground = Color(0xFFE8E5DD),
)

@Composable
fun MapChinaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        content = content
    )
}
