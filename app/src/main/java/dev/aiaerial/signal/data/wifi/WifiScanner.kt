package dev.aiaerial.signal.data.wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WifiScanner @Inject constructor(
    private val wifiManager: WifiManager,
    private val connectivityManager: ConnectivityManager,
    @ApplicationContext private val context: Context,
) {
    /** Emits scan results whenever a WiFi scan completes. */
    fun scanResults(): Flow<List<WifiScanResult>> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val results = wifiManager.scanResults.map { it.toWifiScanResult() }
                trySend(results)
            }
        }
        context.registerReceiver(
            receiver,
            IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION),
        )
        // Emit current results immediately
        trySend(wifiManager.scanResults.map { it.toWifiScanResult() })

        awaitClose { context.unregisterReceiver(receiver) }
    }

    /**
     * Trigger an active WiFi scan.
     * Deprecated in API 28 but no replacement exists — platform throttles to
     * ~4 scans per 2 minutes in foreground. This is the only way to request scans.
     */
    @Suppress("DEPRECATION")
    fun triggerScan() {
        wifiManager.startScan()
    }

    /**
     * Get current WiFi connection info, or null if not connected.
     * Uses modern ConnectivityManager API on API 31+ (avoids deprecated WifiManager.connectionInfo),
     * falls back to deprecated API on API 29-30.
     */
    fun connectionInfo(): WifiConnectionInfo? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            connectionInfoModern()
        } else {
            connectionInfoLegacy()
        }
    }

    /** API 31+: Get WifiInfo from ConnectivityManager's active network capabilities. */
    private fun connectionInfoModern(): WifiConnectionInfo? {
        val network = connectivityManager.activeNetwork ?: return null
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return null
        if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return null

        val wifiInfo = caps.transportInfo as? WifiInfo ?: return null
        return wifiInfoToConnectionInfo(wifiInfo)
    }

    /** API 29-30: Fall back to deprecated WifiManager.connectionInfo. */
    @Suppress("DEPRECATION")
    private fun connectionInfoLegacy(): WifiConnectionInfo? {
        val info = wifiManager.connectionInfo ?: return null
        return wifiInfoToConnectionInfo(info)
    }

    private fun wifiInfoToConnectionInfo(info: WifiInfo): WifiConnectionInfo? {
        @Suppress("DEPRECATION")
        val ssid = info.ssid?.removeSurrounding("\"") ?: return null
        if (ssid == "<unknown ssid>") return null
        return WifiConnectionInfo(
            ssid = ssid,
            bssid = info.bssid ?: "",
            rssi = info.rssi,
            linkSpeed = info.linkSpeed,
            frequency = info.frequency,
            ipAddress = intToIp(info.ipAddress),
        )
    }

    private fun ScanResult.toWifiScanResult(): WifiScanResult {
        val ssidName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            wifiSsid?.toString()?.removeSurrounding("\"") ?: ""
        } else {
            @Suppress("DEPRECATION")
            SSID ?: ""
        }
        return WifiScanResult(
            ssid = ssidName,
            bssid = BSSID ?: "",
            rssi = level,
            frequency = frequency,
            channelWidth = channelWidth,
            security = extractSecurity(this),
            timestamp = timestamp,
        )
    }

    companion object {
        /** Extract security type from a ScanResult's capabilities string. */
        fun extractSecurity(result: ScanResult): String {
            val caps = result.capabilities ?: return "Open"
            return when {
                "SAE" in caps || "WPA3" in caps -> "WPA3"
                "RSN" in caps || "WPA2" in caps -> "WPA2"
                "WPA" in caps -> "WPA"
                "WEP" in caps -> "WEP"
                else -> "Open"
            }
        }

        /** Convert an integer IP address from WifiInfo to dotted-quad string. */
        fun intToIp(ip: Int): String =
            "${ip and 0xFF}.${ip shr 8 and 0xFF}.${ip shr 16 and 0xFF}.${ip shr 24 and 0xFF}"
    }
}
