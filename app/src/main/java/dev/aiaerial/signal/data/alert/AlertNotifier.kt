package dev.aiaerial.signal.data.alert

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.aiaerial.signal.MainActivity
import dev.aiaerial.signal.R
import javax.inject.Inject
import javax.inject.Singleton

private const val CHANNEL_ID = "signal_alerts"
private const val CHANNEL_NAME = "SIGNAL Alerts"
private const val MIN_NOTIFY_INTERVAL_MS = 60_000L

/**
 * Posts Android notifications for WARNING and CRITICAL alerts.
 *
 * Spam-prevention: tracks the last notification time per [AlertType] and
 * suppresses subsequent firings within [MIN_NOTIFY_INTERVAL_MS] (60 s).
 */
@Singleton
class AlertNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val notificationManager = NotificationManagerCompat.from(context)
    private val lastNotifiedAt = mutableMapOf<AlertType, Long>()
    private var channelCreated = false
    // Each AlertType gets its own stable notification ID so updates replace
    // the previous notification for that type rather than stacking.
    private val notificationIdMap = AlertType.entries.associateWith { it.ordinal + 1 }

    fun notify(alert: Alert) {
        if (alert.severity == AlertSeverity.INFO) return

        val now = System.currentTimeMillis()
        val last = lastNotifiedAt[alert.type] ?: 0L
        if (now - last < MIN_NOTIFY_INTERVAL_MS) return

        ensureChannelCreated()

        if (!hasNotificationPermission()) return

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            alert.type.ordinal,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(alert.title)
            .setContentText(alert.detail)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alert.detail))
            .setPriority(
                if (alert.severity == AlertSeverity.CRITICAL) {
                    NotificationCompat.PRIORITY_HIGH
                } else {
                    NotificationCompat.PRIORITY_DEFAULT
                }
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notifId = notificationIdMap[alert.type] ?: alert.type.ordinal + 1
        notificationManager.notify(notifId, notification)
        lastNotifiedAt[alert.type] = now
    }

    private fun ensureChannelCreated() {
        if (channelCreated) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Alerts for weak signal, roam churn, and auth failures"
        }
        val systemManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        systemManager.createNotificationChannel(channel)
        channelCreated = true
    }

    private fun hasNotificationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }
}
