package dev.aiaerial.signal

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import dev.aiaerial.signal.data.retention.DataRetentionManager
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SignalApplication : Application() {

    @Inject lateinit var retentionManager: DataRetentionManager

    // Application-scoped coroutine scope. The Application lives for the entire
    // process lifetime, so this scope is never cancelled — which is intentional.
    // SupervisorJob ensures a single failure doesn't cancel other children.
    // The exception handler logs failures instead of crashing silently.
    private val appScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
            Log.e("SignalApplication", "Uncaught coroutine exception", throwable)
        }
    )

    override fun onCreate() {
        super.onCreate()
        appScope.launch { retentionManager.cleanup() }
    }
}
