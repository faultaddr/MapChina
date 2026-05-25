package com.mapchina.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.mapchina.ui.animation.LocalAnimationScale

private val LightColorScheme = lightColorScheme(
    primary = MapChinaColors.Primary,
    onPrimary = MapChinaColors.OnPrimary,
    surface = MapChinaColors.Surface,
    onSurface = MapChinaColors.OnSurface,
    error = MapChinaColors.Error,
)

private val DarkColorScheme = darkColorScheme(
    primary = MapChinaColors.Primary,
    onPrimary = MapChinaColors.OnPrimary,
    surface = MapChinaColors.BlockViewBackground,
    onSurface = MapChinaColors.Surface,
    error = MapChinaColors.Error,
)

@Composable
fun MapChinaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    CompositionLocalProvider(LocalAnimationScale provides 1f) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
