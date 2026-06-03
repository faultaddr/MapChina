package com.mapchina.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val RicePaper = Color(0xFFF7F5F0)

@Composable
fun InkAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp
) {
    val avatarColor = nameToInkColor(name)
    // Deterministic random for ink splatter based on name
    val seed = name.hashCode().toLong()
    val rng = Random(seed)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(size.toPx() / 2, size.toPx() / 2)
            val radius = size.toPx() / 2

            // Rice paper base
            drawCircle(color = RicePaper, radius = radius, center = center)

            // Layer 1: broad ink wash — deep center fading outward
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        avatarColor.copy(alpha = 0.22f),
                        avatarColor.copy(alpha = 0.10f),
                        avatarColor.copy(alpha = 0.03f),
                        RicePaper
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )

            // Layer 2: ink splatter spots — 3-5 small random dots simulating splashed ink
            repeat(3 + (rng.nextInt(3))) {
                val angle = rng.nextDouble() * 2.0 * kotlin.math.PI
                val dist = rng.nextDouble() * radius * 0.65
                val spotCenter = Offset(
                    center.x + (dist * cos(angle)).toFloat(),
                    center.y + (dist * sin(angle)).toFloat()
                )
                val spotRadius = radius * (0.08f + rng.nextFloat() * 0.12f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            avatarColor.copy(alpha = 0.18f),
                            avatarColor.copy(alpha = 0.06f),
                            Color.Transparent
                        ),
                        center = spotCenter,
                        radius = spotRadius * 1.5f
                    ),
                    radius = spotRadius * 1.5f,
                    center = spotCenter
                )
            }

            // Layer 3: irregular brush stroke edge — wavy circle simulating brush edge
            val edgePath = Path()
            val segments = 24
            for (i in 0..segments) {
                val angle = (i.toFloat() / segments) * 2f * kotlin.math.PI.toFloat()
                val wobble = 1f + (rng.nextFloat() - 0.5f) * 0.08f
                val r = (radius - 1.5.dp.toPx()) * wobble
                val x = center.x + r * cos(angle.toDouble()).toFloat()
                val y = center.y + r * sin(angle.toDouble()).toFloat()
                if (i == 0) edgePath.moveTo(x, y)
                else edgePath.lineTo(x, y)
            }
            edgePath.close()
            drawPath(
                path = edgePath,
                color = avatarColor.copy(alpha = 0.20f),
                style = Stroke(width = 1.2.dp.toPx())
            )

            // Layer 4: dry brush accent — a short stroke across the circle
            val strokeAngle = rng.nextDouble() * kotlin.math.PI
            val strokeStart = Offset(
                center.x + (radius * 0.3f * cos(strokeAngle)).toFloat(),
                center.y + (radius * 0.3f * sin(strokeAngle)).toFloat()
            )
            val strokeEnd = Offset(
                center.x + (radius * 0.7f * cos(strokeAngle + kotlin.math.PI * 0.15)).toFloat(),
                center.y + (radius * 0.7f * sin(strokeAngle + kotlin.math.PI * 0.15)).toFloat()
            )
            drawLine(
                color = avatarColor.copy(alpha = 0.08f),
                start = strokeStart,
                end = strokeEnd,
                strokeWidth = 2.dp.toPx()
            )
        }
        Text(
            name.take(1),
            color = avatarColor,
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

private fun nameToInkColor(name: String): Color {
    val hash = name.hashCode()
    return when (hash % 5) {
        0 -> Color(0xFF1A1A1A) // 墨黑
        1 -> Color(0xFF2C4A3E) // 松烟
        2 -> Color(0xFF4A3728) // 赭墨
        3 -> Color(0xFF3D3D5C) // 青墨
        else -> Color(0xFF5C3A2E) // 朱砂
    }
}

@Composable
fun UserAvatar(
    name: String,
    avatarUrl: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp
) {
    if (avatarUrl != null) {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            coil3.compose.AsyncImage(
                model = avatarUrl,
                contentDescription = name,
                modifier = Modifier.size(size).clip(CircleShape),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                error = null,
                fallback = null
            )
            // Fallback overlay — shown when image fails to load
            // InkAvatar will be shown via the error handler below
        }
    } else {
        InkAvatar(name = name, modifier = modifier, size = size, fontSize = fontSize)
    }
}
