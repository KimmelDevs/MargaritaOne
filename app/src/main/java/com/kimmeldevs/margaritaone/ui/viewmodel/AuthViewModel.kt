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
    val info: String? = null,
    /** Email to fill in on the login screen (after signing up, or when the account already exists). */
    val prefillEmail: String? = null,
    /** Sign-up worked but the email must be confirmed first: the sign-up screen goes back to login. */
    val signUpComplete: Boolean = false,
    /** Sign-up was refused because the email is already registered: offer "Sign in instead". */
    val accountExists: Boolean = false
)

/** Login and sign-up forms. On success the session flow in [SessionViewModel] takes over navigation. */
class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(AuthFormState())
    val state: StateFlow<AuthFormState> = _state.asStateFlow()

    fun clearMessages() = _state.update { it.copy(error = null, info = null, accountExists = false) }

    /** The login screen has taken the email and the sign-up screen has gone back. */
    fun consumeSignUpResult() = _state.update { it.copy(prefillEmail = null, signUpComplete = false, accountExists = false) }

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
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, error = null, info = null, accountExists = false) }
        viewModelScope.launch {
            var needsConfirmation = false
            val error = runAction { needsConfirmation = authRepository.signUp(trimmedEmail, password, fullName.trim()) }
            _state.update {
                when {
                    // Pressing Sign up again after it already worked lands here too.
                    error != null && error.contains("already registered", ignoreCase = true) -> it.copy(
                        isLoading = false,
                        error = "An account with this email already exists. Sign in instead.",
                        accountExists = true,
                        prefillEmail = trimmedEmail
                    )
                    error != null -> it.copy(isLoading = false, error = error)
                    // Without email confirmation the user is signed in now and the session takes over.
                    needsConfirmation -> it.copy(
                        isLoading = false,
                        info = "Account created. Check your email for a confirmation link, then sign in.",
                        prefillEmail = trimmedEmail,
                        signUpComplete = true
                    )
                    else -> it.copy(isLoading = false)
                }
            }
        }
    }

    private fun showError(message: String) = _state.update { it.copy(error = message, info = null, accountExists = false) }

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
