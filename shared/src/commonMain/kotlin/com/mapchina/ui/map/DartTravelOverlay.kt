package com.mapchina.ui.map

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapchina.map.MapController
import com.mapchina.platform.HapticType
import com.mapchina.platform.LocalHapticFeedback
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val GlowJade = Color(0xFF0D7377)
private val GlowJadeBright = Color(0xFF2EC4B6)
private val GlowGold = Color(0xFFC8963E)
private val GlowGoldBright = Color(0xFFE8D48E)
private val GlowCinnabar = Color(0xFFC84530)
private val GlowCinnabarBright = Color(0xFFE87060)

private data class Particle(val angle: Float, val speed: Float, val size: Float, val color: Color)
private data class TrailDot(val screenPos: Offset, val alpha: Float, val radius: Float)

// Pre-converted px values to avoid .dp.toPx() per frame
private data class PxValues(
    val dotGlow: Float,
    val dotCore: Float,
    val beamW: Float,
    val ringMax: Float,
    val particleMaxDist: Float,
    val sparkleR: Float,
    val pinJumpR: Float,
    val pinLandR: Float,
    val pinTailLen: Float,
    val pinTailW: Float,
    val pinShadow: Float,
)

@Composable
fun DartTravelOverlay(
    cityDots: List<CityDot>,
    onCitySelected: (String) -> Unit,
    onDismiss: () -> Unit,
    mapController: MapController,
    modifier: Modifier = Modifier,
    key: Int = 0
) {
    if (cityDots.isEmpty()) return

    val density = LocalDensity.current.density
    val px = remember(density) {
        fun dp(v: Float) = v * density
        PxValues(
            dotGlow = dp(8f), dotCore = dp(1.5f),
            beamW = dp(20f),
            ringMax = dp(70f), particleMaxDist = dp(50f), sparkleR = dp(1.5f),
            pinJumpR = dp(5f), pinLandR = dp(8f),
            pinTailLen = dp(18f), pinTailW = dp(3.5f), pinShadow = dp(1.5f),
        )
    }

    var currentDotIndex by remember(key) { mutableIntStateOf(Random.nextInt(cityDots.size)) }
    var prevDotIndex by remember(key) { mutableIntStateOf(currentDotIndex) }
    var phase by remember(key) { mutableIntStateOf(0) }
    var jumpCount by remember(key) { mutableIntStateOf(0) }
    val totalJumps = 16

    val dartScale = remember(key) { Animatable(0.8f) }
    val nameAlpha = remember(key) { Animatable(0f) }
    val nameScale = remember(key) { Animatable(0.6f) }
    val overlayAlpha = remember(key) { Animatable(0f) }
    val ringExpand = remember(key) { Animatable(0f) }
    val particleAlpha = remember(key) { Animatable(1f) }
    val beamAlpha = remember(key) { Animatable(0f) }

    val particles = remember(key) { mutableStateListOf<Particle>() }
    val trailDots = remember(key) { mutableStateListOf<TrailDot>() }
    val visitedIndices = remember(key) { mutableStateListOf<Int>() }

    val finalCityId = remember(key) { cityDots.random().id }
    val finalDotIndex = remember(key) { cityDots.indexOfFirst { it.id == finalCityId }.coerceAtLeast(0) }

    // Pre-compute screen positions (once)
    val cityScreenPositions = remember(key) {
        cityDots.mapNotNull { dot ->
            mapController.toScreenLocation(dot.lat, dot.lng)?.let { Offset(it.first, it.second) }
        }
    }

    // Fade in
    LaunchedEffect(Unit) { overlayAlpha.animateTo(1f, animationSpec = tween(600)) }

    // Jumping
    LaunchedEffect(Unit) {
        while (jumpCount < totalJumps) {
            val progress = jumpCount.toFloat() / totalJumps
            delay((80L + (progress * progress * 300L)).toLong())
            prevDotIndex = currentDotIndex
            if (jumpCount == totalJumps - 1) {
                currentDotIndex = finalDotIndex
                phase = 1
            } else {
                currentDotIndex = Random.nextInt(cityDots.size)
            }
            cityScreenPositions.getOrNull(prevDotIndex)?.let {
                trailDots.add(TrailDot(it, 0.9f, 4f))
                visitedIndices.add(prevDotIndex)
            }
            val toRemove = mutableListOf<TrailDot>()
            trailDots.forEachIndexed { i, dot ->
                trailDots[i] = dot.copy(alpha = dot.alpha * 0.55f, radius = dot.radius * 0.8f)
                if (dot.alpha < 0.03f) toRemove.add(trailDots[i])
            }
            trailDots.removeAll(toRemove)
            if (visitedIndices.size > 6) visitedIndices.removeAt(0)
            jumpCount++
        }
    }

    val haptic = LocalHapticFeedback.current

    // Landing
    LaunchedEffect(phase) {
        if (phase == 1) {
            haptic.perform(HapticType.HEAVY)
            particles.clear()
            for (i in 0..23) {
                val angle = (i.toFloat() / 24) * 2 * Math.PI.toFloat() + Random.nextFloat() * 0.2f
                particles.add(Particle(
                    angle = angle,
                    speed = 0.6f + Random.nextFloat() * 1.3f,
                    size = 1.5f + Random.nextFloat() * 3f,
                    color = when (i % 3) { 0 -> GlowGoldBright; 1 -> GlowCinnabarBright; else -> GlowGold }
                ))
            }
            launch {
                dartScale.animateTo(2f, animationSpec = tween(60))
                dartScale.animateTo(1f, animationSpec = spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMedium))
            }
            launch { ringExpand.animateTo(1f, animationSpec = tween(700)) }
            launch {
                delay(80)
                beamAlpha.animateTo(0.7f, animationSpec = tween(120))
                delay(350)
                beamAlpha.animateTo(0f, animationSpec = tween(500))
            }
            launch { delay(200); particleAlpha.animateTo(0f, animationSpec = tween(700)) }
            launch {
                delay(400)
                nameScale.animateTo(1f, animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow))
                nameAlpha.animateTo(1f, animationSpec = tween(300))
            }
            launch { delay(1800); phase = 2; onCitySelected(cityDots[currentDotIndex].id) }
        }
    }

    val selectedCity = cityDots.getOrNull(currentDotIndex)
    val selectedPos = cityScreenPositions.getOrNull(currentDotIndex)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val a = overlayAlpha.value
            val h = size.height

            // Cinematic overlay: translucent, map still visible
            drawRect(color = Color(0xFF0A0E14).copy(alpha = a * 0.55f))
            // Top/bottom fade for depth
            drawRect(brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF0A0E14).copy(alpha = a * 0.45f), Color.Transparent, Color.Transparent, Color(0xFF0A0E14).copy(alpha = a * 0.35f)),
                startY = 0f, endY = h
            ))

            // City dots (2 layers: glow + core)
            val visitedSet = visitedIndices.toSet()
            for ((index, pos) in cityScreenPositions.withIndex()) {
                val isTarget = index == currentDotIndex && phase >= 1
                val isTrail = index in visitedSet
                drawCircle(color = when { isTarget -> GlowGold.copy(alpha = 0.25f); isTrail -> GlowJadeBright.copy(alpha = 0.15f); else -> GlowJade.copy(alpha = 0.06f) }, radius = px.dotGlow, center = pos)
                drawCircle(color = when { isTarget -> Color.White.copy(alpha = 0.9f); isTrail -> GlowJadeBright.copy(alpha = 0.5f); else -> Color.White.copy(alpha = 0.2f) }, radius = px.dotCore, center = pos)
            }

            // Comet trail
            if (phase == 0) {
                for (trail in trailDots) {
                    val r = trail.radius * density
                    drawCircle(color = GlowCinnabar.copy(alpha = trail.alpha * 0.5f), radius = r, center = trail.screenPos)
                    drawCircle(color = GlowCinnabarBright.copy(alpha = trail.alpha * 0.9f), radius = r * 0.4f, center = trail.screenPos)
                }
            }

            // Light beam
            if (phase >= 1 && selectedPos != null && beamAlpha.value > 0.01f) {
                val ba = beamAlpha.value
                val bw = px.beamW
                val beamPath = Path().apply {
                    moveTo(selectedPos.x - bw * 0.3f, selectedPos.y); lineTo(selectedPos.x - bw * 2f, 0f)
                    lineTo(selectedPos.x + bw * 2f, 0f); lineTo(selectedPos.x + bw * 0.3f, selectedPos.y); close()
                }
                drawPath(path = beamPath, color = GlowGoldBright.copy(alpha = ba * 0.12f))
                val inner = Path().apply {
                    moveTo(selectedPos.x - bw * 0.1f, selectedPos.y); lineTo(selectedPos.x - bw * 0.5f, 0f)
                    lineTo(selectedPos.x + bw * 0.5f, 0f); lineTo(selectedPos.x + bw * 0.1f, selectedPos.y); close()
                }
                drawPath(path = inner, color = Color.White.copy(alpha = ba * 0.18f))
            }

            // Dart + landing
            if (selectedCity != null && selectedPos != null) {
                val scale = dartScale.value
                if (phase == 0) {
                    drawDartPin(selectedPos, scale, true, px)
                } else {
                    drawLandingEffects(selectedPos, ringExpand.value, particleAlpha.value, particles.toList(), px, density)
                    drawDartPin(selectedPos, scale, false, px)
                }
            }
        }

        // City name card
        if (phase >= 1 && selectedCity != null && selectedPos != null) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Transparent,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 20.dp)
                    .graphicsLayer {
                        val s = nameScale.value
                        scaleX = s; scaleY = s; alpha = nameAlpha.value
                    }
            ) {
                Box(
                    modifier = Modifier
                        .background(Brush.horizontalGradient(colors = listOf(Color(0xDD1A1C1E), Color(0xBB2A2520))), shape = RoundedCornerShape(20.dp))
                        .padding(horizontal = 36.dp, vertical = 16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(selectedCity.name, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .background(Brush.horizontalGradient(colors = listOf(GlowGold, GlowCinnabar)), shape = RoundedCornerShape(4.dp))
                                .padding(horizontal = 12.dp, vertical = 3.dp)
                        ) {
                            Text("出发探索", color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp)
                        }
                    }
                }
            }
        }

        // Skip button
        if (phase < 2) {
            Text(
                "跳过",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 16.dp, end = 20.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        haptic.perform(HapticType.LIGHT)
                        if (phase == 0) {
                            phase = 1
                            currentDotIndex = finalDotIndex
                        }
                        if (phase == 1) {
                            phase = 2
                            onCitySelected(cityDots[currentDotIndex].id)
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

private fun DrawScope.drawDartPin(center: Offset, scale: Float, jumping: Boolean, px: PxValues) {
    val radius = (if (jumping) px.pinJumpR else px.pinLandR) * scale
    drawCircle(color = Color.Black.copy(alpha = 0.25f), radius = radius * 1.15f, center = center + Offset(0f, px.pinShadow))
    if (jumping) {
        val tailLen = px.pinTailLen * scale
        val tailW = px.pinTailW * scale
        val p = Path()
        p.moveTo(center.x, center.y - radius)
        p.lineTo(center.x - tailW, center.y - radius - tailLen)
        p.lineTo(center.x, center.y - radius - tailLen + tailW * 1.5f)
        p.lineTo(center.x + tailW, center.y - radius - tailLen)
        p.close()
        drawPath(path = p, color = GlowCinnabar)
        drawPath(path = p, color = GlowGold.copy(alpha = 0.25f))
        drawCircle(color = GlowCinnabar, radius = radius, center = center)
        drawCircle(color = GlowCinnabarBright.copy(alpha = 0.5f), radius = radius * 0.7f, center = center)
        drawCircle(color = Color.White, radius = radius * 0.35f, center = center)
    } else {
        drawCircle(color = GlowCinnabar.copy(alpha = 0.12f), radius = radius * 2f, center = center)
        drawCircle(color = GlowCinnabar, radius = radius, center = center)
        drawCircle(color = GlowCinnabarBright.copy(alpha = 0.4f), radius = radius * 0.8f, center = center)
        drawCircle(color = Color.White, radius = radius * 0.4f, center = center)
    }
}

private fun DrawScope.drawLandingEffects(
    center: Offset, ringProgress: Float, particleAlpha: Float,
    particles: List<Particle>, px: PxValues, density: Float
) {
    val maxR = px.ringMax
    for (i in 0..2) {
        val d = i * 0.12f
        val p = ((ringProgress - d) / (1f - d)).coerceIn(0f, 1f)
        if (p > 0f) {
            val rr = maxR * p
            val ra = (1f - p) * 0.4f
            val c = when (i) { 0 -> GlowCinnabar.copy(alpha = ra); 1 -> GlowGold.copy(alpha = ra); else -> GlowGoldBright.copy(alpha = ra * 0.6f) }
            drawCircle(color = c, radius = rr, center = center, style = Stroke(width = (2.5f - i * 0.5f) * density, cap = StrokeCap.Round))
        }
    }
    if (particleAlpha > 0.01f) {
        val maxDist = px.particleMaxDist
        for (pt in particles) {
            val dist = maxDist * ringProgress * pt.speed
            val pa = particleAlpha * (1f - ringProgress * 0.4f)
            drawCircle(color = pt.color.copy(alpha = pa * 0.7f), radius = pt.size * density * (1f - ringProgress * 0.3f), center = Offset(center.x + cos(pt.angle) * dist, center.y + sin(pt.angle) * dist))
        }
    }
    if (ringProgress > 0.25f) {
        val sa = (ringProgress - 0.25f) / 0.75f * particleAlpha
        for (i in 0..7) {
            val angle = (i.toFloat() / 8) * 2 * Math.PI.toFloat() + ringProgress * 0.4f
            val d = maxR * 0.5f * ringProgress
            drawCircle(color = GlowGoldBright.copy(alpha = sa * 0.6f), radius = px.sparkleR, center = Offset(center.x + cos(angle) * d, center.y + sin(angle) * d))
        }
    }
}
