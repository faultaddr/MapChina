package com.mapchina.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
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

internal data class SouthChinaSeaCubicCommand(
    val control1: Offset,
    val control2: Offset,
    val end: Offset,
)

internal data class SouthChinaSeaCurve(
    val start: Offset,
    val commands: List<SouthChinaSeaCubicCommand>,
)

internal fun buildSouthChinaSeaCurve(points: List<Offset>): SouthChinaSeaCurve? {
    if (points.size < 2) return null

    val commands = (0 until points.lastIndex).map { index ->
        val previous = points.getOrElse(index - 1) { points[index] }
        val start = points[index]
        val end = points[index + 1]
        val next = points.getOrElse(index + 2) { end }

        SouthChinaSeaCubicCommand(
            control1 = Offset(
                x = start.x + (end.x - previous.x) / 6f,
                y = start.y + (end.y - previous.y) / 6f,
            ),
            control2 = Offset(
                x = end.x - (next.x - start.x) / 6f,
                y = end.y - (next.y - start.y) / 6f,
            ),
            end = end,
        )
    }

    return SouthChinaSeaCurve(start = points.first(), commands = commands)
}

internal data class SouthChinaSeaStrokeStyle(
    val widthDp: Float,
    val alpha: Float,
)

internal fun southChinaSeaStrokeStyle(zoomLevel: Float): SouthChinaSeaStrokeStyle =
    if (zoomLevel < 6f) {
        SouthChinaSeaStrokeStyle(widthDp = 1.05f, alpha = 0.50f)
    } else {
        SouthChinaSeaStrokeStyle(widthDp = 0.85f, alpha = 0.42f)
    }

fun DrawScope.drawSouthChinaSeaOnMap(
    projection: GeoProjection,
    zoomLevel: Float,
    strokeColor: Color,
) {
    val strokeStyle = southChinaSeaStrokeStyle(zoomLevel)

    for (segment in SouthChinaSea.DASH_SEGMENTS) {
        val curve = buildSouthChinaSeaCurve(
            segment.map { projection.project(it.first, it.second) },
        ) ?: continue
        val path = Path().apply {
            moveTo(curve.start.x, curve.start.y)
            for (command in curve.commands) {
                cubicTo(
                    command.control1.x,
                    command.control1.y,
                    command.control2.x,
                    command.control2.y,
                    command.end.x,
                    command.end.y,
                )
            }
        }
        drawPath(
            path = path,
            color = strokeColor.copy(alpha = strokeStyle.alpha),
            style = Stroke(
                width = strokeStyle.widthDp.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}
