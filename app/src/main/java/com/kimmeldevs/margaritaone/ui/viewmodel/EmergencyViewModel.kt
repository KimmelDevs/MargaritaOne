package com.kimmeldevs.margaritaone.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kimmeldevs.margaritaone.data.model.PostDto
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
import kotlinx.coroutines.launch

/** The latest emergency post (last 72h), shown as a banner on every screen until dismissed (#14). */
class EmergencyViewModel(
    private val postRepository: PostRepository,
    realtimeRepository: RealtimeRepository,
    private val localStore: LocalStore
) : ViewModel() {

    /** Saved so the alert still shows with no signal. Holds a list because the cache can't store "none". */
    private val saved = localStore.offlineCache<List<PostDto>>("emergency")

    private val _emergency = MutableStateFlow(saved.read()?.firstOrNull()?.takeUnless { localStore.isEmergencyDismissed(it.id) })
    val emergency: StateFlow<PostDto?> = _emergency.asStateFlow()

    init {
        load()
        realtimeRepository.tableChanges(Tables.POSTS)
            .onEach { load() }
            .catch { }
            .launchIn(viewModelScope)
    }

    fun load() {
        viewModelScope.launch {
            // On failure (no signal) keep whatever is showing.
            val latest = runCatching { postRepository.latestEmergency() }.getOrElse { return@launch }
            saved.write(listOfNotNull(latest))
            _emergency.value = latest?.takeUnless { localStore.isEmergencyDismissed(it.id) }
        }
    }

    fun dismiss() {
        _emergency.value?.let { localStore.dismissEmergency(it.id) }
        _emergency.value = null
    }

    companion object {
        val Factory = appViewModelFactory { EmergencyViewModel(it.postRepository, it.realtimeRepository, it.localStore) }
    }
}
