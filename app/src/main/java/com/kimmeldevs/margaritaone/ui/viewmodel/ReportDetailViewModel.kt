package com.kimmeldevs.margaritaone.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import com.kimmeldevs.margaritaone.data.model.NewReportUpdate
import com.kimmeldevs.margaritaone.data.model.Profile
import com.kimmeldevs.margaritaone.data.model.ReportDto
import com.kimmeldevs.margaritaone.data.model.ReportStatus
import com.kimmeldevs.margaritaone.data.model.ReportUpdateDto
import com.kimmeldevs.margaritaone.data.model.ReportView
import com.kimmeldevs.margaritaone.data.remote.Tables
import com.kimmeldevs.margaritaone.data.repository.DirectoryRepository
import com.kimmeldevs.margaritaone.data.repository.ProfileRepository
import com.kimmeldevs.margaritaone.data.repository.RealtimeRepository
import com.kimmeldevs.margaritaone.data.repository.ReportRepository
import com.kimmeldevs.margaritaone.navigation.Screen
import com.kimmeldevs.margaritaone.util.LocalStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class ReportDetail(
    val report: ReportDto,
    val timeline: List<ReportUpdateDto>,
    /** Signed URL for the private photo, valid for an hour. */
    val photoUrl: String?,
    /** Only loaded for staff (citizens can't read other profiles anyway). */
    val reporter: Profile?,
    val assignee: Profile?,
    val barangayName: String? = null,
    /** Staff and admins who opened this report. Only loaded for staff/admins. */
    val seenBy: List<ReportView> = emptyList()
)

data class ActionState(
    val isSaving: Boolean = false,
    val message: String? = null
)

class ReportDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val reportRepository: ReportRepository,
    private val profileRepository: ProfileRepository,
    realtimeRepository: RealtimeRepository,
    private val localStore: LocalStore,
    private val directoryRepository: DirectoryRepository,
    private val isStaff: Boolean,
    val currentUserId: String
) : ViewModel() {

    private val reportId: String = checkNotNull(savedStateHandle[Screen.ReportDetail.ARG_REPORT_ID])

    /** Marked once per visit; re-marking on every refresh would loop with the realtime listener. */
    private var markedSeen = false

    val detail = Loadable(viewModelScope, localStore.offlineCache("report_$reportId")) {
        val report = reportRepository.getReport(reportId) ?: error("Report not found")
        if (isStaff && !markedSeen) {
            markedSeen = runCatching { reportRepository.markSeen(reportId) }.isSuccess
        }
        // Opening the report clears its "Updated" dot in My reports.
        localStore.markReportSeen(report.id, report.updatedAt)
        ReportDetail(
            report = report,
            timeline = reportRepository.getTimeline(reportId),
            photoUrl = report.photoPath?.let { runCatching { reportRepository.photoUrl(it) }.getOrNull() },
            reporter = if (isStaff) profileRepository.getProfile(report.reporterId) else null,
            assignee = if (isStaff) report.assignedTo?.let { profileRepository.getProfile(it) } else null,
            barangayName = report.barangayId?.let { id ->
                runCatching { directoryRepository.getBarangays() }.getOrNull()?.firstOrNull { it.id == id }?.name
            },
            seenBy = if (isStaff) runCatching { reportRepository.getViews(reportId) }.getOrDefault(emptyList()) else emptyList()
        )
    }

    private val _actionState = MutableStateFlow(ActionState())
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()

    init {
        detail.refresh()
        merge(
            realtimeRepository.tableChanges(Tables.REPORTS),
            realtimeRepository.tableChanges(Tables.REPORT_UPDATES),
            realtimeRepository.tableChanges(Tables.REPORT_VIEWS)
        )
            .onEach { detail.refresh(showIndicator = false) }
            .catch { /* best-effort */ }
            .launchIn(viewModelScope)
    }

    /** Staff only (RLS). A status here also updates the report's status via a database trigger. */
    fun addUpdate(status: ReportStatus?, note: String, onDone: () -> Unit = {}) {
        if (status == null && note.isBlank()) {
            _actionState.value = ActionState(message = "Choose a status or write a note.")
            return
        }
        runAndRefresh(successMessage = if (status != null) "Status updated" else "Note added", onDone) {
            reportRepository.addUpdate(NewReportUpdate(reportId, status, note.trim().ifBlank { null }))
        }
    }

    /** Staff claims the report, or releases it with [assign] = false (#21). */
    fun assignToMe(assign: Boolean) = runAndRefresh(if (assign) "Assigned to you" else "Unassigned") {
        reportRepository.assign(reportId, if (assign) currentUserId else null)
    }

    /** Reporter answers "Was this fixed?" on their resolved report (#11). */
    fun rateResolution(fixed: Boolean, feedback: String) =
        runAndRefresh("Thanks for your feedback") {
            reportRepository.rateResolution(reportId, if (fixed) 1 else -1, feedback.trim().ifBlank { null })
        }

    /** Reporter withdraws a pending report (#18). */
    fun cancelReport() = runAndRefresh("Report cancelled") { reportRepository.cancelReport(reportId) }

    private fun runAndRefresh(successMessage: String, onDone: () -> Unit = {}, block: suspend () -> Unit) {
        if (_actionState.value.isSaving) return
        _actionState.value = ActionState(isSaving = true)
        viewModelScope.launch {
            val error = runAction(block)
            _actionState.value = ActionState(message = error ?: successMessage)
            if (error == null) {
                onDone()
                detail.refresh(showIndicator = false)
            }
        }
    }

    fun clearMessage() {
        _actionState.value = _actionState.value.copy(message = null)
    }

    companion object {
        fun factory(isStaff: Boolean, currentUserId: String) = appViewModelFactory {
            ReportDetailViewModel(
                createSavedStateHandle(),
                it.reportRepository,
                it.profileRepository,
                it.realtimeRepository,
                it.localStore,
                it.directoryRepository,
                isStaff,
                currentUserId
            )
        }
    }
}
