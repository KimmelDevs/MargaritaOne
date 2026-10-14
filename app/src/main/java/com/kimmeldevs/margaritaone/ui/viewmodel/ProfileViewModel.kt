package com.kimmeldevs.margaritaone.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kimmeldevs.margaritaone.data.model.Barangay
import com.kimmeldevs.margaritaone.data.model.ProfileUpdate
import com.kimmeldevs.margaritaone.data.model.UserRole
import com.kimmeldevs.margaritaone.data.repository.AuthRepository
import com.kimmeldevs.margaritaone.data.repository.DirectoryRepository
import com.kimmeldevs.margaritaone.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SaveState(val isSaving: Boolean = false, val message: String? = null)

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
    directoryRepository: DirectoryRepository
) : ViewModel() {

    private val _saveState = MutableStateFlow(SaveState())
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    private val _barangays = MutableStateFlow<List<Barangay>>(emptyList())
    val barangays: StateFlow<List<Barangay>> = _barangays.asStateFlow()

    init {
        viewModelScope.launch {
            _barangays.value = runCatching { directoryRepository.getBarangays() }.getOrDefault(emptyList())
        }
    }

    fun save(userId: String, fullName: String, phone: String, barangay: String, onSaved: () -> Unit) {
        if (fullName.isBlank()) {
            _saveState.value = SaveState(message = "Name can't be empty.")
            return
        }
        _saveState.value = SaveState(isSaving = true)
        viewModelScope.launch {
            val error = runAction {
                profileRepository.updateProfile(
                    userId,
                    ProfileUpdate(
                        fullName = fullName.trim(),
                        phone = phone.trim().ifBlank { null },
                        barangay = barangay.trim().ifBlank { null }
                    )
                )
            }
            _saveState.value = SaveState(message = error ?: "Profile saved")
            if (error == null) onSaved()
        }
    }

    /**
     * Deletes the account and everything in it (#4). Admins are stopped here, before any photo is removed,
     * because the database refuses to delete an admin anyway.
     */
    fun deleteAccount(role: UserRole) {
        if (role == UserRole.ADMIN) {
            _saveState.value = SaveState(message = "Admins cannot delete their account. Ask another admin to change your role first.")
            return
        }
        _saveState.value = SaveState(isSaving = true)
        viewModelScope.launch {
            // On success the session ends and the app returns to the login screen by itself.
            val error = runAction { authRepository.deleteAccount() }
            _saveState.value = SaveState(message = error)
        }
    }

    fun clearMessage() {
        _saveState.value = _saveState.value.copy(message = null)
    }

    companion object {
        val Factory = appViewModelFactory {
            ProfileViewModel(it.profileRepository, it.authRepository, it.directoryRepository)
        }
    }
}
