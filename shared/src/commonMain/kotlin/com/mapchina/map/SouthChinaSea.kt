package com.mapchina.map

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

object SouthChinaSea {
    // Standard nine-dash line: from east of Taiwan, down the east side of South China Sea,
    // across the south near Nansha, and back up the west side toward Hainan.
    val DASH_SEGMENTS = listOf(
        // 1. Taiwan NE offshore (~121E, 26N) down to Taiwan E coast (~122E, 23N)
        listOf(121.5 to 26.0, 122.0 to 24.0, 121.5 to 22.5),
        // 2. Continue south along Philippines west coast (~119E-117E)
        listOf(120.5 to 21.5, 119.5 to 19.5, 118.5 to 17.0),
        // 3. Down to Scarborough / Huangyan (~117E, 15N)
        listOf(117.5 to 15.5, 117.0 to 13.5, 116.5 to 11.5),
        // 4. Further south toward Nansha (~116E-115E, 9N-7N)
        listOf(116.0 to 10.0, 115.5 to 8.0, 114.5 to 6.5),
        // 5. Southernmost near Nansha (~113E-112E, 5N-6N)
        listOf(113.5 to 5.5, 112.0 to 5.0, 110.5 to 5.5),
        // 6. West side heading NW (~109E, 7N)
        listOf(109.5 to 6.5, 108.5 to 8.0, 108.0 to 10.5),
        // 7. Continue north up west side (~107E-108E, 12N-14N)
        listOf(108.0 to 12.5, 108.2 to 14.5, 108.5 to 16.0),
        // 8. Approaching Hainan SE coast (~109E, 17N-18N)
        listOf(108.8 to 17.0, 109.5 to 18.0, 110.0 to 18.5),
        // 9. Final segment reconnecting toward mainland (~110E-111E, 20N)
        listOf(110.5 to 19.0, 111.0 to 20.0, 111.5 to 21.0)
    )
}

fun DrawScope.drawSouthChinaSeaOnMap(
    projection: GeoProjection,
    zoomLevel: Float,
    strokeColor: Color,
    islandColor: Color
) {
    val lineWidth = if (zoomLevel < 6f) 1.2.dp.toPx() else 0.8.dp.toPx()
    val dashOn = 6.dp.toPx()
    val dashOff = 4.dp.toPx()

    for (segment in SouthChinaSea.DASH_SEGMENTS) {
        val path = Path().apply {
            val points = segment.map { projection.project(it.first, it.second) }
            if (points.isNotEmpty()) {
                moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) {
                    lineTo(points[i].x, points[i].y)
                }
            }
        }
        drawPath(
            path = path,
            color = strokeColor,
            style = Stroke(
                width = lineWidth,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashOn, dashOff))
            )
        )
    }
}
