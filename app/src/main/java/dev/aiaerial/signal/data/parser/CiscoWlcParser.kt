package dev.aiaerial.signal.data.parser

import dev.aiaerial.signal.data.model.EventType
import dev.aiaerial.signal.data.model.NetworkEvent
import dev.aiaerial.signal.data.model.Vendor

class CiscoWlcParser : VendorParser {

    private val MAC_PATTERN = """([0-9a-f]{2}:[0-9a-f]{2}:[0-9a-f]{2}:[0-9a-f]{2}:[0-9a-f]{2}:[0-9a-f]{2})""".toRegex(RegexOption.IGNORE_CASE)
    private val CHANNEL_PATTERN = """channel\s+\(?(\d+)\)?""".toRegex(RegexOption.IGNORE_CASE)
    private val RSSI_PATTERN = """rssi\s+\(?(-?\d+)\)?""".toRegex(RegexOption.IGNORE_CASE)
    private val REASON_PATTERN = """reason\s+(\d+)""".toRegex(RegexOption.IGNORE_CASE)

    private val CLIENT_ADDED = """CLIENT_ADDED_TO_RUN_STATE|joined with ssid""".toRegex(RegexOption.IGNORE_CASE)
    private val ASSOC = """%DOT11-\d-ASSOC|Associated MAP""".toRegex(RegexOption.IGNORE_CASE)
    private val DISASSOC = """%DOT11-\d-DISASSOC|Disassociated""".toRegex(RegexOption.IGNORE_CASE)
    private val DEAUTH = """%DOT11-\d-DEAUTH|Deauthenticated|DEAUTHENTICATION""".toRegex(RegexOption.IGNORE_CASE)
    private val AUTH_FAIL = """%DOT1X-\d-AUTH_FAIL|Authentication failed""".toRegex(RegexOption.IGNORE_CASE)

    private val CISCO_INDICATORS = """apfMsConnTask|%DOT11|%DOT1X|%CLIENT_ORCH|wncd:|capwap""".toRegex(RegexOption.IGNORE_CASE)

    private val AP_NAME_PARENS = """AP\s+name\s+\(([^)]+)\)""".toRegex(RegexOption.IGNORE_CASE)
    private val AP_NAME_MAP = """MAP\s+([A-Za-z0-9_\-]+)""".toRegex()

    override fun canParse(line: String): Boolean = CISCO_INDICATORS.containsMatchIn(line)

    override fun parse(line: String, sessionId: String): NetworkEvent? {
        if (!canParse(line)) return null

        val eventType = detectEventType(line) ?: return null
        val clientMac = MAC_PATTERN.find(line)?.groupValues?.get(1)
        val apName = extractApName(line)
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
            vendor = Vendor.CISCO,
            rawMessage = line,
            sessionId = sessionId,
        )
    }

    private fun detectEventType(line: String): EventType? = when {
        CLIENT_ADDED.containsMatchIn(line) -> EventType.ROAM
        DEAUTH.containsMatchIn(line) -> EventType.DEAUTH
        DISASSOC.containsMatchIn(line) -> EventType.DISASSOC
        ASSOC.containsMatchIn(line) -> EventType.ASSOC
        AUTH_FAIL.containsMatchIn(line) -> EventType.AUTH
        else -> null
    }

    private fun extractApName(line: String): String? {
        AP_NAME_PARENS.find(line)?.let { return it.groupValues[1] }
        AP_NAME_MAP.find(line)?.let { return it.groupValues[1] }
        return null
    }
}
