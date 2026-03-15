package dev.aiaerial.signal.data.alert

import dev.aiaerial.signal.data.model.EventType
import dev.aiaerial.signal.data.model.NetworkEvent
import dev.aiaerial.signal.data.model.Vendor
import dev.aiaerial.signal.data.wifi.WifiScanResult
import org.junit.Assert.*
import org.junit.Test

class AlertEngineTest {

    private fun event(
        type: EventType, rssi: Int? = null, clientMac: String? = "aa:bb:cc:dd:ee:ff",
        apName: String? = "AP-1", channel: Int? = 36, reason: Int? = null,
        offsetSec: Int = 0,
    ) = NetworkEvent(
        timestamp = 1000000L + offsetSec * 1000L,
        eventType = type, clientMac = clientMac, apName = apName,
        channel = channel, rssi = rssi, reasonCode = reason,
        vendor = Vendor.CISCO, rawMessage = "test", sessionId = "test-session",
    )

    private fun scan(rssi: Int, frequency: Int = 5540, channel: Int = 108) = WifiScanResult(
        ssid = "Test", bssid = "aa:bb:cc:dd:ee:ff", rssi = rssi,
        frequency = frequency, channelWidth = 80, security = "WPA2", timestamp = 0,
    )

    @Test
    fun `detects weak signal events`() {
        val events = listOf(
            event(EventType.ASSOC, rssi = -75),
            event(EventType.ROAM, rssi = -82),
        )
        val alerts = AlertEngine.analyzeEvents(events, rssiThreshold = -70)
        assertTrue(alerts.any { it.type == AlertType.WEAK_SIGNAL })
        val alert = alerts.first { it.type == AlertType.WEAK_SIGNAL }
        assertEquals(AlertSeverity.WARNING, alert.severity) // -82 is 12 below -70, needs 15+ for CRITICAL
    }

    @Test
    fun `no weak signal alert when all above threshold`() {
        val events = listOf(
            event(EventType.ASSOC, rssi = -45),
            event(EventType.ROAM, rssi = -55),
        )
        val alerts = AlertEngine.analyzeEvents(events, rssiThreshold = -70)
        assertFalse(alerts.any { it.type == AlertType.WEAK_SIGNAL })
    }

    @Test
    fun `detects auth failure storm`() {
        val events = listOf(
            event(EventType.DEAUTH, reason = 4, offsetSec = 0),
            event(EventType.DEAUTH, reason = 4, offsetSec = 60),
            event(EventType.DEAUTH, reason = 4, offsetSec = 120),
        )
        val alerts = AlertEngine.analyzeEvents(events, authFailureCount = 3)
        assertTrue(alerts.any { it.type == AlertType.AUTH_FAILURE_STORM })
    }

    @Test
    fun `detects RSSI degradation`() {
        val events = listOf(
            event(EventType.ASSOC, rssi = -45, offsetSec = 0),
            event(EventType.ROAM, rssi = -55, offsetSec = 60),
            event(EventType.ROAM, rssi = -70, offsetSec = 120),
        )
        val alerts = AlertEngine.analyzeEvents(events)
        assertTrue(alerts.any { it.type == AlertType.RSSI_DEGRADATION })
    }

    @Test
    fun `detects channel congestion`() {
        val scans = (1..6).map { scan(rssi = -50 - it, frequency = 2437) }
        val alerts = AlertEngine.analyzeCongestion(scans)
        assertTrue(alerts.any { it.type == AlertType.CHANNEL_CONGESTION })
    }

    @Test
    fun `no congestion alert for sparse channels`() {
        val scans = listOf(scan(rssi = -60, frequency = 5540))
        val alerts = AlertEngine.analyzeCongestion(scans)
        assertTrue(alerts.isEmpty())
    }

    @Test
    fun `empty events produce no alerts`() {
        val alerts = AlertEngine.analyzeEvents(emptyList())
        assertTrue(alerts.isEmpty())
    }
}
