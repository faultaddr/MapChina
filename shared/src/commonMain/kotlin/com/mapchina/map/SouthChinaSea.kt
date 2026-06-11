package com.mapchina.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

object SouthChinaSea {
    val NINE_DASH_LINE = listOf(
        109.5 to 18.5, 111.0 to 16.5, 112.5 to 14.5, 114.0 to 12.0,
        115.5 to 10.5, 117.0 to 8.5, 117.5 to 7.0, 116.0 to 6.0,
        114.0 to 5.5, 112.0 to 5.0, 110.0 to 6.0, 109.0 to 8.0,
        108.5 to 11.0, 108.0 to 13.0, 108.5 to 15.0, 109.0 to 17.0,
        109.5 to 18.5
    )

    val ISLANDS = mapOf(
        "西沙" to (112.0 to 16.5),
        "南沙" to (114.0 to 10.0),
        "中沙" to (115.0 to 15.0),
        "东沙" to (117.0 to 20.5)
    )
}

fun DrawScope.drawSouthChinaSeaInset(
    zoomLevel: Float,
    strokeColor: Color,
    islandColor: Color
) {
    if (zoomLevel > 8f) return

    val insetWidth = 80.dp.toPx()
    val insetHeight = 100.dp.toPx()
    val margin = 12.dp.toPx()
    val insetLeft = size.width - insetWidth - margin
    val insetTop = size.height - insetHeight - margin

    val minLng = 107.0
    val maxLng = 118.0
    val minLat = 4.0
    val maxLat = 22.0
    val lngRange = maxLng - minLng
    val latRange = maxLat - minLat

    fun insetProject(lng: Double, lat: Double): Offset {
        val x = insetLeft + ((lng - minLng) / lngRange * insetWidth).toFloat()
        val y = insetTop + ((maxLat - lat) / latRange * insetHeight).toFloat()
        return Offset(x, y)
    }

    val borderPath = Path().apply {
        moveTo(insetLeft, insetTop)
        lineTo(insetLeft + insetWidth, insetTop)
        lineTo(insetLeft + insetWidth, insetTop + insetHeight)
        lineTo(insetLeft, insetTop + insetHeight)
        close()
    }
    drawPath(borderPath, color = Color.White.copy(alpha = 0.9f))
    drawPath(borderPath, color = strokeColor, style = Stroke(1.dp.toPx()))

    val dashPath = Path().apply {
        val points = SouthChinaSea.NINE_DASH_LINE.map { insetProject(it.first, it.second) }
        if (points.isNotEmpty()) {
            moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                lineTo(points[i].x, points[i].y)
            }
        }
    }
    drawPath(dashPath, color = strokeColor, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))

    for ((_, coords) in SouthChinaSea.ISLANDS) {
        val center = insetProject(coords.first, coords.second)
        drawCircle(islandColor, radius = 2.dp.toPx(), center = center)
    }
}
