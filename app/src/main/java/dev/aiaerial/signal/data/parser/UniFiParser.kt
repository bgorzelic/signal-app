package dev.aiaerial.signal.data.parser

import dev.aiaerial.signal.data.model.EventType
import dev.aiaerial.signal.data.model.NetworkEvent
import dev.aiaerial.signal.data.model.Vendor

/**
 * Parser for Ubiquiti UniFi controller syslog messages.
 *
 * UniFi syslog uses formats:
 *   hostname BZ2,f0:9f:c2:xx:xx:xx,v6.6.55: sta: aa:bb:cc:dd:ee:ff ... associated
 *   hostname U7PG2,dc:9f:db:xx:xx:xx: hostapd: ath0: STA aa:bb:cc:dd:ee:ff IEEE 802.11: associated
 *   hostname kernel: [12345.678] wlan0: STA aa:bb:cc:dd:ee:ff IEEE 802.11: deauthenticated
 *
 * Also handles newer UniFi OS / Network Application format:
 *   [UAP-AC-Pro] STA aa:bb:cc:dd:ee:ff associated to SSID "CorpNet" on channel 36
 *   [USW-Pro] STA aa:bb:cc:dd:ee:ff disassociated from SSID "CorpNet" reason 8
 */
class UniFiParser : VendorParser {

    private val MAC = """([0-9a-f]{2}:[0-9a-f]{2}:[0-9a-f]{2}:[0-9a-f]{2}:[0-9a-f]{2}:[0-9a-f]{2})""".toRegex(RegexOption.IGNORE_CASE)
    private val CHANNEL = """channel\s+(\d+)""".toRegex(RegexOption.IGNORE_CASE)
    private val RSSI = """rssi\s+(-?\d+)|signal\s+(-?\d+)""".toRegex(RegexOption.IGNORE_CASE)
    private val REASON = """reason\s+(\d+)""".toRegex(RegexOption.IGNORE_CASE)
    private val AP_BRACKET = """\[([^\]]+)]""".toRegex()
    private val SSID_QUOTED = """SSID\s+"([^"]+)"""".toRegex(RegexOption.IGNORE_CASE)

    private val INDICATORS = """\bUniFi|hostapd:|BZ2,|U7P|UAP-|USW-|UDM-|STA\s+[0-9a-f]{2}:.*(?:associat|deauth|disassoc|auth)""".toRegex(RegexOption.IGNORE_CASE)

    override fun canParse(line: String): Boolean = INDICATORS.containsMatchIn(line)

    override fun parse(line: String, sessionId: String): NetworkEvent? {
        if (!canParse(line)) return null

        val eventType = when {
            line.contains("reassociated", ignoreCase = true) -> EventType.ROAM
            line.contains("deauthenticated", ignoreCase = true) -> EventType.DEAUTH
            line.contains("disassociated", ignoreCase = true) -> EventType.DISASSOC
            line.contains("associated", ignoreCase = true) -> EventType.ASSOC
            line.contains("auth", ignoreCase = true) -> EventType.AUTH
            else -> return null
        }

        // Extract STA MAC — skip the first MAC which is often the AP's own MAC
        val macs = MAC.findAll(line).toList()
        // In UniFi syslog, the STA MAC typically follows "STA" keyword
        val staIndex = line.indexOf("STA", ignoreCase = true)
        val clientMac = if (staIndex >= 0) {
            macs.firstOrNull { it.range.first > staIndex }?.groupValues?.get(1)
        } else {
            macs.lastOrNull()?.groupValues?.get(1)
        }

        val apName = AP_BRACKET.find(line)?.groupValues?.get(1)
        val channel = CHANNEL.find(line)?.groupValues?.get(1)?.toIntOrNull()
        val rssiMatch = RSSI.find(line)
        val rssi = rssiMatch?.let {
            (it.groupValues[1].toIntOrNull() ?: it.groupValues[2].toIntOrNull())
        }
        val reasonCode = REASON.find(line)?.groupValues?.get(1)?.toIntOrNull()

        return NetworkEvent(
            timestamp = System.currentTimeMillis(),
            eventType = eventType,
            clientMac = clientMac,
            apName = apName,
            channel = channel,
            rssi = rssi,
            reasonCode = reasonCode,
            vendor = Vendor.GENERIC, // No UBIQUITI enum yet — use GENERIC
            rawMessage = line,
            sessionId = sessionId,
        )
    }
}
