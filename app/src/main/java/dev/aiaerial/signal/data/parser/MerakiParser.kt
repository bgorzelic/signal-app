package dev.aiaerial.signal.data.parser

import dev.aiaerial.signal.data.model.EventType
import dev.aiaerial.signal.data.model.NetworkEvent
import dev.aiaerial.signal.data.model.Vendor

/**
 * Parser for Cisco Meraki MR access point syslog messages.
 *
 * Meraki syslog uses a flat key=value format:
 *   <priority>version timestamp host type=<type> <key=value pairs>
 *
 * Key message types:
 *   association      — client associated to AP
 *   disassociation   — client disassociated from AP
 *   8021x_auth       — 802.1X authentication events
 *   8021x_deauth     — 802.1X deauthentication
 *   splash_auth      — captive portal authentication
 *
 * Examples:
 *   1 1678400000.123456 MR-Floor2 events type=association radio=1 vap=0 channel=36 rssi=28 aid=1234567890 mac=AA:BB:CC:DD:EE:FF
 *   1 1678400010.654321 MR-Floor2 events type=disassociation radio=1 vap=0 channel=36 aid=1234567890 mac=AA:BB:CC:DD:EE:FF reason=8
 *   1 1678400020.111111 MR-Floor2 events type=8021x_auth identity=user@corp mac=AA:BB:CC:DD:EE:FF
 *   1 1678400030.222222 MR-Floor2 events type=8021x_deauth identity=user@corp mac=AA:BB:CC:DD:EE:FF
 */
class MerakiParser : VendorParser {

    private val MAC_PATTERN = """mac=([0-9a-fA-F]{2}:[0-9a-fA-F]{2}:[0-9a-fA-F]{2}:[0-9a-fA-F]{2}:[0-9a-fA-F]{2}:[0-9a-fA-F]{2})""".toRegex()
    private val CHANNEL_PATTERN = """channel=(\d+)""".toRegex()
    private val RSSI_PATTERN = """rssi=(-?\d+)""".toRegex()
    private val REASON_PATTERN = """reason=(\d+)""".toRegex()
    private val TYPE_PATTERN = """type=(\S+)""".toRegex()

    // Meraki AP names appear as the syslog hostname, typically before "events"
    private val AP_NAME_PATTERN = """\d+\.\d+\s+([\w\-]+)\s+events""".toRegex()

    // Meraki syslog is identifiable by the "type=<event>" key-value format with "events" keyword
    private val MERAKI_INDICATORS = """\bevents\s+type=|type=association|type=disassociation|type=8021x_""".toRegex(RegexOption.IGNORE_CASE)

    override fun canParse(line: String): Boolean = MERAKI_INDICATORS.containsMatchIn(line)

    override fun parse(line: String, sessionId: String): NetworkEvent? {
        if (!canParse(line)) return null

        val type = TYPE_PATTERN.find(line)?.groupValues?.get(1) ?: return null
        val eventType = detectEventType(type) ?: return null
        val clientMac = MAC_PATTERN.find(line)?.groupValues?.get(1)
        val apName = AP_NAME_PATTERN.find(line)?.groupValues?.get(1)
        val channel = CHANNEL_PATTERN.find(line)?.groupValues?.get(1)?.toIntOrNull()
        val rssi = RSSI_PATTERN.find(line)?.groupValues?.get(1)?.toIntOrNull()
        val reasonCode = REASON_PATTERN.find(line)?.groupValues?.get(1)?.toIntOrNull()

        return NetworkEvent(
            timestamp = System.currentTimeMillis(),
            eventType = eventType,
            clientMac = clientMac,
            apName = apName,
            channel = channel,
            rssi = rssi,
            reasonCode = reasonCode,
            vendor = Vendor.MERAKI,
            rawMessage = line,
            sessionId = sessionId,
        )
    }

    private fun detectEventType(type: String): EventType? = when (type.lowercase()) {
        "association" -> EventType.ASSOC
        "reassociation" -> EventType.ROAM
        "disassociation" -> EventType.DISASSOC
        "8021x_auth", "splash_auth" -> EventType.AUTH
        "8021x_deauth" -> EventType.DEAUTH
        else -> null
    }
}
