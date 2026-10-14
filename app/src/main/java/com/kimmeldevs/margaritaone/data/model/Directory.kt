package com.kimmeldevs.margaritaone.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A row of `barangays` (admin-managed list). */
@Serializable
data class Barangay(
    val id: String,
    val name: String
)

/** A row of `audit_log` (admins only; written by database triggers). */
@Serializable
data class AuditEntry(
    val id: Long,
    @SerialName("actor_name") val actorName: String? = null,
    val action: String,
    @SerialName("target_type") val targetType: String,
    @SerialName("target_id") val targetId: String? = null,
    val summary: String,
    @SerialName("created_at") val createdAt: String
)
