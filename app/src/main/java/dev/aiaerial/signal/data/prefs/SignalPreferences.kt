package dev.aiaerial.signal.data.prefs

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SignalPreferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("signal_prefs", Context.MODE_PRIVATE)

    var openClawUrl: String
        get() = prefs.getString("openclaw_url", "http://127.0.0.1:18789") ?: "http://127.0.0.1:18789"
        set(value) = prefs.edit().putString("openclaw_url", value).apply()

    var syslogPort: Int
        get() = prefs.getInt("syslog_port", 1514)
        set(value) = prefs.edit().putInt("syslog_port", value).apply()

    var setupComplete: Boolean
        get() = prefs.getBoolean("setup_complete", false)
        set(value) = prefs.edit().putBoolean("setup_complete", value).apply()

    /** Data retention period in days. Default 30. Set to 0 to disable auto-cleanup. */
    var retentionDays: Int
        get() = prefs.getInt("retention_days", 30)
        set(value) = prefs.edit().putInt("retention_days", value).apply()

    /** Demo mode: use sample data instead of live infrastructure. */
    var demoMode: Boolean
        get() = prefs.getBoolean("demo_mode", false)
        set(value) = prefs.edit().putBoolean("demo_mode", value).apply()

    /** Selected demo scenario index. Maps to DemoScenario.entries. */
    var demoScenarioIndex: Int
        get() = prefs.getInt("demo_scenario_index", 0)
        set(value) = prefs.edit().putInt("demo_scenario_index", value).apply()

    // --- Alert thresholds ---

    /** RSSI threshold below which a weak signal alert fires. */
    var alertRssiThreshold: Int
        get() = prefs.getInt("alert_rssi_threshold", -70)
        set(value) = prefs.edit().putInt("alert_rssi_threshold", value).apply()

    /** Number of roams within alertRoamWindowMinutes that triggers churn alert. */
    var alertRoamChurnCount: Int
        get() = prefs.getInt("alert_roam_churn_count", 5)
        set(value) = prefs.edit().putInt("alert_roam_churn_count", value).apply()

    /** Window in minutes for roam churn detection. */
    var alertRoamWindowMinutes: Int
        get() = prefs.getInt("alert_roam_window_minutes", 10)
        set(value) = prefs.edit().putInt("alert_roam_window_minutes", value).apply()

    /** Number of auth failures that triggers auth failure alert. */
    var alertAuthFailureCount: Int
        get() = prefs.getInt("alert_auth_failure_count", 3)
        set(value) = prefs.edit().putInt("alert_auth_failure_count", value).apply()
}
