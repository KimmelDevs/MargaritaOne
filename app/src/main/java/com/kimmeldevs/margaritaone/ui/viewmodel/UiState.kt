package com.kimmeldevs.margaritaone.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kimmeldevs.margaritaone.MargaritaOneApp
import com.kimmeldevs.margaritaone.data.AppContainer
import com.kimmeldevs.margaritaone.data.isNetworkError
import com.kimmeldevs.margaritaone.data.remote.SupabaseProvider
import com.kimmeldevs.margaritaone.util.OfflineCache
import io.github.jan.supabase.exceptions.RestException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

/** Turns an exception into a message that is fit to show a user. */
fun Throwable.userMessage(): String = when {
    this is RestException -> error.ifBlank { message ?: "Request failed" }
    // Includes timeouts, which would otherwise show a raw message with the server address.
    isNetworkError() -> "Can't reach the server. Check your internet connection."
    else -> message ?: "Something went wrong"
}

/** Runs [block] and maps any failure to [UiState.Error]. */
internal suspend fun <T> loadState(block: suspend () -> T): UiState<T> =
    try {
        UiState.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        UiState.Error(e.userMessage())
    }

/** Runs [block], returning null on success or a user-facing error message on failure. */
internal suspend fun runAction(block: suspend () -> Unit): String? =
    try {
        block()
        null
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        e.userMessage()
    }

/**
 * A value loaded from the network, with pull-to-refresh support.
 * With a [cache], the last good copy shows straight away and stays on screen when a refresh fails (no signal).
 */
class Loadable<T>(
    private val scope: CoroutineScope,
    private val cache: OfflineCache<T>? = null,
    private val load: suspend () -> T
) {
    private val _state = MutableStateFlow<UiState<T>>(cache?.read()?.let { UiState.Success(it) } ?: UiState.Loading)
    val state: StateFlow<UiState<T>> = _state.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var job: Job? = null

    /** Silent reload when a screen is shown again; skips the first show (init already loads). */
    fun refreshIfLoaded() {
        if (_state.value !is UiState.Loading) refresh(showIndicator = false)
    }

    /** [showIndicator] is false for silent background refreshes (e.g. realtime events). */
    fun refresh(showIndicator: Boolean = true) {
        job?.cancel()
        job = scope.launch {
            // No spinner while the login is still being restored offline; the saved copy is on screen.
            SupabaseProvider.awaitSessionReady()
            if (showIndicator && _state.value is UiState.Success) _isRefreshing.value = true
            try {
                when (val result = loadState(load)) {
                    is UiState.Success -> {
                        _state.value = result
                        cache?.let { withContext(Dispatchers.IO) { it.write(result.data) } }
                    }
                    // Keep showing what we already have; the offline banner explains why it isn't updating.
                    is UiState.Error -> if (_state.value !is UiState.Success) _state.value = result
                    UiState.Loading -> Unit
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}

/** ViewModel factory with access to the app's repositories. */
inline fun <reified VM : ViewModel> appViewModelFactory(
    crossinline create: CreationExtras.(AppContainer) -> VM
): ViewModelProvider.Factory = viewModelFactory {
    initializer { create((this[APPLICATION_KEY] as MargaritaOneApp).container) }
}
