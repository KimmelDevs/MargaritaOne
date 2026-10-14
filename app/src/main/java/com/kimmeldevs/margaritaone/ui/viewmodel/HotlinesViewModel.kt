package com.kimmeldevs.margaritaone.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kimmeldevs.margaritaone.data.model.EmergencyContact
import com.kimmeldevs.margaritaone.data.model.EmergencyContactInput
import com.kimmeldevs.margaritaone.data.model.HotlineCategory
import com.kimmeldevs.margaritaone.data.model.HotlineCategoryInput
import com.kimmeldevs.margaritaone.data.remote.Tables
import com.kimmeldevs.margaritaone.data.repository.EmergencyContactRepository
import com.kimmeldevs.margaritaone.data.repository.RealtimeRepository
import com.kimmeldevs.margaritaone.util.LocalStore
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class HotlinesState(
    val contacts: List<EmergencyContact> = emptyList(),
    val categories: List<HotlineCategory> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    /** Set when the latest download failed; cached data is still shown. */
    val loadError: String? = null,
    val isSaving: Boolean = false,
    val message: String? = null
)

/**
 * Emergency hotlines and their categories. Shows the copy saved on the phone immediately (works with no signal),
 * then refreshes from Supabase. Admins can manage both (enforced by RLS).
 */
class HotlinesViewModel(
    private val repository: EmergencyContactRepository,
    realtimeRepository: RealtimeRepository,
    private val localStore: LocalStore
) : ViewModel() {

    private val _state = MutableStateFlow(
        HotlinesState(
            contacts = localStore.cachedContacts,
            categories = localStore.cachedHotlineCategories,
            isLoading = localStore.cachedContacts.isEmpty()
        )
    )
    val state: StateFlow<HotlinesState> = _state.asStateFlow()

    init {
        refresh(showIndicator = false)
        merge(
            realtimeRepository.tableChanges(Tables.EMERGENCY_CONTACTS),
            realtimeRepository.tableChanges(Tables.HOTLINE_CATEGORIES)
        )
            .onEach { refresh(showIndicator = false) }
            .catch { }
            .launchIn(viewModelScope)
    }

    fun refresh(showIndicator: Boolean = true) {
        viewModelScope.launch {
            if (showIndicator) _state.value = _state.value.copy(isRefreshing = true)
            val result = loadState {
                val contacts = async { repository.getContacts() }
                // Categories are optional: if migration 0006 hasn't run yet, hotlines still load.
                val categories = async { runCatching { repository.getCategories() }.getOrDefault(emptyList()) }
                contacts.await() to categories.await()
            }
            when (result) {
                is UiState.Success -> {
                    val (contacts, categories) = result.data
                    localStore.cachedContacts = contacts
                    localStore.cachedHotlineCategories = categories
                    _state.value = _state.value.copy(
                        contacts = contacts,
                        categories = categories,
                        isLoading = false,
                        isRefreshing = false,
                        loadError = null
                    )
                }
                is UiState.Error -> _state.value = _state.value.copy(
                    isLoading = false, isRefreshing = false, loadError = result.message
                )
                UiState.Loading -> Unit
            }
        }
    }

    /** Hotlines grouped under their category, categories in admin-set order; uncategorised last. */
    fun grouped(state: HotlinesState): List<Pair<HotlineCategory?, List<EmergencyContact>>> {
        val byId = state.categories.associateBy { it.id }
        val groups = state.contacts.groupBy { contact -> contact.categoryId?.let(byId::get) }
        return state.categories.mapNotNull { category -> groups[category]?.let { category to it } } +
            listOfNotNull(groups[null]?.let { null to it })
    }

    /** Add when [existing] is null, otherwise update it. */
    fun saveContact(existing: EmergencyContact?, input: EmergencyContactInput, onDone: () -> Unit) {
        val name = input.name.trim()
        val phone = input.phone.trim()
        when {
            name.length < 2 -> return showMessage("Enter a name.")
            phone.length < 3 -> return showMessage("Enter a phone number.")
        }
        val clean = input.copy(name = name, phone = phone, note = input.note?.trim()?.ifBlank { null })
        mutate(if (existing == null) "Hotline added" else "Hotline updated", onDone) {
            if (existing == null) repository.add(clean) else repository.update(existing.id, clean)
        }
    }

    fun deleteContact(contact: EmergencyContact) = mutate("Hotline deleted") { repository.delete(contact.id) }

    fun saveCategory(existing: HotlineCategory?, input: HotlineCategoryInput, onDone: () -> Unit) {
        val name = input.name.trim()
        if (name.length < 2) return showMessage("Enter a name.")
        val clean = input.copy(name = name)
        mutate(if (existing == null) "Category added" else "Category updated", onDone) {
            if (existing == null) repository.addCategory(clean) else repository.updateCategory(existing.id, clean)
        }
    }

    fun deleteCategory(category: HotlineCategory) =
        mutate("Category deleted") { repository.deleteCategory(category.id) }

    private fun mutate(success: String, onDone: () -> Unit = {}, block: suspend () -> Unit) {
        if (_state.value.isSaving) return
        _state.value = _state.value.copy(isSaving = true)
        viewModelScope.launch {
            val error = runAction(block)
            _state.value = _state.value.copy(isSaving = false, message = error ?: success)
            if (error == null) {
                onDone()
                refresh(showIndicator = false)
            }
        }
    }

    fun showMessage(message: String) {
        _state.value = _state.value.copy(message = message)
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }

    companion object {
        val Factory = appViewModelFactory {
            HotlinesViewModel(it.emergencyContactRepository, it.realtimeRepository, it.localStore)
        }
    }
}
