package dev.aiaerial.signal.data.retention

import android.util.Log
import dev.aiaerial.signal.data.local.NetworkEventDao
import dev.aiaerial.signal.data.prefs.SignalPreferences
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages automatic cleanup of old network events to prevent unbounded DB growth.
 *
 * Default policy: delete events older than 30 days.
 * Configurable via [SignalPreferences.retentionDays].
 * Set retentionDays to 0 to disable automatic cleanup.
 */
@Singleton
class DataRetentionManager @Inject constructor(
    private val dao: NetworkEventDao,
    private val prefs: SignalPreferences,
) {
    /**
     * Run retention cleanup. Call on app startup or session start.
     * Returns the number of deleted events, or 0 if retention is disabled.
     */
    suspend fun cleanup(): Int {
        val days = prefs.retentionDays
        if (days <= 0) return 0

        val cutoff = System.currentTimeMillis() - (days * 24L * 60L * 60L * 1000L)
        val deleted = dao.deleteOlderThan(cutoff)
        if (deleted > 0) {
            Log.i(TAG, "Retention cleanup: deleted $deleted events older than $days days")
        }
        return deleted
    }

    /** Get current database size in events. */
    suspend fun totalEventCount(): Int = dao.getTotalEventCount()

    companion object {
        private const val TAG = "DataRetention"
    }
}
