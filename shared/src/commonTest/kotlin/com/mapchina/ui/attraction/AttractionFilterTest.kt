package com.mapchina.ui.attraction

import com.mapchina.domain.model.FootprintLevel
import kotlin.test.Test
import kotlin.test.assertEquals

class AttractionFilterTest {
    @Test
    fun levelSpecificUnvisitedFiltersExcludeVisitedAttractions() {
        val attractions = listOf(
            attraction("a5-new", "A5", null),
            attraction("a5-visited", "A5", FootprintLevel.DEEP),
            attraction("a4-new", "A4", null),
        )

        assertEquals(listOf("a5-new"), filterAttractions(attractions, AttractionFilter.A5_UNVISITED).map { it.id })
        assertEquals(listOf("a4-new"), filterAttractions(attractions, AttractionFilter.A4_UNVISITED).map { it.id })
    }

    private fun attraction(id: String, level: String, visitLevel: FootprintLevel?) = AttractionUi(
        id = id,
        name = id,
        level = level,
        regionId = "110000",
        latitude = 0.0,
        longitude = 0.0,
        description = null,
        imageUrl = null,
        visitLevel = visitLevel,
    )
}
