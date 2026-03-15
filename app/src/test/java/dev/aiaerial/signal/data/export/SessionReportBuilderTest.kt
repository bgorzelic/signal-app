package dev.aiaerial.signal.data.export

import dev.aiaerial.signal.data.alert.Alert
import dev.aiaerial.signal.data.alert.AlertSeverity
import dev.aiaerial.signal.data.alert.AlertType
import dev.aiaerial.signal.data.model.EventType
import dev.aiaerial.signal.data.model.NetworkEvent
import dev.aiaerial.signal.data.model.Vendor
import org.junit.Assert.*
import org.junit.Test

class SessionReportBuilderTest {

    private fun event(
        type: EventType, clientMac: String = "aa:bb:cc:dd:ee:ff",
        apName: String = "AP-1", rssi: Int = -50, offsetSec: Int = 0,
    ) = NetworkEvent(
        timestamp = 1710000000000L + offsetSec * 1000L,
        eventType = type, clientMac = clientMac, apName = apName,
        rssi = rssi, vendor = Vendor.CISCO,
        rawMessage = "test event", sessionId = "test-session",
    )

    @Test
    fun `report contains session metadata`() {
        val events = listOf(event(EventType.ASSOC), event(EventType.ROAM, offsetSec = 60))
        val report = SessionReportBuilder.buildMarkdownReport("test-session", events)
        assertTrue(report.contains("# SIGNAL Session Report"))
        assertTrue(report.contains("test-session"))
        assertTrue(report.contains("Total events:** 2"))
    }

    @Test
    fun `report includes event type summary`() {
        val events = listOf(
            event(EventType.ASSOC),
            event(EventType.ROAM, offsetSec = 30),
            event(EventType.ROAM, offsetSec = 60),
        )
        val report = SessionReportBuilder.buildMarkdownReport("s1", events)
        assertTrue(report.contains("ROAM"))
        assertTrue(report.contains("ASSOC"))
    }

    @Test
    fun `report includes client summary`() {
        val events = listOf(
            event(EventType.ASSOC, clientMac = "aa:bb:cc:11:22:33"),
            event(EventType.ROAM, clientMac = "aa:bb:cc:11:22:33", offsetSec = 60),
        )
        val report = SessionReportBuilder.buildMarkdownReport("s1", events)
        assertTrue(report.contains("aa:bb:cc:11:22:33"))
        assertTrue(report.contains("Clients"))
    }

    @Test
    fun `report includes alerts when provided`() {
        val events = listOf(event(EventType.DEAUTH))
        val alerts = listOf(
            Alert(AlertType.WEAK_SIGNAL, AlertSeverity.WARNING, "Weak signal", "detail")
        )
        val report = SessionReportBuilder.buildMarkdownReport("s1", events, alerts)
        assertTrue(report.contains("[WARNING]"))
        assertTrue(report.contains("Weak signal"))
    }

    @Test
    fun `report includes AI summary when provided`() {
        val events = listOf(event(EventType.ASSOC))
        val report = SessionReportBuilder.buildMarkdownReport("s1", events, aiSummary = "Everything looks healthy.")
        assertTrue(report.contains("Everything looks healthy."))
    }

    @Test
    fun `empty events produce minimal report`() {
        val report = SessionReportBuilder.buildMarkdownReport("s1", emptyList())
        assertTrue(report.contains("Total events:** 0"))
    }
}
