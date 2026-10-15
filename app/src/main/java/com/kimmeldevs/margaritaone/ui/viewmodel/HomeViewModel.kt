package com.kimmeldevs.margaritaone.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kimmeldevs.margaritaone.data.model.PostDto
import com.kimmeldevs.margaritaone.data.remote.Tables
import com.kimmeldevs.margaritaone.data.repository.PostRepository
import com.kimmeldevs.margaritaone.data.repository.RealtimeRepository
import com.kimmeldevs.margaritaone.util.Dates
import com.kimmeldevs.margaritaone.util.LocalStore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/** Home tab: the newest bulletin posts for "Latest updates" (saved for offline). */
class HomeViewModel(
    postRepository: PostRepository,
    realtimeRepository: RealtimeRepository,
    localStore: LocalStore
) : ViewModel() {

    val latest = Loadable(viewModelScope, localStore.offlineCache<List<PostDto>>("home_latest")) {
        postRepository.getPosts()
            .filter { (Dates.toMillis(it.publishedAt) ?: 0L) <= System.currentTimeMillis() }
            .sortedByDescending { Dates.toMillis(it.publishedAt) ?: 0L }
            .take(LATEST_COUNT)
    }

    init {
        latest.refresh()
        realtimeRepository.tableChanges(Tables.POSTS)
            .onEach { latest.refresh(showIndicator = false) }
            .catch { }
            .launchIn(viewModelScope)
    }

    companion object {
        private const val LATEST_COUNT = 4
        val Factory = appViewModelFactory { HomeViewModel(it.postRepository, it.realtimeRepository, it.localStore) }
    }
}
