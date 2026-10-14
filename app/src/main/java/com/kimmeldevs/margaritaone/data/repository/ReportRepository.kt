package com.kimmeldevs.margaritaone.data.repository

import com.kimmeldevs.margaritaone.data.model.NewReport
import com.kimmeldevs.margaritaone.data.model.NewReportUpdate
import com.kimmeldevs.margaritaone.data.model.ReportDto
import com.kimmeldevs.margaritaone.data.model.ReportStatus
import com.kimmeldevs.margaritaone.data.model.ReportUpdateDto
import com.kimmeldevs.margaritaone.data.model.ReportView
import com.kimmeldevs.margaritaone.data.remote.Buckets
import com.kimmeldevs.margaritaone.data.remote.Tables
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID
import kotlin.time.Duration.Companion.hours

class ReportRepository(private val client: () -> SupabaseClient) {

    /** RLS limits citizens to their own reports; staff get all of them. */
    suspend fun getReports(status: ReportStatus? = null): List<ReportDto> =
        client().from(Tables.REPORTS).select {
            if (status != null) filter { eq("status", status) }
            order("created_at", Order.DESCENDING)
        }.decodeList()

    suspend fun getMyReports(): List<ReportDto> {
        val userId = client().auth.currentUserOrNull()?.id ?: return emptyList()
        return client().from(Tables.REPORTS).select {
            filter { eq("reporter_id", userId) }
            order("created_at", Order.DESCENDING)
        }.decodeList()
    }

    suspend fun getReport(id: String): ReportDto? =
        client().from(Tables.REPORTS)
            .select { filter { eq("id", id) } }
            .decodeSingleOrNull()

    suspend fun getTimeline(reportId: String): List<ReportUpdateDto> =
        client().from(Tables.REPORT_UPDATES).select {
            filter { eq("report_id", reportId) }
            order("created_at", Order.ASCENDING)
        }.decodeList()

    suspend fun submitReport(report: NewReport): ReportDto =
        client().from(Tables.REPORTS)
            .insert(report) { select() }
            .decodeSingle()

    /** Staff only (enforced by RLS). A status here also updates reports.status via trigger. */
    suspend fun addUpdate(update: NewReportUpdate) {
        client().from(Tables.REPORT_UPDATES).insert(update)
    }

    /** Uploads a JPEG to "<user id>/<uuid>.jpg" as the storage policy requires; returns the object path. */
    suspend fun uploadPhoto(jpegBytes: ByteArray): String {
        val userId = requireNotNull(client().auth.currentUserOrNull()?.id) { "Not signed in" }
        val path = "$userId/${UUID.randomUUID()}.jpg"
        client().storage.from(Buckets.REPORT_PHOTOS).upload(path, jpegBytes) {
            contentType = ContentType.Image.JPEG
        }
        return path
    }

    /** Citizen withdraws their own pending report (checked inside the database function). */
    suspend fun cancelReport(reportId: String) {
        client().postgrest.rpc("cancel_my_report", buildJsonObject { put("target_report", reportId) })
    }

    /** Reporter rates their own resolved report: 1 = fixed, -1 = not fixed. */
    suspend fun rateResolution(reportId: String, rating: Int, feedback: String?) {
        client().postgrest.rpc(
            "rate_resolution",
            buildJsonObject {
                put("target_report", reportId)
                put("rating", rating)
                put("feedback", feedback)
            }
        )
    }

    /** Staff only (RLS). Pass null to unassign. */
    suspend fun assign(reportId: String, staffId: String?) {
        client().from(Tables.REPORTS).update(
            buildJsonObject { put("assigned_to", staffId) }
        ) { filter { eq("id", reportId) } }
    }

    /** Signed URLs for several photos in one request; map of object path to URL. */
    /** Staff/admins only (the database ignores it for residents): records that they opened the report. */
    suspend fun markSeen(reportId: String) {
        client().postgrest.rpc("mark_report_seen", buildJsonObject { put("target_report", reportId) })
    }

    /** Staff and admins who opened the report, earliest first. Readable by staff/admins only (RLS). */
    suspend fun getViews(reportId: String): List<ReportView> =
        client().from(Tables.REPORT_VIEWS).select {
            filter { eq("report_id", reportId) }
            order("first_seen_at", Order.ASCENDING)
        }.decodeList()

    /** Which of these reports at least one staff member or admin has opened. */
    suspend fun seenReportIds(reportIds: List<String>): Set<String> =
        reportIds.chunked(100).flatMap { chunk ->
            client().from(Tables.REPORT_VIEWS).select(Columns.list("report_id")) {
                filter { isIn("report_id", chunk) }
            }.decodeList<SeenRow>().map { it.reportId }
        }.toSet()

    @Serializable
    private data class SeenRow(@SerialName("report_id") val reportId: String)

    suspend fun photoUrls(paths: List<String>): Map<String, String> {
        if (paths.isEmpty()) return emptyMap()
        return client().storage.from(Buckets.REPORT_PHOTOS)
            .createSignedUrls(1.hours, paths)
            .associate { it.path to it.signedURL }
    }

    suspend fun photoUrl(path: String): String =
        client().storage.from(Buckets.REPORT_PHOTOS).createSignedUrl(path, expiresIn = 1.hours)
}
