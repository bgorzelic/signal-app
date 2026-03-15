package dev.aiaerial.signal.data.parser

import dev.aiaerial.signal.data.model.EventType
import dev.aiaerial.signal.data.model.NetworkEvent
import dev.aiaerial.signal.data.model.Vendor

/**
 * Parser for Juniper Mist syslog messages.
 *
 * Mist uses JSON-structured syslog or key=value format:
 *   mist: event_type=client_assoc client_mac=aa:bb:cc:dd:ee:ff ap_name=AP-Floor1 channel=36 rssi=-48
 *   mist: event_type=client_roam client_mac=aa:bb:cc:dd:ee:ff ap_name=AP-Floor2 channel=149 rssi=-52
 *   mist: event_type=client_deauth client_mac=aa:bb:cc:dd:ee:ff ap_name=AP-Floor1 reason=4
 *   mist: event_type=client_auth_fail client_mac=aa:bb:cc:dd:ee:ff ap_name=AP-Floor2
 *
 * Also handles Juniper EX/SRX style:
 *   UI_DAUTH_LOGIN_EVENT: aa:bb:cc:dd:ee:ff authenticated on wlan0
 */
class JuniperMistParser : VendorParser {

    private val MAC = """([0-9a-f]{2}:[0-9a-f]{2}:[0-9a-f]{2}:[0-9a-f]{2}:[0-9a-f]{2}:[0-9a-f]{2})""".toRegex(RegexOption.IGNORE_CASE)
    private val KV_EVENT = """event_type=(\S+)""".toRegex(RegexOption.IGNORE_CASE)
    private val KV_CLIENT_MAC = """client_mac=([0-9a-f:]+)""".toRegex(RegexOption.IGNORE_CASE)
    private val KV_AP_NAME = """ap_name=(\S+)""".toRegex(RegexOption.IGNORE_CASE)
    private val KV_CHANNEL = """channel=(\d+)""".toRegex(RegexOption.IGNORE_CASE)
    private val KV_RSSI = """rssi=(-?\d+)""".toRegex(RegexOption.IGNORE_CASE)
    private val KV_REASON = """reason=(\d+)""".toRegex(RegexOption.IGNORE_CASE)

    private val INDICATORS = """\bmist:|event_type=client_|juniper|UI_DAUTH""".toRegex(RegexOption.IGNORE_CASE)

    override fun canParse(line: String): Boolean = INDICATORS.containsMatchIn(line)

    override fun parse(line: String, sessionId: String): NetworkEvent? {
        if (!canParse(line)) return null

        val eventTypeStr = KV_EVENT.find(line)?.groupValues?.get(1)?.lowercase()

        val eventType = when {
            eventTypeStr == "client_roam" -> EventType.ROAM
            eventTypeStr == "client_assoc" || eventTypeStr == "client_association" -> EventType.ASSOC
            eventTypeStr == "client_disassoc" -> EventType.DISASSOC
            eventTypeStr == "client_deauth" -> EventType.DEAUTH
            eventTypeStr == "client_auth" -> EventType.AUTH
            eventTypeStr == "client_auth_fail" -> EventType.AUTH
            // Fallback to keyword detection
            line.contains("roam", ignoreCase = true) -> EventType.ROAM
            line.contains("deauth", ignoreCase = true) -> EventType.DEAUTH
            line.contains("disassoc", ignoreCase = true) -> EventType.DISASSOC
            line.contains("assoc", ignoreCase = true) -> EventType.ASSOC
            line.contains("auth", ignoreCase = true) -> EventType.AUTH
            else -> return null
        }

        val clientMac = KV_CLIENT_MAC.find(line)?.groupValues?.get(1) ?: MAC.find(line)?.groupValues?.get(1)
        val apName = KV_AP_NAME.find(line)?.groupValues?.get(1)
        val channel = KV_CHANNEL.find(line)?.groupValues?.get(1)?.toIntOrNull()
        val rssi = KV_RSSI.find(line)?.groupValues?.get(1)?.toIntOrNull()
        val reasonCode = KV_REASON.find(line)?.groupValues?.get(1)?.toIntOrNull()

        return NetworkEvent(
            timestamp = System.currentTimeMillis(),
            eventType = eventType,
            clientMac = clientMac,
            apName = apName,
            channel = channel,
            rssi = rssi,
            reasonCode = reasonCode,
            vendor = Vendor.JUNIPER,
            rawMessage = line,
            sessionId = sessionId,
        )
    }
}
