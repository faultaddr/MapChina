package com.mapchina.ui.attraction

import kotlin.test.Test
import kotlin.test.assertEquals

class AttractionDetailImageTest {

    @Test
    fun recommendationImage_isFirstAndDeduplicated() {
        assertEquals(
            listOf("card.jpg", "second.jpg"),
            attractionDetailImageUrls(
                primaryImageUrl = "card.jpg",
                detailImageUrls = listOf("card.jpg", "second.jpg", "")
            )
        )
    }

    @Test
    fun blankPrimary_usesDetailImages() {
        assertEquals(
            listOf("detail.jpg"),
            attractionDetailImageUrls(" ", listOf("detail.jpg"))
        )
    }

    @Test
    fun blankAndDuplicateDetailImages_areRemovedInOrder() {
        assertEquals(
            listOf("first.jpg", "second.jpg"),
            attractionDetailImageUrls(
                primaryImageUrl = null,
                detailImageUrls = listOf("", "first.jpg", "first.jpg", "second.jpg")
            )
        )
    }
}
