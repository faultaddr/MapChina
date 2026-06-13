package com.mapchina.map

import kotlin.math.abs
import kotlin.math.sqrt

object DouglasPeucker {

    fun simplify(points: List<Pair<Double, Double>>, epsilon: Double): List<Pair<Double, Double>> {
        if (points.size <= 2) return points.toList()
        if (epsilon <= 0.0) return points.toList()

        var maxDist = 0.0
        var maxIndex = 0

        val first = points.first()
        val last = points.last()

        for (i in 1 until points.size - 1) {
            val dist = perpendicularDistance(points[i], first, last)
            if (dist > maxDist) {
                maxDist = dist
                maxIndex = i
            }
        }

        return if (maxDist > epsilon) {
            val left = simplify(points.subList(0, maxIndex + 1), epsilon)
            val right = simplify(points.subList(maxIndex, points.size), epsilon)
            left.dropLast(1) + right
        } else {
            listOf(first, last)
        }
    }

    private fun perpendicularDistance(
        point: Pair<Double, Double>,
        lineStart: Pair<Double, Double>,
        lineEnd: Pair<Double, Double>
    ): Double {
        val dx = lineEnd.first - lineStart.first
        val dy = lineEnd.second - lineStart.second
        if (dx == 0.0 && dy == 0.0) {
            val ddx = point.first - lineStart.first
            val ddy = point.second - lineStart.second
            return sqrt(ddx * ddx + ddy * ddy)
        }
        val numerator = abs(dy * point.first - dx * point.second + lineEnd.first * lineStart.second - lineEnd.second * lineStart.first)
        val denominator = sqrt(dx * dx + dy * dy)
        return numerator / denominator
    }
}
