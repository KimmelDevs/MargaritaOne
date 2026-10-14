package com.kimmeldevs.margaritaone.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kimmeldevs.margaritaone.data.model.Profile
import com.kimmeldevs.margaritaone.data.remote.SupabaseProvider
import com.kimmeldevs.margaritaone.data.repository.AuthRepository
import com.kimmeldevs.margaritaone.data.repository.ProfileRepository
import com.kimmeldevs.margaritaone.util.LocalStore
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SessionState {
    data object Loading : SessionState
    data object NotConfigured : SessionState
    data object SignedOut : SessionState
    data class SignedIn(val profile: Profile) : SessionState
    data class Error(val message: String) : SessionState
}

/**
 * App-wide: who is signed in and what their role is. Drives which navigation graph is shown.
 * With no signal, the profile saved on the phone is used so the app still opens (lists show their saved copies).
 */
class SessionViewModel(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val localStore: LocalStore
) : ViewModel() {

    private val _state = MutableStateFlow<SessionState>(SessionState.Loading)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    /** True while Supabase can't renew an expired login because there's no connection (it keeps retrying). */
    private var restoringOffline = false

    init {
        if (!SupabaseProvider.isConfigured) {
            _state.value = SessionState.NotConfigured
        } else {
            viewModelScope.launch {
                authRepository.sessionStatus.collect { status ->
                    restoringOffline = status is SessionStatus.RefreshFailure
                    when (status) {
                        is SessionStatus.Authenticated -> {
                            val userId = status.session.user?.id
                            val current = _state.value
                            // Token refreshes also emit Authenticated; don't reload for the same user.
                            if (current !is SessionState.SignedIn || current.profile.id != userId) {
                                loadProfile()
                            }
                        }
                        is SessionStatus.NotAuthenticated -> {
                            localStore.clearAccountData()
                            _state.value = SessionState.SignedOut
                        }
                        SessionStatus.Initializing -> _state.value = SessionState.Loading
                        // Opened offline with an expired login: open with the saved profile. Supabase retries by
                        // itself and emits Authenticated once there's signal.
                        is SessionStatus.RefreshFailure -> if (_state.value !is SessionState.SignedIn) {
                            _state.value = offlineState()
                        }
                    }
                }
            }
        }
    }

    /** Re-reads the profile, e.g. after the user edits it or an admin changes their role. */
    fun loadProfile() {
        viewModelScope.launch {
            val userId = authRepository.currentUserId() ?: run {
                _state.value = if (restoringOffline) offlineState() else SessionState.SignedOut
                return@launch
            }
            val current = _state.value
            if (current !is SessionState.SignedIn) _state.value = SessionState.Loading
            _state.value = when (val result = loadState { profileRepository.getProfile(userId) }) {
                is UiState.Success -> result.data?.let {
                    localStore.cachedProfile = it
                    SessionState.SignedIn(it)
                } ?: SessionState.Error("Your profile could not be found. Please contact the LGU.")
                // No signal: keep the profile already on screen, or the one saved on the phone.
                is UiState.Error -> (current as? SessionState.SignedIn)?.takeIf { it.profile.id == userId }
                    ?: localStore.cachedProfile?.takeIf { it.id == userId }?.let { SessionState.SignedIn(it) }
                    ?: SessionState.Error(result.message)
                UiState.Loading -> SessionState.Loading
            }
        }
    }

    private fun offlineState(): SessionState =
        localStore.cachedProfile?.let { SessionState.SignedIn(it) }
            ?: SessionState.Error("Can't reach the server. Check your internet connection.")

    fun signOut() {
        viewModelScope.launch {
            runAction { authRepository.signOut() }
            localStore.clearAccountData()
            _state.value = SessionState.SignedOut
        }
    }

    companion object {
        val Factory = appViewModelFactory { SessionViewModel(it.authRepository, it.profileRepository, it.localStore) }
    }
}
