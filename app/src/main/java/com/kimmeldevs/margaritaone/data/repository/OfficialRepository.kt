package com.kimmeldevs.margaritaone.data.repository

import com.kimmeldevs.margaritaone.data.model.Official
import com.kimmeldevs.margaritaone.data.model.OfficialInput
import com.kimmeldevs.margaritaone.data.remote.Buckets
import com.kimmeldevs.margaritaone.data.remote.Tables
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import java.util.UUID

/** Municipal officials (About screen). Readable by everyone, even signed out; writes are admin-only via RLS. */
class OfficialRepository(private val client: () -> SupabaseClient) {

    suspend fun getOfficials(): List<Official> =
        client().from(Tables.OFFICIALS).select {
            order("sort_order", Order.ASCENDING)
            order("name", Order.ASCENDING)
        }.decodeList()

    suspend fun add(input: OfficialInput) {
        client().from(Tables.OFFICIALS).insert(input)
    }

    suspend fun update(id: String, input: OfficialInput) {
        client().from(Tables.OFFICIALS).update(input) { filter { eq("id", id) } }
    }

    suspend fun delete(official: Official) {
        client().from(Tables.OFFICIALS).delete { filter { eq("id", official.id) } }
        official.photoPath?.let { deletePhoto(it) }
    }

    /** Admin only (storage policy). Returns the object path to save in officials.photo_path. */
    suspend fun uploadPhoto(jpeg: ByteArray): String {
        val path = "officials/${UUID.randomUUID()}.jpg"
        client().storage.from(Buckets.POST_ATTACHMENTS).upload(path, jpeg) { contentType = ContentType.Image.JPEG }
        return path
    }

    /** Best-effort cleanup of a replaced or removed photo. */
    suspend fun deletePhoto(path: String) {
        runCatching { client().storage.from(Buckets.POST_ATTACHMENTS).delete(path) }
    }

    fun photoUrl(path: String): String = client().storage.from(Buckets.POST_ATTACHMENTS).publicUrl(path)
}
