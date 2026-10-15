package com.kimmeldevs.margaritaone.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import com.kimmeldevs.margaritaone.data.Outbox
import com.kimmeldevs.margaritaone.data.PendingReport
import com.kimmeldevs.margaritaone.data.model.Barangay
import com.kimmeldevs.margaritaone.data.model.ReportCategory
import com.kimmeldevs.margaritaone.data.model.ReportDto
import com.kimmeldevs.margaritaone.data.model.ReportStatus
import com.kimmeldevs.margaritaone.data.remote.Tables
import com.kimmeldevs.margaritaone.data.repository.DirectoryRepository
import com.kimmeldevs.margaritaone.data.repository.RealtimeRepository
import com.kimmeldevs.margaritaone.data.repository.ReportRepository
import com.kimmeldevs.margaritaone.util.Dates
import com.kimmeldevs.margaritaone.util.LocalStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

enum class DateRange(val label: String, val days: Int?) {
    WEEK("Last 7 days", 7),
    MONTH("Last 30 days", 30),
    ALL("All time", null)
}

enum class ReportSort(val label: String) {
    NEWEST("Newest first"),
    OLDEST("Oldest first"),
    UPDATED("Recently updated")
}

data class ReportFilters(
    val query: String = "",
    val dateRange: DateRange = DateRange.ALL,
    val category: ReportCategory? = null,
    val status: ReportStatus? = null,
    val sort: ReportSort = ReportSort.NEWEST,
    val assignedToMe: Boolean = false,
    val barangayId: String? = null
)

@Serializable
data class ReportListData(
    val reports: List<ReportDto>,
    /** photo_path -> signed URL, for card thumbnails. */
    val thumbnails: Map<String, String>,
    val barangays: List<Barangay> = emptyList(),
    /** Reports a staff member or admin has opened. Staff dashboard only. */
    val seenIds: Set<String> = emptySet()
) {
    val barangayNames: Map<String, String> get() = barangays.associate { it.id to it.name }
}

/**
 * Report lists. Citizens see their own reports (My reports tab);
 * staff see every report (Dashboard tab). RLS enforces the same split on the server.
 */
