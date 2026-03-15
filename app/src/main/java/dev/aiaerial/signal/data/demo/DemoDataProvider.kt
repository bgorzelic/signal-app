package dev.aiaerial.signal.data.demo

import dev.aiaerial.signal.data.model.EventType
import dev.aiaerial.signal.data.model.NetworkEvent
import dev.aiaerial.signal.data.model.Vendor
import dev.aiaerial.signal.data.syslog.SyslogMessage
import dev.aiaerial.signal.data.wifi.ChannelUtilization
import dev.aiaerial.signal.data.wifi.WifiConnectionInfo
import dev.aiaerial.signal.data.wifi.WifiScanResult
import java.util.UUID

/**
 * Deterministic demo data factory.
 * Produces realistic sample data for all screens without live infrastructure.
 * Data is seeded and reproducible — not random.
 */
object DemoDataProvider {

    private const val DEMO_SESSION = "demo-session-001"
    private val baseTime = System.currentTimeMillis() - 3_600_000L // 1 hour ago

    // =========================================================================
    // WiFi Scan Results
    // =========================================================================

    fun wifiScanResults(scenario: DemoScenario = DemoScenario.HEALTHY_ROAMING): List<WifiScanResult> =
        when (scenario) {
            DemoScenario.HEALTHY_ROAMING -> healthyOfficeNetworks()
            DemoScenario.STICKY_CLIENT -> stickyClientNetworks()
            DemoScenario.CHANNEL_CONGESTION -> congestedNetworks()
        }

    private fun healthyOfficeNetworks() = listOf(
        scan("CorpNet-5G", "a8:13:0b:10:01:01", -42, 5540, 80, "WPA2-Enterprise"),
        scan("CorpNet-5G", "a8:13:0b:10:01:02", -55, 5500, 80, "WPA2-Enterprise"),
        scan("CorpNet-5G", "a8:13:0b:10:01:03", -63, 5260, 80, "WPA2-Enterprise"),
        scan("CorpNet-5G", "a8:13:0b:10:01:04", -71, 5745, 80, "WPA2-Enterprise"),
        scan("CorpNet-2G", "a8:13:0b:10:02:01", -48, 2437, 20, "WPA2-Enterprise"),
        scan("CorpNet-2G", "a8:13:0b:10:02:02", -62, 2462, 20, "WPA2-Enterprise"),
        scan("Guest-WiFi", "a8:13:0b:10:03:01", -58, 5540, 40, "WPA2"),
        scan("Guest-WiFi", "a8:13:0b:10:03:02", -68, 5500, 40, "WPA2"),
        scan("IoT-Sensors", "a8:13:0b:10:04:01", -55, 2412, 20, "WPA2"),
    )

    private fun stickyClientNetworks() = listOf(
        scan("CorpNet-5G", "a8:13:0b:20:01:01", -78, 5540, 80, "WPA2-Enterprise"), // weak — sticky
        scan("CorpNet-5G", "a8:13:0b:20:01:02", -41, 5500, 80, "WPA2-Enterprise"), // strong — should roam here
        scan("CorpNet-5G", "a8:13:0b:20:01:03", -52, 5260, 80, "WPA2-Enterprise"),
        scan("CorpNet-2G", "a8:13:0b:20:02:01", -65, 2437, 20, "WPA2-Enterprise"),
        scan("Neighbor-AP", "dc:a6:32:aa:bb:cc", -72, 2437, 20, "WPA2"),
    )

    private fun congestedNetworks(): List<WifiScanResult> {
        val results = mutableListOf<WifiScanResult>()
        // 12 APs on channel 1, 6, and 11 — dense 2.4 GHz
        val ssids = listOf("CorpNet", "Guest", "IoT", "BYOD")
        val ch1 = 2412; val ch6 = 2437; val ch11 = 2462
        var i = 0
        for (ch in listOf(ch1, ch1, ch1, ch1, ch6, ch6, ch6, ch11, ch11, ch11, ch11, ch11)) {
            val ssid = ssids[i % ssids.size]
            val rssi = -45 - (i * 3)
            results.add(scan(ssid, "aa:bb:cc:dd:%02x:%02x".format(i / 256, i % 256), rssi, ch, 20, "WPA2"))
            i++
        }
        // A few clean 5 GHz APs
        results.add(scan("CorpNet-5G", "a8:13:0b:30:05:01", -48, 5540, 80, "WPA2-Enterprise"))
        results.add(scan("CorpNet-5G", "a8:13:0b:30:05:02", -56, 5260, 80, "WPA2-Enterprise"))
        return results
    }

