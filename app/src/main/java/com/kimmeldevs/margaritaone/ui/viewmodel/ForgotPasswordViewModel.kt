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

data class ForgotPasswordState(
    /** Set once the code has been emailed; the screen then asks for the code and new password. */
    val sentTo: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Two steps: email a 6-digit code, then verify it and set a new password.
 * Verifying signs the user in, so the session flow takes them into the app afterwards.
 */
class ForgotPasswordViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(ForgotPasswordState())
    val state: StateFlow<ForgotPasswordState> = _state.asStateFlow()

    fun sendCode(email: String) {
        val trimmed = email.trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()) return showError("Enter a valid email address.")
        run {
            authRepository.sendPasswordReset(trimmed)
            _state.update { it.copy(sentTo = trimmed) }
        }
    }

    fun resetPassword(code: String, newPassword: String, confirmPassword: String) {
        val email = _state.value.sentTo ?: return
        when {
            code.trim().length < 6 -> return showError("Enter the code from the email.")
            newPassword.length < 8 -> return showError("Password must be at least 8 characters.")
            newPassword != confirmPassword -> return showError("Passwords don't match.")
        }
        run { authRepository.resetPassword(email, code.trim(), newPassword) }
    }

    /** Go back to step 1 (wrong email, or to request a new code). */
    fun startOver() = _state.update { ForgotPasswordState() }

    private fun showError(message: String) = _state.update { it.copy(error = message) }

    private fun run(block: suspend () -> Unit) {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val error = runAction(block)
            _state.update { it.copy(isLoading = false, error = error) }
        }
    }

    companion object {
        val Factory = appViewModelFactory { ForgotPasswordViewModel(it.authRepository) }
    }
}