class ReportListViewModel(
    private val app: Application,
    private val reportRepository: ReportRepository,
    private val directoryRepository: DirectoryRepository,
    realtimeRepository: RealtimeRepository,
    private val localStore: LocalStore,
    private val outbox: Outbox,
    private val allReports: Boolean,
    private val currentUserId: String
) : ViewModel() {

    /** Staff dashboard (every report) rather than a resident's own list. */
    val isStaffList: Boolean get() = allReports

    /** Reports written offline that haven't been sent yet (#10). Only meaningful on My reports. */
    val pending: StateFlow<List<PendingReport>> = outbox.pending

    fun sendPendingNow() = outbox.retryAll()

    fun discardPending(item: PendingReport) = outbox.remove(item.localId)

    val reports = Loadable(viewModelScope, localStore.offlineCache(if (allReports) "reports_all" else "reports_mine")) {
        val list = if (allReports) reportRepository.getReports() else reportRepository.getMyReports()
        val thumbs = runCatching { reportRepository.photoUrls(list.mapNotNull { it.photoPath }) }
            .getOrDefault(emptyMap())
        val barangays = runCatching { directoryRepository.getBarangays() }.getOrDefault(emptyList())
        val seen = if (allReports) {
            runCatching { reportRepository.seenReportIds(list.map { it.id }) }.getOrDefault(emptySet())
        } else {
            emptySet()
        }
        ReportListData(list, thumbs, barangays, seen)
    }

    /** List or map (#8). Kept here so filters and search carry over when switching. */
    private val _showMap = MutableStateFlow(false)
    val showMap: StateFlow<Boolean> = _showMap.asStateFlow()

    fun toggleMap() {
        _showMap.value = !_showMap.value
    }

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() {
        _message.value = null
    }

    /**
     * Writes the currently filtered reports to a CSV file the user picked (#16).
     * Starts with a UTF-8 byte-order mark so Excel shows ñ and other characters correctly.
     */
    fun exportCsv(target: Uri, data: ReportListData, filters: ReportFilters) {
        val rows = apply(data.reports, filters)
        viewModelScope.launch {
            val error = runAction {
                withContext(Dispatchers.IO) {
                    val names = data.barangayNames
                    val header = listOf(
                        "Reference no", "Title", "Category", "Status", "Barangay", "Landmark", "Latitude", "Longitude",
                        "Submitted", "Last updated", "Fixed? (resident)", "Resident feedback", "Description"
                    )
                    val lines = rows.map { r ->
                        listOf(
                            r.referenceNumber.orEmpty(),
                            r.title,
                            r.category.label,
                            r.status.label,
                            r.barangayId?.let(names::get).orEmpty(),
                            r.address.orEmpty(),
                            r.latitude?.toString().orEmpty(),
                            r.longitude?.toString().orEmpty(),
                            Dates.dateTime(r.createdAt),
                            Dates.dateTime(r.updatedAt),
                            when (r.resolutionRating) { 1 -> "Yes"; -1 -> "No"; else -> "" },
                            r.resolutionFeedback.orEmpty(),
                            r.description
                        )
                    }
                    val csv = (listOf(header) + lines).joinToString("\r\n") { row -> row.joinToString(",", transform = ::csvCell) }
                    app.contentResolver.openOutputStream(target)?.use { out ->
                        out.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                        out.write(csv.toByteArray(Charsets.UTF_8))
                    } ?: error("Couldn't save the file.")
                }
            }
            _message.value = error ?: "Export complete"
        }
    }

    private val _filters = MutableStateFlow(ReportFilters())
    val filters: StateFlow<ReportFilters> = _filters.asStateFlow()

    init {
        reports.refresh()
        realtimeRepository.tableChanges(Tables.REPORTS)
            .onEach { reports.refresh(showIndicator = false) }
            .catch { /* best-effort */ }
            .launchIn(viewModelScope)
    }

    fun updateFilters(change: (ReportFilters) -> ReportFilters) = _filters.update(change)

    fun isUpdated(report: ReportDto): Boolean =
        !allReports && localStore.isReportUpdated(report.id, report.updatedAt, report.createdAt)

    fun apply(list: List<ReportDto>, f: ReportFilters): List<ReportDto> {
        val q = f.query.trim()
        return list
            .filter { r ->
                (q.isEmpty() || r.title.contains(q, true) || r.description.contains(q, true) ||
                    r.address.orEmpty().contains(q, true) || r.referenceNumber.orEmpty().contains(q, true)) &&
                    (f.dateRange.days == null || Dates.isWithinDays(r.createdAt, f.dateRange.days)) &&
                    (f.category == null || r.category == f.category) &&
                    (f.status == null || r.status == f.status) &&
                    (!f.assignedToMe || r.assignedTo == currentUserId) &&
                    (f.barangayId == null || r.barangayId == f.barangayId)
            }
            .let { filtered ->
                when (f.sort) {
                    ReportSort.NEWEST -> filtered.sortedByDescending { Dates.toMillis(it.createdAt) }
                    ReportSort.OLDEST -> filtered.sortedBy { Dates.toMillis(it.createdAt) }
                    ReportSort.UPDATED -> filtered.sortedByDescending { Dates.toMillis(it.updatedAt) }
                }
            }
    }

    /** Timestamp used for the Today / Yesterday / ... headers. */
    fun groupKey(report: ReportDto, sort: ReportSort): String =
        if (sort == ReportSort.UPDATED) report.updatedAt else report.createdAt

    companion object {
        fun factory(allReports: Boolean, currentUserId: String) = appViewModelFactory {
            ReportListViewModel(
                this[APPLICATION_KEY] as Application,
                it.reportRepository,
                it.directoryRepository,
                it.realtimeRepository,
                it.localStore,
                it.outbox,
                allReports,
                currentUserId
            )
        }
    }
}

fun Dates.Bucket.label(): String = when (this) {
    Dates.Bucket.TODAY -> "Today"
    Dates.Bucket.YESTERDAY -> "Yesterday"
    Dates.Bucket.THIS_WEEK -> "This week"
    Dates.Bucket.EARLIER -> "Earlier"
}

/** Quote a CSV cell when needed (commas, quotes, new lines). */
private fun csvCell(value: String): String =
    if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + value.replace("\"", "\"\"") + "\"" else value
