package dev.aiaerial.signal.data.wifi

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Serializes WiFi scan results to/from JSON for storage in ScanSnapshot.dataJson.
 */
object ScanSnapshotSerializer {

    @Serializable
    private data class ScanEntry(
        val ssid: String,
        val bssid: String,
        val rssi: Int,
        val frequency: Int,
        val channelWidth: Int,
        val security: String,
    )

    private val json = Json { prettyPrint = false }

    fun serialize(results: List<WifiScanResult>): String {
        val entries = results.map {
            ScanEntry(it.ssid, it.bssid, it.rssi, it.frequency, it.channelWidth, it.security)
        }
        return json.encodeToString(entries)
    }

    fun deserialize(data: String): List<WifiScanResult> {
        return try {
            val entries = json.decodeFromString<List<ScanEntry>>(data)
            val now = System.currentTimeMillis()
            entries.map {
                WifiScanResult(it.ssid, it.bssid, it.rssi, it.frequency, it.channelWidth, it.security, now)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
