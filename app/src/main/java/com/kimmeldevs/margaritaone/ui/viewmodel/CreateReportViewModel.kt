package com.kimmeldevs.margaritaone.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import com.kimmeldevs.margaritaone.data.Outbox
import com.kimmeldevs.margaritaone.data.isNetworkError
import com.kimmeldevs.margaritaone.data.model.Barangay
import com.kimmeldevs.margaritaone.data.model.NewReport
import com.kimmeldevs.margaritaone.data.repository.DirectoryRepository
import kotlinx.coroutines.CancellationException
import com.kimmeldevs.margaritaone.data.model.ReportCategory
import com.kimmeldevs.margaritaone.data.repository.ReportRepository
import com.kimmeldevs.margaritaone.util.ImageCompressor
import com.kimmeldevs.margaritaone.util.LatLng
import com.kimmeldevs.margaritaone.util.LocationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreateReportState(
    val photoUri: Uri? = null,
    val location: LatLng? = null,
    /** Admin-managed list (#17); empty until an admin adds barangays. */
    val barangays: List<Barangay> = emptyList(),
    val barangay: Barangay? = null,
    val isLocating: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    /** Set once the report is saved; the screen navigates to it. */
    val submittedId: String? = null,
    /** No connection: saved to the outbox and will send by itself (#10). */
    val savedOffline: Boolean = false
)

class CreateReportViewModel(
    private val app: Application,
    private val reportRepository: ReportRepository,
    private val directoryRepository: DirectoryRepository,
    private val outbox: Outbox
) : ViewModel() {

    private val _state = MutableStateFlow(CreateReportState())
    val state: StateFlow<CreateReportState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val list = runCatching { directoryRepository.getBarangays() }.getOrDefault(emptyList())
            _state.update { it.copy(barangays = list) }
        }
    }

    fun setBarangay(barangay: Barangay?) = _state.update { it.copy(barangay = barangay) }

    /** From the map picker (#9). */
    fun setLocation(location: LatLng) = _state.update { it.copy(location = location, error = null) }

    fun setPhoto(uri: Uri?) = _state.update { it.copy(photoUri = uri) }

    fun clearLocation() = _state.update { it.copy(location = null) }

    fun clearError() = _state.update { it.copy(error = null) }

    fun fetchLocation() {
        if (_state.value.isLocating) return
        _state.update { it.copy(isLocating = true, error = null) }
        viewModelScope.launch {
            val location = runCatching { LocationHelper.currentLocation(app) }.getOrNull()
            _state.update {
                it.copy(
                    isLocating = false,
                    location = location ?: it.location,
                    error = if (location == null) "Couldn't get your location. Make sure location is turned on." else null
                )
            }
        }
    }

    /** Checks required fields before showing the confirmation summary; sets [CreateReportState.error] if invalid. */
    fun validate(title: String, category: ReportCategory?): Boolean {
        val error = when {
            category == null -> "Choose a category."
            title.trim().length < 3 -> "Give the report a short title."
            else -> null
        }
        _state.update { it.copy(error = error) }
        return error == null
    }

    fun submit(title: String, description: String, category: ReportCategory?, address: String) {
        val current = _state.value
        if (current.isSubmitting) return
        when {
            category == null -> return _state.update { it.copy(error = "Choose a category.") }
            title.trim().length < 3 -> return _state.update { it.copy(error = "Give the report a short title.") }
        }
        _state.update { it.copy(isSubmitting = true, error = null) }
        val report = NewReport(
            title = title.trim(),
            description = description.trim(),
            category = category!!,
            latitude = current.location?.latitude,
            longitude = current.location?.longitude,
            address = address.trim().ifBlank { null },
            barangayId = current.barangay?.id
        )
        viewModelScope.launch {
            // Compress once; the same bytes go to the server or into the outbox.
            val jpeg = try {
                current.photoUri?.let { ImageCompressor.compressToJpeg(app, it) }
            } catch (e: Exception) {
                _state.update { it.copy(isSubmitting = false, error = e.userMessage()) }
                return@launch
            }
            try {
                val photoPath = jpeg?.let { reportRepository.uploadPhoto(it) }
                val saved = reportRepository.submitReport(report.copy(photoPath = photoPath))
                _state.update { it.copy(isSubmitting = false, submittedId = saved.id) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (e.isNetworkError()) {
                    outbox.add(report, jpeg)
                    _state.update { it.copy(isSubmitting = false, savedOffline = true) }
                } else {
                    _state.update { it.copy(isSubmitting = false, error = e.userMessage()) }
                }
            }
        }
    }

    companion object {
        val Factory = appViewModelFactory {
            CreateReportViewModel(this[APPLICATION_KEY] as Application, it.reportRepository, it.directoryRepository, it.outbox)
        }
    }
}