    // =========================================================================
    // WiFi Connection Info
    // =========================================================================

    fun connectionInfo(scenario: DemoScenario = DemoScenario.HEALTHY_ROAMING): WifiConnectionInfo =
        when (scenario) {
            DemoScenario.HEALTHY_ROAMING -> WifiConnectionInfo(
                ssid = "CorpNet-5G", bssid = "a8:13:0b:10:01:01",
                rssi = -42, linkSpeed = 866, frequency = 5540, ipAddress = "10.1.10.42",
            )
            DemoScenario.STICKY_CLIENT -> WifiConnectionInfo(
                ssid = "CorpNet-5G", bssid = "a8:13:0b:20:01:01",
                rssi = -78, linkSpeed = 72, frequency = 5540, ipAddress = "10.1.10.105",
            )
            DemoScenario.CHANNEL_CONGESTION -> WifiConnectionInfo(
                ssid = "CorpNet", bssid = "aa:bb:cc:dd:00:00",
                rssi = -45, linkSpeed = 54, frequency = 2412, ipAddress = "10.1.10.200",
            )
        }

    fun rssiHistory(scenario: DemoScenario = DemoScenario.HEALTHY_ROAMING): List<Pair<Long, Int>> {
        val now = System.currentTimeMillis()
        return when (scenario) {
            DemoScenario.HEALTHY_ROAMING -> (0 until 60).map { i ->
                // Stable around -42, minor fluctuation
                val rssi = -42 + (if (i % 7 == 0) -3 else if (i % 5 == 0) 2 else 0)
                (now - (60 - i) * 2000L) to rssi
            }
            DemoScenario.STICKY_CLIENT -> (0 until 60).map { i ->
                // Degrading signal: starts at -55, drops to -80
                val rssi = -55 - (i * 25 / 60) + (if (i % 4 == 0) 2 else -1)
                (now - (60 - i) * 2000L) to rssi
            }
            DemoScenario.CHANNEL_CONGESTION -> (0 until 60).map { i ->
                // Jittery: bounces between -40 and -65
                val rssi = -52 + (if (i % 3 == 0) 12 else if (i % 3 == 1) -13 else 0)
                (now - (60 - i) * 2000L) to rssi
            }
        }
    }

    // =========================================================================
    // Syslog Messages
    // =========================================================================

    fun syslogMessages(scenario: DemoScenario = DemoScenario.HEALTHY_ROAMING): List<SyslogMessage> =
        when (scenario) {
            DemoScenario.HEALTHY_ROAMING -> healthyRoamingSyslog()
            DemoScenario.STICKY_CLIENT -> stickyClientSyslog()
            DemoScenario.CHANNEL_CONGESTION -> congestionSyslog()
        }

    private fun healthyRoamingSyslog(): List<SyslogMessage> {
        val client = "aa:bb:cc:11:22:33"
        return listOf(
            syslog(0, 6, "WLC-9800", "<134>: %DOT11-6-ASSOC: Station $client Associated MAP AP-Floor2-East"),
            syslog(30, 6, "WLC-9800", "<134>: apfMsConnTask: CLIENT_ADDED_TO_RUN_STATE for $client AP name AP-Floor2-East channel 36 rssi -45"),
            syslog(180, 6, "WLC-9800", "<134>: %DOT11-6-ASSOC: Station $client Associated MAP AP-Floor2-West"),
            syslog(185, 6, "WLC-9800", "<134>: apfMsConnTask: CLIENT_ADDED_TO_RUN_STATE for $client AP name AP-Floor2-West channel 40 rssi -48"),
            syslog(400, 6, "WLC-9800", "<134>: %DOT11-6-ASSOC: Station $client Associated MAP AP-Floor3-North"),
            syslog(405, 6, "WLC-9800", "<134>: apfMsConnTask: CLIENT_ADDED_TO_RUN_STATE for $client AP name AP-Floor3-North channel 149 rssi -52"),
            syslog(600, 5, "WLC-9800", "<133>: %DOT11-5-NOTICE: Station $client signal strength -58 dBm on AP-Floor3-North"),
        )
    }

