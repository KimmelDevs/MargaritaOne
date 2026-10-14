package com.kimmeldevs.margaritaone.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.kimmeldevs.margaritaone.BuildConfig
import com.kimmeldevs.margaritaone.data.repository.PushRepository
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Push notifications through Firebase Cloud Messaging. The server side (who gets what) is the `send-push`
 * Supabase Edge Function, called by database triggers (migration 0009). Everything here is a no-op until the
 * FIREBASE_* values are in local.properties.
 */
object Push {

    /** Topic every signed-in user is subscribed to, for bulletin posts. */
    const val TOPIC_BULLETIN = "bulletin"

    const val CHANNEL_REPORTS = "reports"
    const val CHANNEL_BULLETIN = "bulletin"
    const val CHANNEL_EMERGENCY = "emergency"

    val isConfigured: Boolean
        get() = listOf(
            BuildConfig.FIREBASE_PROJECT_ID, BuildConfig.FIREBASE_APP_ID,
            BuildConfig.FIREBASE_API_KEY, BuildConfig.FIREBASE_SENDER_ID
        ).all { it.isNotBlank() }

    /** Called once from the Application. */
    fun init(context: Context) {
        createChannels(context)
        if (!isConfigured || FirebaseApp.getApps(context).isNotEmpty()) return
        FirebaseApp.initializeApp(
            context,
            FirebaseOptions.Builder()
                .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                .setApiKey(BuildConfig.FIREBASE_API_KEY)
                .setGcmSenderId(BuildConfig.FIREBASE_SENDER_ID)
                .build()
        )
    }

    /** After sign-in: tell the server this phone belongs to the user, and follow the bulletin. */
    suspend fun register(repository: PushRepository) {
        if (!isConfigured) return
        val messaging = FirebaseMessaging.getInstance()
        repository.register(messaging.token.await())
        messaging.subscribeToTopic(TOPIC_BULLETIN).await()
    }

    /** Before sign-out: stop sending this user's notifications to this phone. */
    suspend fun unregister(repository: PushRepository) {
        if (!isConfigured) return
        // Never hold up signing out (e.g. offline); the server drops dead tokens by itself later.
        withTimeoutOrNull(5_000) { unregisterNow(repository) }
    }

    private suspend fun unregisterNow(repository: PushRepository) {
        val messaging = FirebaseMessaging.getInstance()
        runCatching { repository.unregister(messaging.token.await()) }
        runCatching { messaging.unsubscribeFromTopic(TOPIC_BULLETIN).await() }
        // A fresh token next time, so the old one can't be reused for another account on this phone.
        runCatching { messaging.deleteToken().await() }
    }

    private fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_REPORTS, "Report updates", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Status changes on your reports, and new reports for LGU staff"
                },
                NotificationChannel(CHANNEL_BULLETIN, "Bulletin", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "New announcements, ordinances and events"
                },
                NotificationChannel(CHANNEL_EMERGENCY, "Emergency alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Emergency posts from the LGU"
                    enableVibration(true)
                }
            )
        )
    }
}
