package com.kimmeldevs.margaritaone.push

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kimmeldevs.margaritaone.MainActivity
import com.kimmeldevs.margaritaone.MargaritaOneApp
import com.kimmeldevs.margaritaone.R
import com.kimmeldevs.margaritaone.data.remote.SupabaseProvider
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives pushes. The server sends data-only messages (title, body, channel, report_id / post_id) so the app
 * always builds the notification itself, in the foreground and background alike.
 */
class PushService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** The full-colour mascot shown inside each notification (the status-bar icon must be a plain silhouette). */
    private val largeIcon: Bitmap? by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.mascot)?.let { Bitmap.createScaledBitmap(it, 192, 192, true) }
    }

    override fun onNewToken(token: String) {
        if (!SupabaseProvider.isConfigured) return
        val repository = (application as MargaritaOneApp).container.pushRepository
        scope.launch {
            // Only signed-in users have tokens on the server; otherwise this runs again after sign-in.
            if (SupabaseProvider.client.auth.currentUserOrNull() != null) runCatching { repository.register(token) }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val title = data["title"] ?: message.notification?.title ?: return
        val body = data["body"] ?: message.notification?.body.orEmpty()
        val channel = data["channel"] ?: Push.CHANNEL_BULLETIN

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val open = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            data["report_id"]?.let { putExtra(PushTarget.EXTRA_REPORT_ID, it) }
            data["post_id"]?.let { putExtra(PushTarget.EXTRA_POST_ID, it) }
        }
        val id = (data["report_id"] ?: data["post_id"] ?: title).hashCode()
        val pending = PendingIntent.getActivity(
            this, id, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(largeIcon)
            .setColor(ContextCompat.getColor(this, R.color.brand_red))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(if (channel == Push.CHANNEL_EMERGENCY) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        NotificationManagerCompat.from(this).notify(id, notification)
    }
}
