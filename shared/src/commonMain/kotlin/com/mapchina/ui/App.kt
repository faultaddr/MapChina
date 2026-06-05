package com.mapchina.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mapchina.ui.navigation.AppNavHost
import com.mapchina.ui.navigation.MapScreen
import com.mapchina.ui.navigation.AttractionsScreen
import com.mapchina.ui.navigation.ProfileScreen
import com.mapchina.ui.navigation.Screen
import com.mapchina.ui.theme.MapChinaColors
import com.mapchina.ui.theme.MapChinaTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import mapchina.shared.generated.resources.Res
import mapchina.shared.generated.resources.splash

data class BottomNavItem(val screen: Screen, val label: String, val icon: ImageVector)

val bottomNavItems = listOf(
    BottomNavItem(MapScreen, "足迹地图", Icons.Default.LocationOn),
    BottomNavItem(AttractionsScreen, "景点", Icons.Default.Attractions),
    BottomNavItem(ProfileScreen, "我的", Icons.Default.Person),
)

internal val LocalScaffoldBottomPadding = compositionLocalOf { 0.dp }

@Composable
fun MapChinaApp() {
    MapChinaTheme {
        var showSplash by remember { mutableStateOf(true) }

        if (showSplash) {
            SplashScreen(onFinish = { showSplash = false })
        } else {
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            val showBottomBar = bottomNavItems.any { item ->
                currentDestination?.hasRoute(item.screen::class) == true
            }

            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    if (showBottomBar) {
                        InkBottomBar(currentDestination, navController)
                    }
                }
            ) { innerPadding ->
                CompositionLocalProvider(
                    LocalScaffoldBottomPadding provides innerPadding.calculateBottomPadding()
                ) {
                    AppNavHost(navController = navController)
                }
            }
        }
    }
}

@Composable
private fun SplashScreen(onFinish: () -> Unit) {
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(800))
        delay(1800)
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

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center)
        ) {
            Text(
                "MapChina",
                color = MapChinaColors.Primary,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Text(
                "用脚步丈量中国",
                color = MapChinaColors.PrimaryVariant,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 4.sp
            )
        }
    }
}

private val InkGrey = Color(0xFF8E8E93)
private val RicePaper = Color(0xFFF8F6F1)

@Composable
private fun InkBottomBar(
    currentDestination: androidx.navigation.NavDestination?,
    navController: androidx.navigation.NavHostController
) {
    var selectedIndex by remember { mutableStateOf(0) }

    LaunchedEffect(currentDestination) {
        bottomNavItems.forEachIndexed { index, item ->
            if (currentDestination?.hierarchy?.any { it.hasRoute(item.screen::class) } == true) {
                selectedIndex = index
            }
        }
    }

    Column {
        // Top shadow cast by bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    )
                )
        )

        Surface(
            color = RicePaper,
            shadowElevation = 8.dp,
            tonalElevation = 2.dp
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Ink stain glow under selected tab
                Canvas(modifier = Modifier.matchParentSize()) {
                    val tabWidth = size.width / bottomNavItems.size
                    val selectedCenter = Offset(
                        tabWidth * selectedIndex + tabWidth / 2,
                        size.height * 0.4f
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MapChinaColors.Primary.copy(alpha = 0.1f),
                                MapChinaColors.Primary.copy(alpha = 0.03f),
                                Color.Transparent
                            ),
                            center = selectedCenter,
                            radius = tabWidth * 0.5f
                        ),
                        radius = tabWidth * 0.5f,
                        center = selectedCenter
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    bottomNavItems.forEachIndexed { index, item ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.hasRoute(item.screen::class)
                        } == true
                        InkTabItem(
                            item = item,
                            selected = selected,
                            onTap = {
                                navController.navigate(item.screen) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InkTabItem(
    item: BottomNavItem,
    selected: Boolean,
    onTap: () -> Unit
) {
    val tint by animateColorAsState(
        targetValue = if (selected) MapChinaColors.Primary else InkGrey,
        animationSpec = tween(220),
        label = "tint"
    )
    val pillAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(220),
        label = "pillAlpha"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.18f else 1f,
        animationSpec = spring(
            dampingRatio = 0.45f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "iconScale"
    )
    val iconOffsetY by animateFloatAsState(
        targetValue = if (selected) -4f else 0f,
        animationSpec = spring(
            dampingRatio = 0.55f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "iconOffsetY"
    )
    val indicatorWidth by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "indicatorWidth"
    )

    Column(
        modifier = Modifier
            .clickable(onClick = onTap)
            .padding(horizontal = 24.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Selection pill
            if (pillAlpha > 0.01f) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    MapChinaColors.Primary.copy(alpha = 0.14f * pillAlpha),
                                    MapChinaColors.Primary.copy(alpha = 0.04f * pillAlpha)
                                )
                            ),
                            shape = CircleShape
                        )
                )
            }
            Icon(
                item.icon,
                contentDescription = item.label,
                tint = tint,
                modifier = Modifier
                    .size(24.dp)
                    .offset(y = iconOffsetY.dp)
                    .graphicsLayer {
                        scaleX = iconScale; scaleY = iconScale
                    }
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            item.label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = tint,
            letterSpacing = if (selected) 0.5.sp else 0.sp
        )
        // Active indicator dot
        if (indicatorWidth > 0.01f) {
            Spacer(Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .height(3.dp)
                    .width((16 * indicatorWidth).dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MapChinaColors.Primary,
                                MapChinaColors.PrimaryVariant
                            )
                        ),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

@Composable
private fun Spacer(modifier: Modifier) {
    androidx.compose.foundation.layout.Spacer(modifier = modifier)
}
