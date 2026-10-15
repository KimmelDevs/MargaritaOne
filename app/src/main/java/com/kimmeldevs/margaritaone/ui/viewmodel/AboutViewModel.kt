package com.kimmeldevs.margaritaone.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import com.kimmeldevs.margaritaone.data.model.Official
import com.kimmeldevs.margaritaone.data.model.OfficialInput
import com.kimmeldevs.margaritaone.data.remote.Tables
import com.kimmeldevs.margaritaone.data.repository.OfficialRepository
import com.kimmeldevs.margaritaone.data.repository.RealtimeRepository
import com.kimmeldevs.margaritaone.util.ImageCompressor
import com.kimmeldevs.margaritaone.util.LocalStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/** About Santa Margarita: the officials list (saved for offline) and admin editing. */
class AboutViewModel(
    private val app: Application,
    private val repository: OfficialRepository,
    realtimeRepository: RealtimeRepository,
    localStore: LocalStore
) : ViewModel() {

    val officials = Loadable(viewModelScope, localStore.offlineCache("officials")) { repository.getOfficials() }

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        officials.refresh()
        realtimeRepository.tableChanges(Tables.OFFICIALS)
            .onEach { officials.refresh(showIndicator = false) }
            .catch { }
            .launchIn(viewModelScope)
    }

    fun photoUrl(official: Official): String? = official.photoPath?.let(repository::photoUrl)

    /**
     * Adds ([existing] = null) or edits an official. [newPhoto] replaces the current photo; [removePhoto] clears it.
     */
    fun save(
        existing: Official?,
        name: String,
        position: String,
        sortOrder: Int,
        newPhoto: Uri?,
        removePhoto: Boolean,
        onDone: () -> Unit
    ) {
        if (name.trim().length < 2) return run { _message.value = "Enter a name." }
        if (position.trim().length < 2) return run { _message.value = "Enter a position." }
        if (_isSaving.value) return
        _isSaving.value = true
        viewModelScope.launch {
            val oldPath = existing?.photoPath
            var uploaded: String? = null
            val error = runAction {
                uploaded = newPhoto?.let { repository.uploadPhoto(ImageCompressor.compressToJpeg(app, it)) }
                val photoPath = uploaded ?: if (removePhoto) null else oldPath
                val input = OfficialInput(name.trim(), position.trim(), photoPath, sortOrder)
                if (existing == null) repository.add(input) else repository.update(existing.id, input)
            }
            _isSaving.value = false
            if (error == null) {
                // The old picture is no longer used.
                if (oldPath != null && (uploaded != null || removePhoto)) repository.deletePhoto(oldPath)
                _message.value = if (existing == null) "Official added" else "Official updated"
                onDone()
                officials.refresh(showIndicator = false)
            } else {
                uploaded?.let { repository.deletePhoto(it) }
                _message.value = error
            }
        }
    }

    fun delete(official: Official) {
        viewModelScope.launch {
            _message.value = runAction { repository.delete(official) } ?: "Official removed"
            officials.refresh(showIndicator = false)
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    companion object {
        val Factory = appViewModelFactory {
            AboutViewModel(this[APPLICATION_KEY] as Application, it.officialRepository, it.realtimeRepository, it.localStore)
        }
    }
}
