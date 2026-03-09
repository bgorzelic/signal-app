package dev.aiaerial.signal.data.wifi

data class WifiScanResult(
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val frequency: Int,
    val channelWidth: Int,
    val security: String,
    val timestamp: Long,
) {
    /** Derive WiFi channel number from frequency in MHz. */
    val channel: Int
        get() = when {
            frequency in 2400..2500 -> (frequency - 2407) / 5
            frequency in 5000..5900 -> (frequency - 5000) / 5
            frequency in 5925..7125 -> (frequency - 5950) / 5
            else -> 0
        }

    /** Human-readable band string. */
    val band: String
        get() = when {
            frequency in 2400..2500 -> "2.4 GHz"
            frequency in 5000..5900 -> "5 GHz"
            frequency in 5925..7125 -> "6 GHz"
            else -> "Unknown"
        }
}
