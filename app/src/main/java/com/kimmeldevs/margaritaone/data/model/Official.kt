package com.kimmeldevs.margaritaone.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A row of `officials`: mayor, vice mayor and councilors shown on the About screen. Admins edit them. */
@Serializable
data class Official(
    val id: String,
    val name: String,
    /** English title, also the translation key: "Mayor", "Vice Mayor", "Councilor"... */
    val position: String,
    @SerialName("photo_path") val photoPath: String? = null,
    @SerialName("sort_order") val sortOrder: Int = 100
)

/** The columns an admin may set. */
@Serializable
data class OfficialInput(
    val name: String,
    val position: String,
    @SerialName("photo_path") val photoPath: String?,
    @SerialName("sort_order") val sortOrder: Int
)
