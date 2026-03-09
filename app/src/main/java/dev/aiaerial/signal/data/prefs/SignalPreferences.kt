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
}
