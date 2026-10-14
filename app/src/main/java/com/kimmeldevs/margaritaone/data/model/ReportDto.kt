package com.kimmeldevs.margaritaone.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ReportStatus(val label: String) {
    @SerialName("pending") PENDING("Pending"),
    @SerialName("in_progress") IN_PROGRESS("In progress"),
    @SerialName("resolved") RESOLVED("Resolved"),
    @SerialName("rejected") REJECTED("Rejected"),
    @SerialName("cancelled") CANCELLED("Cancelled");

    /** Statuses staff can set (cancelling is the reporter's choice only). */
    val isStaffSettable: Boolean get() = this != CANCELLED
}

@Serializable
enum class ReportCategory(val label: String) {
    @SerialName("road") ROAD("Road and infrastructure"),
    @SerialName("garbage") GARBAGE("Garbage collection"),
    @SerialName("streetlight") STREETLIGHT("Streetlight"),
    @SerialName("peace_order") PEACE_ORDER("Peace and order"),
    @SerialName("other") OTHER("Other")
}

/** A row of the `reports` table. */
@Serializable
data class ReportDto(
    val id: String,
    @SerialName("reporter_id") val reporterId: String,
    val title: String,
    val description: String = "",
    val category: ReportCategory,
    val status: ReportStatus = ReportStatus.PENDING,
    @SerialName("photo_path") val photoPath: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
    @SerialName("assigned_to") val assignedTo: String? = null,
    @SerialName("ref_no") val refNo: Long? = null,
    @SerialName("barangay_id") val barangayId: String? = null,
    /** 1 = "fixed", -1 = "not fixed"; set by the reporter after it's resolved. */
    @SerialName("resolution_rating") val resolutionRating: Int? = null,
    @SerialName("resolution_feedback") val resolutionFeedback: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
) {
    /** e.g. MG-2026-0042, for quoting when following up with the LGU. */
    val referenceNumber: String?
        get() = refNo?.let { "MG-${createdAt.take(4)}-${it.toString().padStart(4, '0')}" }
}

/** Columns a citizen may set when submitting. Everything else is filled in by the database. */
@Serializable
data class NewReport(
    val title: String,
    val description: String,
    val category: ReportCategory,
    @SerialName("photo_path") val photoPath: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
    @SerialName("barangay_id") val barangayId: String? = null
)

/** A row of the `report_updates` table (the report timeline). */
@Serializable
data class ReportUpdateDto(
    val id: String,
    @SerialName("report_id") val reportId: String,
    @SerialName("author_id") val authorId: String? = null,
    val status: ReportStatus? = null,
    val note: String? = null,
    @SerialName("created_at") val createdAt: String,
    /** Who made the update, copied by a database trigger so residents can see it too. */
    @SerialName("author_name") val authorName: String? = null,
    @SerialName("author_role") val authorRole: UserRole? = null
)

/** A row of `report_views`: a staff member or admin who opened the report ("Seen by"; staff/admins only). */
@Serializable
data class ReportView(
    @SerialName("report_id") val reportId: String,
    @SerialName("viewer_id") val viewerId: String,
    @SerialName("viewer_name") val viewerName: String,
    @SerialName("viewer_role") val viewerRole: UserRole,
    @SerialName("first_seen_at") val firstSeenAt: String,
    @SerialName("last_seen_at") val lastSeenAt: String
)

@Serializable
data class NewReportUpdate(
    @SerialName("report_id") val reportId: String,
    val status: ReportStatus? = null,
    val note: String? = null
)
