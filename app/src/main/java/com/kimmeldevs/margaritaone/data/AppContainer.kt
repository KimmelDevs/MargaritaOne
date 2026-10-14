package com.kimmeldevs.margaritaone.data

import android.content.Context
import com.kimmeldevs.margaritaone.util.LocalStore
import com.kimmeldevs.margaritaone.data.remote.SupabaseProvider
import com.kimmeldevs.margaritaone.data.repository.AuthRepository
import com.kimmeldevs.margaritaone.data.repository.DirectoryRepository
import com.kimmeldevs.margaritaone.data.repository.EmergencyContactRepository
import com.kimmeldevs.margaritaone.data.repository.PostRepository
import com.kimmeldevs.margaritaone.data.repository.ProfileRepository
import com.kimmeldevs.margaritaone.data.repository.RealtimeRepository
import com.kimmeldevs.margaritaone.data.repository.ReportRepository

/** Simple manual DI: one instance of each repository for the whole app. */
class AppContainer(context: Context) {
    val localStore by lazy { LocalStore(context) }
    val outbox by lazy { Outbox(context) }
    val authRepository by lazy { AuthRepository { SupabaseProvider.client } }
    val profileRepository by lazy { ProfileRepository { SupabaseProvider.client } }
    val reportRepository by lazy { ReportRepository { SupabaseProvider.client } }
    val postRepository by lazy { PostRepository { SupabaseProvider.client } }
    val emergencyContactRepository by lazy { EmergencyContactRepository { SupabaseProvider.client } }
    val directoryRepository by lazy { DirectoryRepository({ SupabaseProvider.client }, localStore.offlineCache("barangays")) }
    val realtimeRepository by lazy { RealtimeRepository { SupabaseProvider.client } }
}
