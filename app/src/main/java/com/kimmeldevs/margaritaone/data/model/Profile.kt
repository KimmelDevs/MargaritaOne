package com.kimmeldevs.margaritaone.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class UserRole(val label: String) {
    @SerialName("citizen") CITIZEN("Citizen"),
    @SerialName("staff") STAFF("Staff"),
    @SerialName("admin") ADMIN("Admin");

    val isStaff: Boolean get() = this == STAFF || this == ADMIN
}

@Serializable
data class Profile(
    val id: String,
    @SerialName("full_name") val fullName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val barangay: String? = null,
    val role: UserRole = UserRole.CITIZEN,
    @SerialName("created_at") val createdAt: String? = null
) {
    val displayName: String get() = fullName?.takeIf { it.isNotBlank() } ?: email ?: "Unnamed user"
}

/** The only profile columns a user may change themselves (see column grants in the SQL). */
@Serializable
data class ProfileUpdate(
    @SerialName("full_name") val fullName: String,
    val phone: String?,
    val barangay: String?
)
