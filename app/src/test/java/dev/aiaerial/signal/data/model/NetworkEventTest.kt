package dev.aiaerial.signal.data.model

import org.junit.Assert.*
import org.junit.Test

class NetworkEventTest {

    @Test
    fun `create network event with all fields`() {
        val event = NetworkEvent(
            timestamp = 1710000000000L,
            eventType = EventType.ROAM,
            clientMac = "AA:BB:CC:DD:EE:FF",
            apName = "AP-Floor3-East",
            bssid = "00:11:22:33:44:55",
            channel = 36,
            rssi = -65,
            reasonCode = 0,
            vendor = Vendor.CISCO,
            rawMessage = "<134>Mar 9 12:00:00 wlc: *apfMsConnTask: ...client roamed",
            sessionId = "session-001"
        )
        assertEquals(EventType.ROAM, event.eventType)
        assertEquals("AA:BB:CC:DD:EE:FF", event.clientMac)
        assertEquals(Vendor.CISCO, event.vendor)
    }

    @Test
    fun `create network event with nullable fields`() {
        val event = NetworkEvent(
            timestamp = 1710000000000L,
            eventType = EventType.UNKNOWN,
            rawMessage = "some unparsed syslog line",
            sessionId = "session-001"
        )
        assertNull(event.clientMac)
        assertNull(event.apName)
        assertNull(event.bssid)
        assertNull(event.channel)
        assertNull(event.rssi)
        assertEquals(EventType.UNKNOWN, event.eventType)
    }
}
