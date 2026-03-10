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
import dev.aiaerial.signal.data.EventPipeline
import dev.aiaerial.signal.data.prefs.SignalPreferences
import dev.aiaerial.signal.data.syslog.SyslogMessage
import dev.aiaerial.signal.data.syslog.SyslogReceiver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject

@AndroidEntryPoint
class SyslogService : Service() {

    @Inject lateinit var eventPipeline: EventPipeline
    @Inject lateinit var prefs: SignalPreferences

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "syslog_receiver"
    }

    private var receiver: SyslogReceiver? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var started = false

    val messages: SharedFlow<SyslogMessage>? get() = receiver?.messages

    inner class LocalBinder : Binder() {
        val service: SyslogService get() = this@SyslogService
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!started) {
            started = true
            val port = prefs.syslogPort
            receiver = SyslogReceiver(port = port)
            startForeground(port)
            scope.launch { receiver!!.start(scope) }
            scope.launch {
                receiver!!.messages.collect { msg ->
                    try {
                        eventPipeline.processSyslogMessage(msg, scope)
                    } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        android.util.Log.e("SyslogService", "Failed to process message", e)
                    }
                }
            }
        }
        return START_STICKY
    }

    private fun startForeground(port: Int) {
        val channel = NotificationChannel(
            CHANNEL_ID, "Syslog Receiver",
            NotificationManager.IMPORTANCE_LOW
        )
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)

        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SIGNAL Syslog Receiver")
            .setContentText("Listening on UDP port $port")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        receiver?.stop()
        kotlinx.coroutines.runBlocking { eventPipeline.flush() }
        scope.cancel()
        super.onDestroy()
    }
}
