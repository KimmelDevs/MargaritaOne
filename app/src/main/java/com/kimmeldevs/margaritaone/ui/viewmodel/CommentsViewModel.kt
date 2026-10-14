package com.kimmeldevs.margaritaone.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kimmeldevs.margaritaone.data.model.PostComment
import com.kimmeldevs.margaritaone.data.remote.Tables
import com.kimmeldevs.margaritaone.data.repository.PostRepository
import com.kimmeldevs.margaritaone.data.repository.RealtimeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class CommentsState(
    val comments: List<PostComment> = emptyList(),
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val error: String? = null
)

/** Comments on one post (#7). New comments from others arrive live. */
class CommentsViewModel(
    private val postId: String,
    private val repository: PostRepository,
    realtimeRepository: RealtimeRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CommentsState())
    val state: StateFlow<CommentsState> = _state.asStateFlow()

    init {
        load()
        realtimeRepository.tableChanges(Tables.POST_COMMENTS)
            .onEach { load() }
            .catch { }
            .launchIn(viewModelScope)
    }

    private fun load() {
        viewModelScope.launch {
            when (val result = loadState { repository.getComments(postId) }) {
                is UiState.Success -> _state.value = _state.value.copy(comments = result.data, isLoading = false)
                is UiState.Error -> _state.value = _state.value.copy(isLoading = false, error = result.message)
                UiState.Loading -> Unit
            }
        }
    }

    fun send(body: String, onSent: () -> Unit) {
        val text = body.trim()
        if (text.isEmpty() || _state.value.isSending) return
        _state.value = _state.value.copy(isSending = true, error = null)
        viewModelScope.launch {
            val error = runAction { repository.addComment(postId, text.take(1000)) }
            _state.value = _state.value.copy(isSending = false, error = error)
            if (error == null) {
                onSent()
                load()
            }
        }
    }

    fun delete(comment: PostComment) {
        viewModelScope.launch {
            val error = runAction { repository.deleteComment(comment.id) }
            _state.value = _state.value.copy(error = error)
            load()
        }
    }

    companion object {
        fun factory(postId: String) = appViewModelFactory {
            CommentsViewModel(postId, it.postRepository, it.realtimeRepository)
        }
    }
}
