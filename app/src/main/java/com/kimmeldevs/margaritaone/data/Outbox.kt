package com.kimmeldevs.margaritaone.data

import android.content.Context
import androidx.core.content.edit
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kimmeldevs.margaritaone.MargaritaOneApp
import com.kimmeldevs.margaritaone.data.model.NewReport
import com.kimmeldevs.margaritaone.data.remote.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.exceptions.HttpRequestException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

/** A report written without signal, waiting to be sent (#10). */
@Serializable
data class PendingReport(
    val localId: String,
    val report: NewReport,
    /** Compressed JPEG copied into app storage, so it survives the gallery photo being deleted. */
    val photoFile: String? = null,
    val savedAt: Long,
    /** Set when the server rejected it (not a connection problem); shown so the user can decide. */
    val lastError: String? = null
)

/** True for "no internet / server unreachable" failures, which are worth retrying later. */
fun Throwable.isNetworkError(): Boolean =
    this is HttpRequestException || this is IOException || cause is IOException

/**
 * On-device queue of reports to send when the phone is back online. WorkManager does the sending,
 * so it happens even if the app is closed.
 */
class Outbox(private val context: Context) {

    private val prefs = context.getSharedPreferences("margaritaone_outbox", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val dir = File(context.filesDir, "outbox").apply { mkdirs() }

    private val _pending = MutableStateFlow(read())
    val pending: StateFlow<List<PendingReport>> = _pending.asStateFlow()

    fun add(report: NewReport, jpegBytes: ByteArray?) {
        val id = UUID.randomUUID().toString()
        val photo = jpegBytes?.let { bytes -> File(dir, "$id.jpg").apply { writeBytes(bytes) }.absolutePath }
        write(_pending.value + PendingReport(id, report, photo, System.currentTimeMillis()))
        scheduleSend()
    }

    fun remove(localId: String) {
        _pending.value.firstOrNull { it.localId == localId }?.photoFile?.let { File(it).delete() }
        write(_pending.value.filterNot { it.localId == localId })
    }

    fun markFailed(localId: String, error: String) =
        write(_pending.value.map { if (it.localId == localId) it.copy(lastError = error) else it })

    /** Queue a send for when there's a connection (no-op if one is already queued). */
    fun scheduleSend(replace: Boolean = false) {
        val request = OneTimeWorkRequestBuilder<OutboxWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            request
        )
    }

    /** "Send now": clear earlier rejections and try again right away. */
    fun retryAll() {
        write(_pending.value.map { it.copy(lastError = null) })
        scheduleSend(replace = true)
    }

    private fun read(): List<PendingReport> =
        prefs.getString(KEY, null)?.let { runCatching { json.decodeFromString<List<PendingReport>>(it) }.getOrNull() }.orEmpty()

    private fun write(list: List<PendingReport>) {
        prefs.edit { putString(KEY, json.encodeToString(list)) }
        _pending.value = list
    }

    private companion object {
        const val KEY = "pending_reports"
        const val WORK_NAME = "send-outbox"
    }
}

/** Sends every queued report that hasn't been rejected. Retries later on connection problems. */
class OutboxWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!SupabaseProvider.isConfigured) return Result.failure()
        val container = (applicationContext as MargaritaOneApp).container
        val outbox = container.outbox
        // Wait for the saved login to load; without it the database would refuse the report.
        runCatching { SupabaseProvider.client.auth.awaitInitialization() }
        if (SupabaseProvider.client.auth.currentUserOrNull() == null) return Result.success()

        for (item in outbox.pending.value.filter { it.lastError == null }) {
            try {
                val photoPath = item.photoFile?.let { File(it) }?.takeIf { it.exists() }?.let {
                    container.reportRepository.uploadPhoto(it.readBytes())
                }
                container.reportRepository.submitReport(item.report.copy(photoPath = photoPath))
                outbox.remove(item.localId)
            } catch (e: Exception) {
                if (e.isNetworkError()) return Result.retry()
                outbox.markFailed(item.localId, e.message ?: "Couldn't send")
            }
        }
        return Result.success()
    }
}
