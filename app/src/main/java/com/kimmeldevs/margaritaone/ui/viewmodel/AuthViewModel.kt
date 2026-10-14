package com.kimmeldevs.margaritaone.ui.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kimmeldevs.margaritaone.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthFormState(
    val isLoading: Boolean = false,
    val error: String? = null,
    /** Shown after sign-up when Supabase requires email confirmation. */
    val info: String? = null
)

/** Login and sign-up forms. On success the session flow in [SessionViewModel] takes over navigation. */
class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(AuthFormState())
    val state: StateFlow<AuthFormState> = _state.asStateFlow()

    fun clearMessages() = _state.update { it.copy(error = null, info = null) }

    fun signIn(email: String, password: String) {
        val trimmedEmail = email.trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) return showError("Enter a valid email address.")
        if (password.isEmpty()) return showError("Enter your password.")
        submit { authRepository.signIn(trimmedEmail, password) }
    }

    fun signUp(fullName: String, email: String, password: String, confirmPassword: String, acceptedPrivacy: Boolean) {
        val trimmedEmail = email.trim()
        when {
            fullName.isBlank() -> return showError("Enter your full name.")
            !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches() -> return showError("Enter a valid email address.")
            password.length < 8 -> return showError("Password must be at least 8 characters.")
            password != confirmPassword -> return showError("Passwords don't match.")
            !acceptedPrivacy -> return showError("Please read and accept the privacy notice.")
        }
        submit {
            val needsConfirmation = authRepository.signUp(trimmedEmail, password, fullName.trim())
            if (needsConfirmation) {
                _state.update {
                    it.copy(info = "Account created. Check $trimmedEmail for a confirmation link, then sign in.")
                }
            }
        }
    }

    private fun showError(message: String) = _state.update { it.copy(error = message, info = null) }

    private fun submit(block: suspend () -> Unit) {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, error = null, info = null) }
        viewModelScope.launch {
            val error = runAction(block)
            _state.update { it.copy(isLoading = false, error = error) }
        }
    }

    companion object {
        val Factory = appViewModelFactory { AuthViewModel(it.authRepository) }
    }
}
