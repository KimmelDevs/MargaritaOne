package com.kimmeldevs.margaritaone.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kimmeldevs.margaritaone.data.model.PostDto
import com.kimmeldevs.margaritaone.data.model.PostReaction
import com.kimmeldevs.margaritaone.data.model.PostType
import com.kimmeldevs.margaritaone.data.model.ReactionType
import com.kimmeldevs.margaritaone.data.remote.Tables
import com.kimmeldevs.margaritaone.data.repository.PostRepository
import com.kimmeldevs.margaritaone.data.repository.RealtimeRepository
import com.kimmeldevs.margaritaone.util.LocalStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the reaction bar under a post needs. */
data class ReactionSummary(
    val total: Int,
    /** Most-used reactions first, for the little emoji stack (Facebook shows up to 3). */
    val top: List<ReactionType>,
    val counts: Map<ReactionType, Int>,
    val mine: ReactionType?
) {
    companion object {
        val EMPTY = ReactionSummary(0, emptyList(), emptyMap(), null)
    }
}

class BulletinViewModel(
    private val postRepository: PostRepository,
    realtimeRepository: RealtimeRepository,
    localStore: LocalStore,
    private val currentUserId: String,
    /** true = Profile's "Posts I reacted to" list instead of the whole bulletin. */
    private val reactedOnly: Boolean = false
) : ViewModel() {

    private val cacheKey = if (reactedOnly) "bulletin_reacted" else "bulletin"
    private val savedReactions = localStore.offlineCache<Map<String, List<PostReaction>>>("${cacheKey}_reactions")
    private val savedCommentCounts = localStore.offlineCache<Map<String, Int>>("${cacheKey}_comment_counts")

    val posts = Loadable(viewModelScope, localStore.offlineCache(cacheKey)) {
        val list = if (reactedOnly) postRepository.getReactedPosts() else postRepository.getPosts()
        list.also { loadReactions(it) }
    }

    /** post id -> everyone's reactions on it. Updated optimistically when the user reacts. */
    private val _reactions = MutableStateFlow(savedReactions.read().orEmpty())
    val reactions: StateFlow<Map<String, List<PostReaction>>> = _reactions.asStateFlow()

    /** post id -> number of comments. */
    private val _commentCounts = MutableStateFlow(savedCommentCounts.read().orEmpty())
    val commentCounts: StateFlow<Map<String, Int>> = _commentCounts.asStateFlow()

    /** null = all types. */
    private val _filter = MutableStateFlow<PostType?>(null)
    val filter: StateFlow<PostType?> = _filter.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        posts.refresh()
        realtimeRepository.tableChanges(Tables.POSTS)
            .onEach { posts.refresh(showIndicator = false) }
            .catch { /* live updates are best-effort; pull to refresh still works */ }
            .launchIn(viewModelScope)
        realtimeRepository.tableChanges(Tables.POST_REACTIONS)
            .onEach { reloadReactions() }
            .catch { }
            .launchIn(viewModelScope)
        realtimeRepository.tableChanges(Tables.POST_COMMENTS)
            .onEach { reloadReactions() }
            .catch { }
            .launchIn(viewModelScope)
    }

    fun setFilter(type: PostType?) {
        _filter.value = type
    }

    fun setQuery(value: String) {
        _query.value = value
    }

    fun visiblePosts(posts: List<PostDto>, type: PostType?, query: String): List<PostDto> {
        val q = query.trim()
        return posts.filter {
            (type == null || it.type == type) &&
                (q.isEmpty() || it.title.contains(q, true) || it.body.contains(q, true))
        }
    }

    fun imageUrls(post: PostDto): List<String> = post.allImagePaths.map(postRepository::attachmentUrl)

    fun documentUrl(post: PostDto): String? = post.documentPath?.let(postRepository::attachmentUrl)

    fun summary(postId: String, all: Map<String, List<PostReaction>>): ReactionSummary {
        val list = all[postId].orEmpty()
        if (list.isEmpty()) return ReactionSummary.EMPTY
        val counts = list.groupingBy { it.reaction }.eachCount()
        return ReactionSummary(
            total = list.size,
            top = counts.entries.sortedByDescending { it.value }.take(3).map { it.key },
            counts = counts,
            mine = list.firstOrNull { it.userId == currentUserId }?.reaction
        )
    }

    /**
     * Facebook behaviour: picking the reaction you already have removes it; picking another one switches.
     * The UI updates immediately; if the server rejects it, the real state is reloaded.
     */
    fun react(postId: String, picked: ReactionType) {
        val current = _reactions.value[postId].orEmpty().firstOrNull { it.userId == currentUserId }?.reaction
        val next = if (picked == current) null else picked

        _reactions.update { map ->
            val others = map[postId].orEmpty().filterNot { it.userId == currentUserId }
            map + (postId to (if (next == null) others else others + PostReaction(postId, currentUserId, next)))
        }
        viewModelScope.launch {
            val error = runAction { postRepository.setReaction(postId, current, next) }
            if (error != null) {
                _message.value = error
                reloadReactions()
            }
        }
    }

    fun deletePost(post: PostDto) {
        viewModelScope.launch {
            _message.value = runAction { postRepository.deletePost(post) } ?: "Post deleted"
            posts.refresh(showIndicator = false)
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    /** Reactions and comment counts for the loaded posts. */
    private suspend fun loadReactions(posts: List<PostDto>) {
        val ids = posts.map { it.id }
        runCatching { postRepository.getReactions(ids) }
            .onSuccess { list -> _reactions.value = list.groupBy { it.postId }.also(savedReactions::write) }
        runCatching { postRepository.commentCounts(ids) }
            .onSuccess { _commentCounts.value = it.also(savedCommentCounts::write) }
    }

    private fun reloadReactions() {
        val loaded = (posts.state.value as? UiState.Success)?.data ?: return
        viewModelScope.launch { loadReactions(loaded) }
    }

    companion object {
        fun factory(currentUserId: String, reactedOnly: Boolean = false) = appViewModelFactory {
            BulletinViewModel(it.postRepository, it.realtimeRepository, it.localStore, currentUserId, reactedOnly)
        }
    }
}
