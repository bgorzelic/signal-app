package dev.aiaerial.signal.data.export

import dev.aiaerial.signal.data.model.NetworkEvent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object SessionExporter {

    private val json = Json { prettyPrint = true }

    private fun String.csvEscape(): String {
        val needsQuoting = contains(',') || contains('"') || contains('\n') || contains('\r') ||
            startsWith('=') || startsWith('+') || startsWith('-') || startsWith('@')
        return if (needsQuoting) "\"${replace("\"", "\"\"")}\"" else this
    }

    fun toCsv(events: List<NetworkEvent>): String = buildString {
        appendLine("timestamp,event_type,client_mac,ap_name,bssid,channel,rssi,reason_code,vendor")
        events.forEach { e ->
            appendLine(
                "${e.timestamp},${e.eventType.name.csvEscape()},${(e.clientMac ?: "").csvEscape()}," +
                "${(e.apName ?: "").csvEscape()},${(e.bssid ?: "").csvEscape()},${e.channel ?: ""}," +
                "${e.rssi ?: ""},${e.reasonCode ?: ""},${e.vendor.name.csvEscape()}"
            )
        }
    }.trimEnd('\n', '\r')

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
