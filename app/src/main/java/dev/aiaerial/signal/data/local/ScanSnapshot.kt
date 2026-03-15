package dev.aiaerial.signal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A saved WiFi scan snapshot — captures the environment at a point in time.
 * Used for before/after comparisons, walk test checkpoints, and historical records.
 */
@Entity(tableName = "scan_snapshots")
data class ScanSnapshot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val label: String, // user-provided or auto-generated label
    val ssid: String?, // connected SSID at time of snapshot
    val bssid: String?, // connected BSSID
    val rssi: Int?, // connected RSSI
    val networkCount: Int, // total networks found
    val dataJson: String, // serialized list of WifiScanResult as JSON
)
