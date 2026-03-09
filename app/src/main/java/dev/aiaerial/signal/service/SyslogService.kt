package dev.aiaerial.signal.service

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.aiaerial.signal.MainActivity
import dev.aiaerial.signal.R
import dev.aiaerial.signal.data.syslog.SyslogMessage
import dev.aiaerial.signal.data.syslog.SyslogReceiver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject

@AndroidEntryPoint
class SyslogService : Service() {

    private val receiver = SyslogReceiver(port = 1514)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val messages: SharedFlow<SyslogMessage> get() = receiver.messages

    inner class LocalBinder : Binder() {
        val service: SyslogService get() = this@SyslogService
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground()
        scope.launch { receiver.start(scope) }
        return START_STICKY
    }

    private fun startForeground() {
        val channelId = "syslog_receiver"
        val channel = NotificationChannel(
            channelId, "Syslog Receiver",
            NotificationManager.IMPORTANCE_LOW
        )
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)

        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("SIGNAL Syslog Receiver")
            .setContentText("Listening on UDP port 1514")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }
    }

    override fun onDestroy() {
        receiver.stop()
        scope.cancel()
        super.onDestroy()
    }
}