    private fun stickyClientSyslog(): List<SyslogMessage> {
        val client = "dd:ee:ff:44:55:66"
        return listOf(
            syslog(0, 6, "WLC-9800", "<134>: %DOT11-6-ASSOC: Station $client Associated MAP AP-Lobby"),
            syslog(5, 6, "WLC-9800", "<134>: apfMsConnTask: CLIENT_ADDED_TO_RUN_STATE for $client AP name AP-Lobby channel 36 rssi -48"),
            // Signal degrades but client won't roam
            syslog(120, 4, "WLC-9800", "<132>: %DOT11-4-WARNING: Station $client rssi (-72) below threshold on AP-Lobby"),
            syslog(180, 4, "WLC-9800", "<132>: %DOT11-4-WARNING: Station $client rssi (-78) below threshold on AP-Lobby"),
            syslog(240, 3, "WLC-9800", "<131>: %DOT11-3-DEAUTH: Station $client Deauthenticated AP-Lobby reason 4"),
            syslog(245, 6, "WLC-9800", "<134>: %DOT11-6-ASSOC: Station $client Associated MAP AP-Lobby"),
            syslog(250, 6, "WLC-9800", "<134>: apfMsConnTask: CLIENT_ADDED_TO_RUN_STATE for $client AP name AP-Lobby channel 36 rssi -80"),
            // Deauth again
            syslog(360, 3, "WLC-9800", "<131>: %DOT11-3-DEAUTH: Station $client Deauthenticated AP-Lobby reason 4"),
            syslog(365, 6, "WLC-9800", "<134>: %DOT11-6-ASSOC: Station $client Associated MAP AP-Conf-Room"),
            syslog(370, 6, "WLC-9800", "<134>: apfMsConnTask: CLIENT_ADDED_TO_RUN_STATE for $client AP name AP-Conf-Room channel 149 rssi -45"),
        )
    }

    private fun congestionSyslog(): List<SyslogMessage> {
        val clients = listOf("11:22:33:aa:bb:01", "11:22:33:aa:bb:02", "11:22:33:aa:bb:03")
        return listOf(
            syslog(0, 6, "WLC-9800", "<134>: %DOT11-6-ASSOC: Station ${clients[0]} Associated MAP AP-OpenFloor channel 1 rssi -48"),
            syslog(10, 6, "WLC-9800", "<134>: %DOT11-6-ASSOC: Station ${clients[1]} Associated MAP AP-OpenFloor channel 1 rssi -52"),
            syslog(20, 6, "WLC-9800", "<134>: %DOT11-6-ASSOC: Station ${clients[2]} Associated MAP AP-OpenFloor channel 1 rssi -55"),
            syslog(60, 4, "WLC-9800", "<132>: %DOT11-4-WARNING: High CCI detected on channel 1 AP-OpenFloor"),
            syslog(90, 6, "WLC-9800", "<134>: apfMsConnTask: CLIENT_ADDED_TO_RUN_STATE for ${clients[0]} AP name AP-Kitchen channel 6 rssi -58"),
            syslog(120, 4, "WLC-9800", "<132>: %DOT11-4-WARNING: Station ${clients[1]} Authentication failed AP-OpenFloor reason 15"),
            syslog(150, 3, "WLC-9800", "<131>: %DOT11-3-DEAUTH: Station ${clients[1]} Deauthenticated AP-OpenFloor reason 6"),
            syslog(155, 6, "WLC-9800", "<134>: %DOT11-6-ASSOC: Station ${clients[1]} Associated MAP AP-Kitchen channel 6 rssi -62"),
        )
    }

    // =========================================================================
    // Network Events (parsed, for Timeline)
    // =========================================================================

    fun networkEvents(scenario: DemoScenario = DemoScenario.HEALTHY_ROAMING): List<NetworkEvent> =
        when (scenario) {
            DemoScenario.HEALTHY_ROAMING -> healthyRoamingEvents()
            DemoScenario.STICKY_CLIENT -> stickyClientEvents()
            DemoScenario.CHANNEL_CONGESTION -> congestionEvents()
        }

    private fun healthyRoamingEvents(): List<NetworkEvent> {
        val client = "aa:bb:cc:11:22:33"
        return listOf(
            event(0, EventType.ASSOC, client, "AP-Floor2-East", 36, -45),
            event(180, EventType.ROAM, client, "AP-Floor2-West", 40, -48),
            event(400, EventType.ROAM, client, "AP-Floor3-North", 149, -52),
        )
    }

