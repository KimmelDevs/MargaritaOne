package com.kimmeldevs.margaritaone.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Formats the ISO-8601 timestamps Supabase returns, in the device's time zone. */
@OptIn(ExperimentalTime::class)
object Dates {

    private const val DAY_MS = 24L * 60 * 60 * 1000

    fun toMillis(iso: String): Long? =
        runCatching { Instant.parse(iso).toEpochMilliseconds() }.getOrNull()

    fun isoHoursAgo(hours: Long): String =
        Instant.fromEpochMilliseconds(System.currentTimeMillis() - hours * 3_600_000).toString()

    fun isoFromMillis(millis: Long): String = Instant.fromEpochMilliseconds(millis).toString()

    fun date(iso: String): String = format(iso, "MMM d, yyyy")

    /** e.g. "Sat, Oct 12, 2026 8:00 AM" for pickers and event times. */
    fun formatMillis(millis: Long, pattern: String = "EEE, MMM d, yyyy h:mm a"): String =
        SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))

    fun dateTime(iso: String): String = format(iso, "MMM d, yyyy h:mm a")

    fun isWithinHours(iso: String, hours: Long): Boolean =
        (toMillis(iso) ?: 0L) >= System.currentTimeMillis() - hours * 3_600_000

    fun isWithinDays(iso: String, days: Int): Boolean =
        (toMillis(iso) ?: 0L) >= System.currentTimeMillis() - days * DAY_MS

    enum class Bucket { TODAY, YESTERDAY, THIS_WEEK, EARLIER }

    /** Which list section a timestamp falls into, relative to local midnight. */
    fun bucket(iso: String, now: Long = System.currentTimeMillis()): Bucket {
        val millis = toMillis(iso) ?: return Bucket.EARLIER
        val startOfToday = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return when {
            millis >= startOfToday -> Bucket.TODAY
            millis >= startOfToday - DAY_MS -> Bucket.YESTERDAY
            millis >= startOfToday - 6 * DAY_MS -> Bucket.THIS_WEEK
            else -> Bucket.EARLIER
        }
    }

    private fun format(iso: String, pattern: String): String {
        val millis = toMillis(iso) ?: return iso
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))
    }
}
