package dev.aiaerial.signal.data.wifi

data class WifiConnectionInfo(
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val linkSpeed: Int,
    val frequency: Int,
    val ipAddress: String,
)