    private fun stickyClientEvents(): List<NetworkEvent> {
        val client = "dd:ee:ff:44:55:66"
        return listOf(
            event(0, EventType.ASSOC, client, "AP-Lobby", 36, -48),
            event(240, EventType.DEAUTH, client, "AP-Lobby", 36, -78, reason = 4),
            event(245, EventType.ASSOC, client, "AP-Lobby", 36, -80),
            event(360, EventType.DEAUTH, client, "AP-Lobby", 36, -82, reason = 4),
            event(365, EventType.ASSOC, client, "AP-Conf-Room", 149, -45),
        )
    }

    private fun congestionEvents(): List<NetworkEvent> {
        val c1 = "11:22:33:aa:bb:01"; val c2 = "11:22:33:aa:bb:02"; val c3 = "11:22:33:aa:bb:03"
        return listOf(
            event(0, EventType.ASSOC, c1, "AP-OpenFloor", 1, -48),
            event(10, EventType.ASSOC, c2, "AP-OpenFloor", 1, -52),
            event(20, EventType.ASSOC, c3, "AP-OpenFloor", 1, -55),
            event(90, EventType.ROAM, c1, "AP-Kitchen", 6, -58),
            event(120, EventType.AUTH, c2, "AP-OpenFloor", 1, -52),
            event(150, EventType.DEAUTH, c2, "AP-OpenFloor", 1, -52, reason = 6),
            event(155, EventType.ASSOC, c2, "AP-Kitchen", 6, -62),
        )
    }

    // =========================================================================
    // Log Import Sample
    // =========================================================================

    fun sampleLogBlock(): String = """
*apfMsConnTask: Mar 15 10:22:05.123: %DOT11-6-ASSOC: Station aa:bb:cc:11:22:33 Associated MAP AP-Floor2-East
*apfMsConnTask: Mar 15 10:22:05.456: CLIENT_ADDED_TO_RUN_STATE for aa:bb:cc:11:22:33 AP name AP-Floor2-East channel 36 rssi -45
*apfMsConnTask: Mar 15 10:25:12.789: %DOT11-6-ASSOC: Station aa:bb:cc:11:22:33 Associated MAP AP-Floor2-West
*apfMsConnTask: Mar 15 10:25:13.012: CLIENT_ADDED_TO_RUN_STATE for aa:bb:cc:11:22:33 AP name AP-Floor2-West channel 40 rssi -48
*apfMsConnTask: Mar 15 10:28:44.567: %DOT11-4-WARNING: Station dd:ee:ff:44:55:66 rssi (-78) below threshold on AP-Lobby
*apfMsConnTask: Mar 15 10:30:01.234: %DOT11-3-DEAUTH: Station dd:ee:ff:44:55:66 Deauthenticated AP-Lobby reason 4
*apfMsConnTask: Mar 15 10:30:05.678: %DOT11-6-ASSOC: Station dd:ee:ff:44:55:66 Associated MAP AP-Lobby
*apfMsConnTask: Mar 15 10:30:06.012: CLIENT_ADDED_TO_RUN_STATE for dd:ee:ff:44:55:66 AP name AP-Lobby channel 36 rssi -80
    """.trimIndent()

    // =========================================================================
    // Helpers
    // =========================================================================

    private fun scan(
        ssid: String, bssid: String, rssi: Int,
        frequency: Int, channelWidthMhz: Int, security: String,
    ) = WifiScanResult(
        ssid = ssid, bssid = bssid, rssi = rssi,
        frequency = frequency, channelWidth = channelWidthMhz,
        security = security, timestamp = baseTime,
    )

    private fun syslog(
        offsetSec: Int, severity: Int, hostname: String, message: String,
    ): SyslogMessage {
        val ts = baseTime + offsetSec * 1000L
        val priority = 16 * 8 + severity // facility=16 (local0)
        return SyslogMessage(
            priority = priority, facility = 16, severity = severity,
            hostname = hostname, message = message, raw = message,
            receivedAt = ts, id = UUID.nameUUIDFromBytes("demo-$offsetSec-$hostname".toByteArray()).toString(),
        )
    }

    private fun event(
        offsetSec: Int, type: EventType, clientMac: String,
        apName: String, channel: Int, rssi: Int, reason: Int? = null,
    ) = NetworkEvent(
        timestamp = baseTime + offsetSec * 1000L,
        eventType = type, clientMac = clientMac, apName = apName,
        channel = channel, rssi = rssi, reasonCode = reason,
        vendor = Vendor.CISCO,
        rawMessage = "Demo event: $type $clientMac on $apName ch$channel ${rssi}dBm",
        sessionId = DEMO_SESSION,
    )
}
