package dev.aiaerial.signal

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.aiaerial.signal.data.retention.DataRetentionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SignalApplication : Application() {

    @Inject lateinit var retentionManager: DataRetentionManager

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch { retentionManager.cleanup() }
    }
}
