package com.mapchina.map

import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpatialIndexTest {

    @Test
    fun `bounding_box_contains_interior_point`() {
        val box = BoundingBox(0f, 0f, 100f, 100f)
        assertTrue(box.contains(Offset(50f, 50f)))
    }

    @Test
    fun `bounding_box_excludes_outside_point`() {
        val box = BoundingBox(0f, 0f, 100f, 100f)
        assertTrue(!box.contains(Offset(150f, 150f)))
    }

    @Test
    fun `bounding_box_intersects_overlapping_box`() {
        val box1 = BoundingBox(0f, 0f, 100f, 100f)
        val box2 = BoundingBox(50f, 50f, 150f, 150f)
        assertTrue(box1.intersects(box2))
    }

    @Test
    fun `bounding_box_no_intersect_disjoint_box`() {
        val box1 = BoundingBox(0f, 0f, 50f, 50f)
        val box2 = BoundingBox(100f, 100f, 200f, 200f)
        assertTrue(!box1.intersects(box2))
    }

    @Test
    fun `query_returns_region_containing_point`() {
        val index = SpatialIndex()
        val triangle = listOf(Offset(0f, 0f), Offset(100f, 0f), Offset(50f, 100f))
        val region = IndexedRegion(
            regionId = "r1",
            bounds = BoundingBox(0f, 0f, 100f, 100f),
            polygon = triangle
        )
        index.insert(region)

        val results = index.query(Offset(50f, 30f))
        assertEquals(1, results.size)
        assertEquals("r1", results[0].regionId)
    }

    @Test
    fun `query_returns_empty_for_point_outside`() {
        val index = SpatialIndex()
        val triangle = listOf(Offset(0f, 0f), Offset(100f, 0f), Offset(50f, 100f))
        val region = IndexedRegion(
            regionId = "r1",
            bounds = BoundingBox(0f, 0f, 100f, 100f),
            polygon = triangle
        )
        index.insert(region)

        val results = index.query(Offset(200f, 200f))
        assertEquals(0, results.size)
    }

    @Test
    fun `query_nearest_returns_closest_region`() {
        val index = SpatialIndex()
        val region1 = IndexedRegion(
            regionId = "near",
            bounds = BoundingBox(0f, 0f, 50f, 50f),
            polygon = listOf(Offset(0f, 0f), Offset(50f, 0f), Offset(25f, 50f))
        )
        val region2 = IndexedRegion(
            regionId = "far",
            bounds = BoundingBox(100f, 100f, 150f, 150f),
            polygon = listOf(Offset(100f, 100f), Offset(150f, 100f), Offset(125f, 150f))
        )
        index.insert(region1)
        index.insert(region2)

        val results = index.queryNearest(Offset(25f, 15f))
        assertTrue(results.isNotEmpty())
        assertEquals("near", results[0].regionId)
    }

    @Test
    fun `clear_removes_all_regions`() {
        val index = SpatialIndex()
        val region = IndexedRegion(
            regionId = "r1",
            bounds = BoundingBox(0f, 0f, 100f, 100f),
            polygon = listOf(Offset(0f, 0f), Offset(100f, 0f), Offset(50f, 100f))
        )
        index.insert(region)
        index.clear()
        assertEquals(0, index.query(Offset(50f, 30f)).size)
    }
}
