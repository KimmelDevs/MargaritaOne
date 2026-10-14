package com.kimmeldevs.margaritaone.data.repository

import com.kimmeldevs.margaritaone.data.model.Profile
import com.kimmeldevs.margaritaone.data.model.ProfileUpdate
import com.kimmeldevs.margaritaone.data.model.UserRole
import com.kimmeldevs.margaritaone.data.remote.Tables
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ProfileRepository(private val client: () -> SupabaseClient) {

    suspend fun getProfile(id: String): Profile? =
        client().from(Tables.PROFILES)
            .select { filter { eq("id", id) } }
            .decodeSingleOrNull()

    suspend fun updateProfile(id: String, update: ProfileUpdate) {
        client().from(Tables.PROFILES).update(update) { filter { eq("id", id) } }
    }

    /** Staff/admin only: RLS returns just the caller's own row for citizens. */
    suspend fun getAllProfiles(): List<Profile> =
        client().from(Tables.PROFILES).select {
            order("full_name", Order.ASCENDING)
        }.decodeList()

    /** Admin only; enforced inside the set_user_role database function. */
    suspend fun setRole(userId: String, role: UserRole) {
        client().postgrest.rpc(
            "set_user_role",
            buildJsonObject {
                put("target_user", userId)
                put("new_role", role.name.lowercase())
            }
        )
    }
}
