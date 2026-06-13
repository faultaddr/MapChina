package com.mapchina.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Attractions
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.mapchina.platform.HapticType
import com.mapchina.platform.LocalHapticFeedback
import com.mapchina.platform.rememberHapticFeedback
import com.mapchina.ui.navigation.AppNavHost
import com.mapchina.ui.navigation.MapScreen
import com.mapchina.ui.navigation.AttractionsScreen
import com.mapchina.ui.navigation.CommunityScreen
import com.mapchina.ui.navigation.ProfileScreen
import com.mapchina.ui.navigation.Screen
import com.mapchina.ui.theme.MapChinaColors
import com.mapchina.ui.theme.MapChinaTheme
import com.mapchina.ui.theme.Copy
import com.mapchina.ui.theme.MapChinaMotion
import com.mapchina.ui.theme.MapChinaRadius
import com.mapchina.ui.theme.MapChinaTypography
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import mapchina.shared.generated.resources.Res
import mapchina.shared.generated.resources.splash

data class BottomNavItem(val screen: Screen, val label: String, val icon: ImageVector)

val bottomNavItems = listOf(
    BottomNavItem(MapScreen, Copy.TAB_MAP, Icons.Default.LocationOn),
    BottomNavItem(AttractionsScreen, Copy.TAB_ATTRACTION, Icons.Default.Attractions),
    BottomNavItem(CommunityScreen, Copy.TAB_COMMUNITY, Icons.Default.AutoStories),
    BottomNavItem(ProfileScreen, Copy.TAB_PROFILE, Icons.Default.Person),
)

internal val LocalScaffoldBottomPadding = compositionLocalOf { 0.dp }

internal val ShareModeState = mutableStateOf(false)

@Composable
fun MapChinaApp(onSplashReady: () -> Unit = {}) {
    val haptic = rememberHapticFeedback()
    CompositionLocalProvider(LocalHapticFeedback provides haptic) {
    MapChinaTheme {
        var showSplash by remember { mutableStateOf(true) }

        if (showSplash) {
            SplashScreen(
                onFinish = {
                    showSplash = false
                    onSplashReady()
                }
            )
        } else {
            val backStack = remember { mutableStateListOf<NavKey>(MapScreen) }
            val currentKey = backStack.last()

            val showBottomBar = bottomNavItems.any { item ->
                currentKey::class == item.screen::class
            } && !ShareModeState.value

            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    AnimatedVisibility(
                        visible = showBottomBar,
                        enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 3 },
                        exit = fadeOut(tween(150)) + slideOutVertically(tween(150)) { it / 3 }
                    ) {
                        MapChinaBottomBar(currentKey, backStack)
                    }
                }
            ) { innerPadding ->
                CompositionLocalProvider(
                    LocalScaffoldBottomPadding provides innerPadding.calculateBottomPadding()
                ) {
                    AppNavHost(backStack = backStack)
                }
            }
        }
    }
    }
}

@Composable
private fun SplashScreen(onFinish: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1500)
        onFinish()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(Res.drawable.splash),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun MapChinaBottomBar(
    currentKey: NavKey,
    backStack: SnapshotStateList<NavKey>
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        color = MapChinaColors.SurfaceElevated,
        shadowElevation = 12.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavItems.forEach { item ->
                val selected = currentKey::class == item.screen::class

                val tint by animateColorAsState(
                    targetValue = if (selected) MapChinaColors.Primary else MapChinaColors.TextTertiary,
                    animationSpec = tween(MapChinaMotion.Instant),
                    label = "tint"
                )

                Column(
                    modifier = Modifier
                        .clip(MapChinaRadius.Medium)
                        .clickable {
                            if (currentKey::class != item.screen::class) {
                                haptic.perform(HapticType.SELECTION)
                                backStack.clear()
                                backStack.add(item.screen)
                            }
                        }
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        tint = tint,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        item.label,
                        style = MapChinaTypography.Caption,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = tint
                    )
                    Spacer(Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .width(if (selected) 16.dp else 0.dp)
                            .height(2.5.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(MapChinaColors.Primary, MapChinaColors.PrimaryVariant)
                                ),
                                shape = RoundedCornerShape(1.dp)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun Spacer(modifier: Modifier) {
    androidx.compose.foundation.layout.Spacer(modifier = modifier)
}
