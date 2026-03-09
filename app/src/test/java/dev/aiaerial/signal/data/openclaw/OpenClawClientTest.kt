package dev.aiaerial.signal.data.openclaw

import dev.aiaerial.signal.data.model.EventType
import dev.aiaerial.signal.data.model.NetworkEvent
import dev.aiaerial.signal.data.model.Vendor
import org.junit.Assert.*
import org.junit.Test

class OpenClawClientTest {

    @Test
    fun `buildTriagePrompt includes event details`() {
        val event = NetworkEvent(
            timestamp = 1710000000000L,
            eventType = EventType.DEAUTH,
            clientMac = "aa:bb:cc:dd:ee:ff",
            apName = "AP-Floor3",
            channel = 36,
            rssi = -75,
            reasonCode = 8,
            vendor = Vendor.CISCO,
            rawMessage = "original syslog line here",
            sessionId = "s1"
        )
        val prompt = OpenClawClient.buildTriagePrompt(event)
        assertTrue(prompt.contains("DEAUTH"))
        assertTrue(prompt.contains("aa:bb:cc:dd:ee:ff"))
        assertTrue(prompt.contains("AP-Floor3"))
        assertTrue(prompt.contains("reason code 8"))
        assertTrue(prompt.contains("-75 dBm"))
    }

    @Test
    fun `buildTriagePrompt asks for root cause`() {
        val event = NetworkEvent(
            timestamp = 0L,
            eventType = EventType.ROAM,
            rawMessage = "test",
            sessionId = "s1"
        )
        val prompt = OpenClawClient.buildTriagePrompt(event)
        assertTrue(prompt.contains("root cause") || prompt.contains("explain"))
    }
}
