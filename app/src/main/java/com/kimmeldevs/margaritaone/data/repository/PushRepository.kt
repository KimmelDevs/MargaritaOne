package com.kimmeldevs.margaritaone.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** This phone's push token on the server (`device_tokens`, written only through these RPCs). */
class PushRepository(private val client: () -> SupabaseClient) {

    /** Links the token to the signed-in user (moves it if another account used this phone before). */
    suspend fun register(token: String) {
        client().postgrest.rpc("register_device_token", buildJsonObject {
            put("p_token", token)
            put("p_platform", "android")
        })
    }

    suspend fun unregister(token: String) {
        client().postgrest.rpc("unregister_device_token", buildJsonObject { put("p_token", token) })
    }
}
