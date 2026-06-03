package com.mapchina.ui.community

import com.mapchina.data.remote.CommentDto
import com.mapchina.data.remote.CommunityPostDto
import com.mapchina.data.remote.MapChinaApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CommunityFeedUi(
    val posts: List<CommunityPostDto> = emptyList(),
    val isLoading: Boolean = false,
    val page: Int = 1,
    val hasMore: Boolean = true
)

data class PostDetailUi(
    val post: CommunityPostDto? = null,
    val comments: List<CommentDto> = emptyList(),
    val isLoading: Boolean = false,
    val commentText: String = ""
)

class CommunityViewModel(
    private val apiClient: MapChinaApiClient
) {
    private val vmScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _feedUi = MutableStateFlow(CommunityFeedUi())
    val feedUi: StateFlow<CommunityFeedUi> = _feedUi.asStateFlow()

    private val _detailUi = MutableStateFlow(PostDetailUi())
    val detailUi: StateFlow<PostDetailUi> = _detailUi.asStateFlow()

    fun loadFeed(refresh: Boolean = false) {
        if (_feedUi.value.isLoading) return
        val page = if (refresh) 1 else _feedUi.value.page
        _feedUi.value = _feedUi.value.copy(isLoading = true)
        vmScope.launch {
            val posts = apiClient.getCommunityFeed(page = page)
            val merged = if (refresh) posts else _feedUi.value.posts + posts
            _feedUi.value = CommunityFeedUi(
                posts = merged,
                isLoading = false,
                page = page + 1,
                hasMore = posts.size >= 20
            )
        }
    }

    fun loadPostDetail(postId: String) {
        _detailUi.value = PostDetailUi(isLoading = true)
        vmScope.launch {
            val post = apiClient.getCommunityPost(postId)
            val comments = apiClient.getComments(postId)
            _detailUi.value = PostDetailUi(
                post = post,
                comments = comments,
                isLoading = false
            )
        }
    }

    fun likePost(postId: String) {
        vmScope.launch {
            apiClient.likePost(postId)
            val currentPosts = _feedUi.value.posts.map {
                if (it.id == postId) {
                    val liked = !it.likedByMe
                    it.copy(
                        likedByMe = liked,
                        likeCount = if (liked) it.likeCount + 1 else it.likeCount - 1
                    )
                } else it
            }
            _feedUi.value = _feedUi.value.copy(posts = currentPosts)
            val detailPost = _detailUi.value.post
            if (detailPost?.id == postId) {
                val liked = !detailPost.likedByMe
                _detailUi.value = _detailUi.value.copy(
                    post = detailPost.copy(
                        likedByMe = liked,
                        likeCount = if (liked) detailPost.likeCount + 1 else detailPost.likeCount - 1
                    )
                )
            }
        }
    }

    fun updateCommentText(text: String) {
        _detailUi.value = _detailUi.value.copy(commentText = text)
    }

    fun submitComment(postId: String) {
        val text = _detailUi.value.commentText
        if (text.isBlank()) return
        vmScope.launch {
            val success = apiClient.createComment(postId, text)
            if (success) {
                _detailUi.value = _detailUi.value.copy(commentText = "")
                loadPostDetail(postId)
            }
        }
    }

    fun publishPost(title: String, content: String, coverImage: String? = null, regionId: String? = null, attractionId: String? = null, onSuccess: () -> Unit = {}) {
        vmScope.launch {
            val success = apiClient.createCommunityPost(title, content, coverImage, regionId, attractionId)
            if (success) onSuccess()
        }
    }

    fun clearDetail() {
        _detailUi.value = PostDetailUi()
    }
}
