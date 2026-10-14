package com.kimmeldevs.margaritaone.data.repository

import com.kimmeldevs.margaritaone.data.model.AuditEntry
import com.kimmeldevs.margaritaone.data.model.Barangay
import com.kimmeldevs.margaritaone.data.remote.Tables
import com.kimmeldevs.margaritaone.data.isNetworkError
import com.kimmeldevs.margaritaone.util.OfflineCache
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.CancellationException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Admin-managed reference data (barangays) and the admin activity log. */
class DirectoryRepository(
    private val client: () -> SupabaseClient,
    private val savedBarangays: OfflineCache<List<Barangay>>
) {

    /** With no signal, returns the last downloaded list (used by the barangay pickers). */
    suspend fun getBarangays(): List<Barangay> = try {
        client().from(Tables.BARANGAYS).select { order("name", Order.ASCENDING) }.decodeList<Barangay>()
            .also(savedBarangays::write)
    } catch (e: Exception) {
        if (e is CancellationException || !e.isNetworkError()) throw e
        savedBarangays.read() ?: throw e
    }

    suspend fun addBarangay(name: String) {
        client().from(Tables.BARANGAYS).insert(buildJsonObject { put("name", name) })
    }

    suspend fun renameBarangay(id: String, name: String) {
        client().from(Tables.BARANGAYS).update(buildJsonObject { put("name", name) }) { filter { eq("id", id) } }
    }

    /** Reports in it keep working; their barangay just becomes empty. */
    suspend fun deleteBarangay(id: String) {
        client().from(Tables.BARANGAYS).delete { filter { eq("id", id) } }
    }

    /** Admins only (RLS). Newest first. */
    suspend fun getAuditLog(limit: Long = 200): List<AuditEntry> =
        client().from(Tables.AUDIT_LOG).select {
            order("created_at", Order.DESCENDING)
            limit(limit)
        }.decodeList()
}
