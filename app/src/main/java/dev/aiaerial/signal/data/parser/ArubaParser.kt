package dev.aiaerial.signal.data.parser

import dev.aiaerial.signal.data.model.EventType
import dev.aiaerial.signal.data.model.NetworkEvent
import dev.aiaerial.signal.data.model.Vendor

/**
 * Parser for Aruba Mobility Controller (AOS-8) and ArubaOS-CX syslog messages.
 *
 * Aruba WLC syslog uses format:
 *   <priority>date host authmgr|stm|sapd|nanny: <CATEGORY> <message>
 *
 * Key message categories:
 *   authmgr — 802.1X authentication events
 *   stm     — station management (association, roaming, deauth)
 *   sapd    — AP/radio management
 *
 * Examples:
 *   stm[1234]: <501051> Station aa:bb:cc:dd:ee:ff Associated to AP ap-floor2 BSSID 00:11:22:33:44:55 SSID CorpWiFi channel 36 reason New Association
 *   stm[1234]: <501080> Station aa:bb:cc:dd:ee:ff Deauthenticated from AP ap-lobby reason 1
 *   authmgr[5678]: <522008> User Authentication Successful: MAC=aa:bb:cc:dd:ee:ff
 *   authmgr[5678]: <522006> User Authentication Failed: MAC=aa:bb:cc:dd:ee:ff reason=timeout
 *   stm[1234]: <501093> Station aa:bb:cc:dd:ee:ff Disassociated from AP ap-floor1 reason 8
 *   stm[1234]: <501058> Station aa:bb:cc:dd:ee:ff Roamed to AP ap-floor3 BSSID 00:11:22:33:44:99 channel 149
 */
class ArubaParser : VendorParser {

    // MAC in colon-separated or dot-separated (Aruba sometimes uses xxxx.xxxx.xxxx)
    private val MAC_COLON = """([0-9a-f]{2}:[0-9a-f]{2}:[0-9a-f]{2}:[0-9a-f]{2}:[0-9a-f]{2}:[0-9a-f]{2})""".toRegex(RegexOption.IGNORE_CASE)
    private val MAC_DOT = """([0-9a-f]{4}\.[0-9a-f]{4}\.[0-9a-f]{4})""".toRegex(RegexOption.IGNORE_CASE)

    private val CHANNEL_PATTERN = """channel\s+(\d+)""".toRegex(RegexOption.IGNORE_CASE)
    private val RSSI_PATTERN = """(?:rssi|signal)\s+(-?\d+)""".toRegex(RegexOption.IGNORE_CASE)
    private val REASON_PATTERN = """reason\s+(\d+)""".toRegex(RegexOption.IGNORE_CASE)

    // AP name extraction: "AP ap-name" or "AP-Name ap-name" patterns
    private val AP_FROM_TO = """(?:from|to)\s+AP\s+([\w\-]+)""".toRegex(RegexOption.IGNORE_CASE)
    private val AP_ASSOCIATED = """Associated\s+to\s+AP\s+([\w\-]+)""".toRegex(RegexOption.IGNORE_CASE)

    // Event type detection
    private val ROAM_PATTERN = """Roamed\s+to\s+AP""".toRegex(RegexOption.IGNORE_CASE)
    private val ASSOC_PATTERN = """(?:Associated\s+to|New Association|association\s+successful)""".toRegex(RegexOption.IGNORE_CASE)
    private val DISASSOC_PATTERN = """Disassociated\s+from""".toRegex(RegexOption.IGNORE_CASE)
    private val DEAUTH_PATTERN = """Deauthenticated\s+from|deauthentication""".toRegex(RegexOption.IGNORE_CASE)
    private val AUTH_FAIL_PATTERN = """Authentication\s+Failed|auth.*fail""".toRegex(RegexOption.IGNORE_CASE)
    private val AUTH_SUCCESS_PATTERN = """Authentication\s+Successful""".toRegex(RegexOption.IGNORE_CASE)

    // Aruba-specific syslog identifiers
    private val ARUBA_INDICATORS = """\bstm\[|authmgr\[|sapd\[|nanny\[|<50\d{4}>|<52\d{4}>|aruba""".toRegex(RegexOption.IGNORE_CASE)

    override fun canParse(line: String): Boolean = ARUBA_INDICATORS.containsMatchIn(line)

    override fun parse(line: String, sessionId: String): NetworkEvent? {
        if (!canParse(line)) return null

        val eventType = detectEventType(line) ?: return null
        val clientMac = extractMac(line)
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
            vendor = Vendor.ARUBA,
            rawMessage = line,
            sessionId = sessionId,
        )
    }

    private fun detectEventType(line: String): EventType? = when {
        ROAM_PATTERN.containsMatchIn(line) -> EventType.ROAM
        DEAUTH_PATTERN.containsMatchIn(line) -> EventType.DEAUTH
        DISASSOC_PATTERN.containsMatchIn(line) -> EventType.DISASSOC
        AUTH_FAIL_PATTERN.containsMatchIn(line) -> EventType.AUTH
        AUTH_SUCCESS_PATTERN.containsMatchIn(line) -> EventType.AUTH
        ASSOC_PATTERN.containsMatchIn(line) -> EventType.ASSOC
        else -> null
    }

    private fun extractMac(line: String): String? {
        // Try colon-separated first (more common in Aruba syslog)
        MAC_COLON.find(line)?.let { return it.groupValues[1] }
        // Try dot-separated (xxxx.xxxx.xxxx), convert to colon format
        MAC_DOT.find(line)?.let { match ->
            val hex = match.groupValues[1].replace(".", "")
            return hex.chunked(2).joinToString(":")
        }
        return null
    }

    private fun extractApName(line: String): String? {
        AP_ASSOCIATED.find(line)?.let { return it.groupValues[1] }
        AP_FROM_TO.find(line)?.let { return it.groupValues[1] }
        return null
    }
}
