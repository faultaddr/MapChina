package com.mapchina.map

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BoundaryParserTest {

    @Test
    fun parse_Feature_type_with_Polygon() {
        val json = """{"type":"Feature","properties":{"adcode":110000,"name":"北京市","center":[116.405,39.905]},"geometry":{"type":"Polygon","coordinates":[[[116.0,39.0],[117.0,39.0],[117.0,40.0],[116.0,40.0],[116.0,39.0]]]}}"""
        val result = BoundaryParser.parse(json)
        assertEquals(1, result.size)
        assertEquals(5, result[0].size)
        assertEquals(116.0, result[0][0].first)
        assertEquals(39.0, result[0][0].second)
    }

    @Test
    fun parse_FeatureCollection_type() {
        val json = """{"type":"FeatureCollection","features":[{"type":"Feature","properties":{"adcode":110101,"name":"东城区"},"geometry":{"type":"Polygon","coordinates":[[[116.4,39.9],[116.5,39.9],[116.5,40.0],[116.4,40.0],[116.4,39.9]]]}}]}"""
        val result = BoundaryParser.parse(json)
        assertEquals(1, result.size)
    }

    @Test
    fun parse_MultiPolygon_geometry() {
        val json = """{"type":"Feature","properties":{"adcode":810000,"name":"香港"},"geometry":{"type":"MultiPolygon","coordinates":[[[[114.0,22.0],[114.1,22.0],[114.1,22.1],[114.0,22.1],[114.0,22.0]]],[[[114.2,22.2],[114.3,22.2],[114.3,22.3],[114.2,22.3],[114.2,22.2]]]]}}"""
        val result = BoundaryParser.parse(json)
        assertEquals(2, result.size)
    }

    @Test
    fun parse_flat_coordinate_array_format() {
        val json = """[[116.0,39.0],[117.0,39.0],[117.0,40.0],[116.0,40.0],[116.0,39.0]]"""
        val result = BoundaryParser.parseFlatCoords(json)
        assertEquals(1, result.size)
        assertEquals(5, result[0].size)
    }

    @Test
    fun parse_empty_json_returns_empty_list() {
        val json = """{"type":"Unknown"}"""
        val result = BoundaryParser.parse(json)
        assertTrue(result.isEmpty())
    }
}
