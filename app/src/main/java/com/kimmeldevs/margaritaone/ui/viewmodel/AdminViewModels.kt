package com.kimmeldevs.margaritaone.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kimmeldevs.margaritaone.data.model.AuditEntry
import com.kimmeldevs.margaritaone.data.model.Barangay
import com.kimmeldevs.margaritaone.data.repository.DirectoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Admin: the barangay list used by reports and profiles (#17). */
class BarangaysViewModel(private val repository: DirectoryRepository) : ViewModel() {

    val barangays = Loadable(viewModelScope) { repository.getBarangays() }

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        barangays.refresh()
    }

    fun save(existing: Barangay?, name: String, onDone: () -> Unit) {
        val clean = name.trim()
        if (clean.length < 2) {
            _message.value = "Enter a name."
            return
        }
        viewModelScope.launch {
            val error = runAction {
                if (existing == null) repository.addBarangay(clean) else repository.renameBarangay(existing.id, clean)
            }
            _message.value = error ?: if (existing == null) "Barangay added" else "Barangay renamed"
            if (error == null) {
                onDone()
                barangays.refresh(showIndicator = false)
            }
        }
    }

    fun delete(barangay: Barangay) {
        viewModelScope.launch {
            _message.value = runAction { repository.deleteBarangay(barangay.id) } ?: "Barangay deleted"
            barangays.refresh(showIndicator = false)
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    companion object {
        val Factory = appViewModelFactory { BarangaysViewModel(it.directoryRepository) }
    }
}

/** What the activity log can be narrowed to; matches the prefix of `audit_log.action`. */
enum class AuditFilter(val label: String, val prefix: String) {
    REPORTS("Reports", "report."),
    USERS("Users", "user."),
    POSTS("Posts", "post."),
    HOTLINES("Hotlines", "hotline."),
    COMMENTS("Comments", "comment.")
}

/** Admin: who did what, written by database triggers (#19). */
class ActivityLogViewModel(repository: DirectoryRepository) : ViewModel() {

    val entries = Loadable(viewModelScope) { repository.getAuditLog() }

    private val _filter = MutableStateFlow<AuditFilter?>(null)
    val filter: StateFlow<AuditFilter?> = _filter.asStateFlow()

    init {
        entries.refresh()
    }

    fun setFilter(value: AuditFilter?) {
        _filter.value = value
    }

    fun visible(all: List<AuditEntry>, filter: AuditFilter?): List<AuditEntry> =
        if (filter == null) all else all.filter { it.action.startsWith(filter.prefix) }

    companion object {
        val Factory = appViewModelFactory { ActivityLogViewModel(it.directoryRepository) }
    }
}
