package com.kimmeldevs.margaritaone.util

import android.content.Context
import androidx.core.content.edit
import com.kimmeldevs.margaritaone.data.model.EmergencyContact
import com.kimmeldevs.margaritaone.data.model.HotlineCategory
import com.kimmeldevs.margaritaone.data.model.Profile
import com.kimmeldevs.margaritaone.i18n.AppLanguage
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.io.File

/**
 * Small on-device memory (SharedPreferences): which report versions the user has already
 * looked at, and which emergency banners they dismissed. Not synced; fine to lose.
 * Also holds the offline copies of screens (JSON files in filesDir/offline).
 */
class LocalStore(context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private val prefs = context.getSharedPreferences("margaritaone_local", Context.MODE_PRIVATE)

    fun markReportSeen(reportId: String, updatedAt: String) =
        prefs.edit { putString("seen_$reportId", updatedAt) }

    /** True when the report changed since the user last opened it. Unopened reports count as changed once staff touch them. */
    fun isReportUpdated(reportId: String, updatedAt: String, createdAt: String): Boolean {
        val seen = prefs.getString("seen_$reportId", null) ?: return updatedAt != createdAt
        return seen != updatedAt
    }

    var language: AppLanguage
        get() = AppLanguage.fromCode(prefs.getString("language", null))
        set(value) = prefs.edit { putString("language", value.code) }

    var themeMode: ThemeMode
        get() = ThemeMode.entries.firstOrNull { it.name == prefs.getString("theme_mode", null) } ?: ThemeMode.SYSTEM
        set(value) = prefs.edit { putString("theme_mode", value.name) }

    /** Last downloaded hotlines, so they still show with no signal. */
    var cachedContacts: List<EmergencyContact>
        get() = prefs.getString("emergency_contacts", null)
            ?.let { runCatching { json.decodeFromString<List<EmergencyContact>>(it) }.getOrNull() }
            .orEmpty()
        set(value) = prefs.edit { putString("emergency_contacts", json.encodeToString(value)) }

    var cachedHotlineCategories: List<HotlineCategory>
        get() = prefs.getString("hotline_categories", null)
            ?.let { runCatching { json.decodeFromString<List<HotlineCategory>>(it) }.getOrNull() }
            .orEmpty()
        set(value) = prefs.edit { putString("hotline_categories", json.encodeToString(value)) }

    /** The signed-in user's profile, so the app can open with no signal. Cleared on sign-out. */
    var cachedProfile: Profile?
        get() = prefs.getString("profile", null)
            ?.let { runCatching { json.decodeFromString<Profile>(it) }.getOrNull() }
        set(value) = prefs.edit {
            if (value == null) remove("profile") else putString("profile", json.encodeToString(value))
        }

    private val offlineDir = File(context.filesDir, "offline")

    /** A saved copy of one screen's data (bulletin, reports...). Cleared on sign-out with [clearAccountData]. */
    fun <T> offlineCache(key: String, serializer: KSerializer<T>): OfflineCache<T> =
        OfflineCache(File(offlineDir, "$key.json"), serializer, json)

    inline fun <reified T> offlineCache(key: String): OfflineCache<T> = offlineCache(key, serializer<T>())

    /** Removes everything saved for the signed-in user, so the next person on this phone doesn't see it. */
    fun clearAccountData() {
        cachedProfile = null
        offlineDir.deleteRecursively()
    }

    /** The notification permission is asked once after the first sign-in; after that it's in system settings. */
    var askedNotificationPermission: Boolean
        get() = prefs.getBoolean("asked_notifications", false)
        set(value) = prefs.edit { putBoolean("asked_notifications", value) }

    fun dismissEmergency(postId: String) = prefs.edit { putString("dismissed_emergency", postId) }

    fun isEmergencyDismissed(postId: String): Boolean = prefs.getString("dismissed_emergency", null) == postId
}

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}
