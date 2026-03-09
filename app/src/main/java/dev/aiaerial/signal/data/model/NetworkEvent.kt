package dev.aiaerial.signal.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "network_events")
data class NetworkEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val eventType: EventType,
    val clientMac: String? = null,
    val apName: String? = null,
    val bssid: String? = null,
    val channel: Int? = null,
    val rssi: Int? = null,
    val reasonCode: Int? = null,
    val vendor: Vendor = Vendor.GENERIC,
    val rawMessage: String,
    val sessionId: String,
)
