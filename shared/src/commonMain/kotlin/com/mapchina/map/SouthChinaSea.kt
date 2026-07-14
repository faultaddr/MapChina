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
    // Standard ten-dash map data: nine South China Sea segments plus the segment east of Taiwan.
    val DASH_SEGMENTS = listOf(
        listOf(
            109.51763678906526 to 16.360467782665847,
            109.72339159230361 to 16.05587198177934,
            109.8780414893003 to 15.766823920473868,
            109.96506402665503 to 15.526031073258686,
            109.98526818797363 to 15.335615618596712,
        ),
        listOf(
            110.48331454715199 to 12.431407837351566,
            110.48240767589328 to 12.085792287259398,
            110.45136562643113 to 11.863835000833953,
            110.25652028695671 to 11.393616070326182,
        ),
        listOf(
            108.3388949586325 to 7.26656318024262,
            108.30727608084116 to 6.727803403200289,
            108.35631901989032 to 6.112648053307836,
        ),
        listOf(
            111.94112275674237 to 3.553559321848772,
            112.40151782268552 to 3.646409974664658,
            112.92104341055976 to 3.845112027649191,
        ),
        listOf(
            115.69079809651517 to 7.29016984601141,
            116.4095482213759 to 8.137962397303875,
        ),
        listOf(
            118.63503455703679 to 11.080904139262175,
            118.85587024190139 to 11.457907321145406,
            119.10128629647166 to 12.062751715859875,
            119.12181771101825 to 12.135585760471585,
        ),
        listOf(
            119.60808384544805 to 18.143451232827125,
            119.91075760817219 to 18.77194701315816,
            120.11918953031866 to 19.117669954512905,
        ),
        listOf(
            121.40591812413318 to 20.8001943859176,
            122.12216430894797 to 21.716094829922323,
        ),
        listOf(
            122.80328441666389 to 23.665545127578547,
            123.00481138309124 to 24.74934291726869,
        ),
        listOf(
            119.16836075308866 to 15.107448879733406,
            119.16981236678279 to 15.755038547478351,
            119.17823197590195 to 16.265658015720753,
        ),
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
