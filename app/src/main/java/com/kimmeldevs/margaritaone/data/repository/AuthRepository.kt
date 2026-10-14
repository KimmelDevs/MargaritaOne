package com.kimmeldevs.margaritaone.data.repository

import io.github.jan.supabase.SupabaseClient
import com.kimmeldevs.margaritaone.data.remote.Buckets
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AuthRepository(private val client: () -> SupabaseClient) {

    val sessionStatus: Flow<SessionStatus> get() = client().auth.sessionStatus

    fun currentUserId(): String? = client().auth.currentUserOrNull()?.id

    /**
     * Role is never sent here; the database trigger creates the profile as citizen.
     * Returns true when Supabase requires email confirmation before the user can sign in.
     */
    suspend fun signUp(email: String, password: String, fullName: String): Boolean {
        client().auth.signUpWith(Email) {
            this.email = email
            this.password = password
            data = buildJsonObject { put("full_name", fullName) }
        }
        return client().auth.currentSessionOrNull() == null
    }

    suspend fun signIn(email: String, password: String) {
        client().auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    /** Always ends the session on this phone, even with no signal (Supabase would otherwise keep it on a network error). */
    suspend fun signOut() {
        try {
            client().auth.signOut()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            client().auth.clearSession()
        }
    }

    /** Emails a password-reset code (the Supabase "Reset password" template must include {{ .Token }}). */
    suspend fun sendPasswordReset(email: String) {
        client().auth.resetPasswordForEmail(email)
    }

    /** Verifies the emailed code (this signs the user in), then sets the new password. */
    suspend fun resetPassword(email: String, code: String, newPassword: String) {
        client().auth.verifyEmailOtp(type = OtpType.Email.RECOVERY, email = email, token = code)
        client().auth.updateUser { password = newPassword }
    }

    /**
     * Removes the user's report photos from storage, then deletes the account in the database
     * (profile, reports, reactions, and comments cascade). Admins are refused by the database;
     * callers check the role first so an admin's photos aren't removed before that refusal.
     */
    suspend fun deleteAccount() {
        val userId = requireNotNull(currentUserId()) { "Not signed in" }
        val bucket = client().storage.from(Buckets.REPORT_PHOTOS)
        val photos = runCatching { bucket.list(userId) }.getOrDefault(emptyList()).map { "$userId/${it.name}" }
        if (photos.isNotEmpty()) bucket.delete(photos)
        client().postgrest.rpc("delete_my_account")
        // The account no longer exists; just clear the local session.
        runCatching { client().auth.signOut() }
    }
}
