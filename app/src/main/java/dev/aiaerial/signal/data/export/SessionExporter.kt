package dev.aiaerial.signal.data.export

import dev.aiaerial.signal.data.model.NetworkEvent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object SessionExporter {

    private val json = Json { prettyPrint = true }

    fun toCsv(events: List<NetworkEvent>): String = buildString {
        appendLine("timestamp,event_type,client_mac,ap_name,bssid,channel,rssi,reason_code,vendor")
        events.forEach { e ->
            appendLine(
                "${e.timestamp},${e.eventType},${e.clientMac ?: ""},${e.apName ?: ""}," +
                "${e.bssid ?: ""},${e.channel ?: ""},${e.rssi ?: ""},${e.reasonCode ?: ""},${e.vendor}"
            )
        }
    }.trimEnd()

    fun toJson(events: List<NetworkEvent>): String {
        val serializable = events.map { e ->
            mapOf(
                "timestamp" to e.timestamp.toString(),
                "eventType" to e.eventType.name,
                "clientMac" to (e.clientMac ?: ""),
                "apName" to (e.apName ?: ""),
                "bssid" to (e.bssid ?: ""),
                "channel" to (e.channel?.toString() ?: ""),
                "rssi" to (e.rssi?.toString() ?: ""),
                "reasonCode" to (e.reasonCode?.toString() ?: ""),
                "vendor" to e.vendor.name,
                "rawMessage" to e.rawMessage,
            )
        }
        return json.encodeToString(serializable)
    }
}
