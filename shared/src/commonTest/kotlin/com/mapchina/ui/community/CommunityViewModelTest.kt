package com.mapchina.ui.community

import com.mapchina.data.remote.CommunityPostDto
import kotlin.test.Test
import kotlin.test.assertEquals

class CommunityViewModelTest {
    @Test
    fun timedOutRefreshPreservesFeedAndPagination() {
        val existing = CommunityPostDto(
            id = "post-1",
            userId = "user-1",
            nickname = "Traveler",
            avatarUrl = null,
            title = "Existing",
            content = "Kept on timeout",
            coverImage = null,
            regionId = null,
            attractionId = null,
            likeCount = 0,
            commentCount = 0,
            likedByMe = false,
            createdAt = 1L,
        )
        val current = CommunityFeedUi(posts = listOf(existing), page = 3, hasMore = true, isLoading = true)

        val result = communityFeedAfterLoad(current, requestedPage = 1, refresh = true, posts = null)

        assertEquals(listOf(existing), result.posts)
        assertEquals(3, result.page)
        assertEquals(true, result.hasMore)
        assertEquals(false, result.isLoading)
    }
}
