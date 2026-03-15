package dev.aiaerial.signal.data.parser

import dev.aiaerial.signal.data.model.EventType
import dev.aiaerial.signal.data.model.NetworkEvent
import dev.aiaerial.signal.data.model.Vendor

/**
 * Parser for Ruckus Wireless (SmartZone / Unleashed) syslog messages.
 *
 * Ruckus syslog format:
 *   <priority>timestamp hostname ruckus: STA-ASSOC/STA-ROAM/STA-LEAVE ...
 *
 * Examples:
 *   ruckus: STA-ASSOC aa:bb:cc:dd:ee:ff on AP[AP-Lobby] ssid[CorpNet] channel[36] rssi[-45]
 *   ruckus: STA-ROAM aa:bb:cc:dd:ee:ff from AP[AP-Floor1] to AP[AP-Floor2] channel[149] rssi[-52]
 *   ruckus: STA-LEAVE aa:bb:cc:dd:ee:ff on AP[AP-Lobby] reason[1]
 *   ruckus: STA-AUTH-FAIL aa:bb:cc:dd:ee:ff on AP[AP-Floor2] reason[auth-timeout]
 *   ruckus: STA-DEAUTH aa:bb:cc:dd:ee:ff on AP[AP-Lobby] reason[4]
 */
class RuckusParser : VendorParser {

    private val MAC = """([0-9a-f]{2}:[0-9a-f]{2}:[0-9a-f]{2}:[0-9a-f]{2}:[0-9a-f]{2}:[0-9a-f]{2})""".toRegex(RegexOption.IGNORE_CASE)
    private val AP_BRACKET = """AP\[([^\]]+)]""".toRegex(RegexOption.IGNORE_CASE)
    private val AP_TO = """to\s+AP\[([^\]]+)]""".toRegex(RegexOption.IGNORE_CASE)
    private val CHANNEL = """channel\[(\d+)]""".toRegex(RegexOption.IGNORE_CASE)
    private val RSSI = """rssi\[(-?\d+)]""".toRegex(RegexOption.IGNORE_CASE)
    private val REASON = """reason\[([^\]]+)]""".toRegex(RegexOption.IGNORE_CASE)

    private val INDICATORS = """\bruckus:|STA-(?:ASSOC|ROAM|LEAVE|AUTH|DEAUTH)""".toRegex(RegexOption.IGNORE_CASE)

    override fun canParse(line: String): Boolean = INDICATORS.containsMatchIn(line)

    override fun parse(line: String, sessionId: String): NetworkEvent? {
        if (!canParse(line)) return null

        val eventType = when {
            line.contains("STA-ROAM", ignoreCase = true) -> EventType.ROAM
            line.contains("STA-ASSOC", ignoreCase = true) -> EventType.ASSOC
            line.contains("STA-DEAUTH", ignoreCase = true) -> EventType.DEAUTH
            line.contains("STA-LEAVE", ignoreCase = true) -> EventType.DISASSOC
            line.contains("STA-AUTH-FAIL", ignoreCase = true) -> EventType.AUTH
            line.contains("STA-AUTH", ignoreCase = true) -> EventType.AUTH
            else -> return null
        }

        val clientMac = MAC.find(line)?.groupValues?.get(1)
        val apName = if (eventType == EventType.ROAM) {
            AP_TO.find(line)?.groupValues?.get(1) ?: AP_BRACKET.find(line)?.groupValues?.get(1)
        } else {
            AP_BRACKET.find(line)?.groupValues?.get(1)
        }
        val channel = CHANNEL.find(line)?.groupValues?.get(1)?.toIntOrNull()
        val rssi = RSSI.find(line)?.groupValues?.get(1)?.toIntOrNull()
        val reasonStr = REASON.find(line)?.groupValues?.get(1)
        val reasonCode = reasonStr?.toIntOrNull()

        return NetworkEvent(
            timestamp = System.currentTimeMillis(),
            eventType = eventType,
            clientMac = clientMac,
            apName = apName,
            channel = channel,
            rssi = rssi,
            reasonCode = reasonCode,
            vendor = Vendor.RUCKUS,
            rawMessage = line,
            sessionId = sessionId,
        )
    }
}
