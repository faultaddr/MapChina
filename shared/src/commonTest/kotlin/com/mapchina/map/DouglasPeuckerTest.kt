package com.mapchina.map

import kotlin.test.Test
import kotlin.test.assertEquals

class DouglasPeuckerTest {

    @Test
    fun simplify_preserves_endpoints() {
        val points = listOf(0.0 to 0.0, 1.0 to 1.0, 2.0 to 0.0, 3.0 to 1.0, 4.0 to 0.0)
        val result = DouglasPeucker.simplify(points, 0.5)
        assertEquals(points.first(), result.first())
        assertEquals(points.last(), result.last())
    }

    @Test
    fun simplify_with_zero_epsilon_returns_original() {
        val points = listOf(0.0 to 0.0, 1.0 to 1.0, 2.0 to 0.0)
        val result = DouglasPeucker.simplify(points, 0.0)
        assertEquals(points.size, result.size)
    }

    @Test
    fun simplify_removes_collinear_points() {
        val points = listOf(0.0 to 0.0, 1.0 to 1.0, 2.0 to 2.0, 3.0 to 3.0)
        val result = DouglasPeucker.simplify(points, 0.1)
        assertEquals(2, result.size)
    }

    @Test
    fun simplify_keeps_points_far_from_line() {
        val points = listOf(0.0 to 0.0, 1.0 to 5.0, 2.0 to 0.0)
        val result = DouglasPeucker.simplify(points, 1.0)
        assertEquals(3, result.size)
    }

    @Test
    fun simplify_handles_two_points() {
        val points = listOf(0.0 to 0.0, 1.0 to 1.0)
        val result = DouglasPeucker.simplify(points, 0.5)
        assertEquals(2, result.size)
    }

    @Test
    fun simplify_handles_single_point() {
        val points = listOf(0.0 to 0.0)
        val result = DouglasPeucker.simplify(points, 0.5)
        assertEquals(1, result.size)
    }
}
