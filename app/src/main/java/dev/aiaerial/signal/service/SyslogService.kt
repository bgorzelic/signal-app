package dev.aiaerial.signal.service

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.aiaerial.signal.MainActivity
import dev.aiaerial.signal.R
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
        private const val TAG = "SyslogService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "syslog_receiver"
        private const val FLUSH_TIMEOUT_MS = 2000L
    }

    private var receiver: SyslogReceiver? = null

    // Service-scoped coroutine scope — tied to service lifecycle.
    // SupervisorJob ensures a single child failure doesn't cancel the whole scope.
    // Cancelled in onDestroy() after flushing pending events.
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.IO)

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
            val rcv = SyslogReceiver(port = port)
            receiver = rcv
            eventPipeline.bindScope(serviceScope)
            startForeground(port)
            serviceScope.launch { rcv.start(serviceScope) }
            serviceScope.launch {
                rcv.messages.collect { msg ->
                    try {
                        eventPipeline.processSyslogMessage(msg)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to process message", e)
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
            .setSmallIcon(R.drawable.ic_notification)
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
        eventPipeline.unbindScope()
        // Flush pending events before cancelling the scope. We use
        // runBlocking with a timeout to guarantee the flush completes
        // (or is abandoned after FLUSH_TIMEOUT_MS) before the scope
        // is cancelled. The Dispatchers.IO context ensures the flush
        // runs on a worker thread rather than blocking the main looper
        // for more than the withTimeout duration (which is near-zero
        // if the flush itself is fast).
        try {
            runBlocking(Dispatchers.IO) {
                withTimeout(FLUSH_TIMEOUT_MS) {
                    eventPipeline.flush()
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Event flush timed out during onDestroy — some events may be lost")
        } catch (e: Exception) {
            Log.e(TAG, "Event flush failed during onDestroy", e)
        }
        serviceScope.cancel()
        super.onDestroy()
    }
}
