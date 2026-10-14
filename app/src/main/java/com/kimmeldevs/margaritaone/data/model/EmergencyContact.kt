package com.kimmeldevs.margaritaone.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A row of `hotline_categories`; admins manage these. [icon] is a key from [HotlineIcon]. */
@Serializable
data class HotlineCategory(
    val id: String,
    val name: String,
    val icon: String = HotlineIcon.PHONE.key,
    @SerialName("is_urgent") val isUrgent: Boolean = false,
    @SerialName("sort_order") val sortOrder: Int = 100
)

@Serializable
data class HotlineCategoryInput(
    val name: String,
    val icon: String,
    @SerialName("is_urgent") val isUrgent: Boolean,
    @SerialName("sort_order") val sortOrder: Int
)

/** The icons an admin can pick for a category. Keys are stored in the database. */
enum class HotlineIcon(val key: String) {
    WARNING("warning"),
    FIRE("fire"),
    POLICE("police"),
    MEDICAL("medical"),
    AMBULANCE("ambulance"),
    FLOOD("flood"),
    POWER("power"),
    WATER("water"),
    RESCUE("rescue"),
    GOVERNMENT("government"),
    SCHOOL("school"),
    PHONE("phone");

    companion object {
        fun fromKey(key: String?): HotlineIcon = entries.firstOrNull { it.key == key } ?: PHONE
    }
}

/** A row of the `emergency_contacts` table. */
@Serializable
data class EmergencyContact(
    val id: String,
    val name: String,
    @SerialName("category_id") val categoryId: String? = null,
    val phone: String,
    val note: String? = null,
    @SerialName("sort_order") val sortOrder: Int = 100
)

/** Columns an admin may set (insert or update). */
@Serializable
data class EmergencyContactInput(
    val name: String,
    @SerialName("category_id") val categoryId: String?,
    val phone: String,
    val note: String?,
    @SerialName("sort_order") val sortOrder: Int
)
