package dev.aiaerial.signal.data.export

import dev.aiaerial.signal.data.model.EventType
import dev.aiaerial.signal.data.model.NetworkEvent
import dev.aiaerial.signal.data.model.Vendor
import org.junit.Assert.*
import org.junit.Test

class SessionExporterTest {

    @Test
    fun `export events as CSV`() {
        val events = listOf(
            NetworkEvent(
                id = 1, timestamp = 1710000000000L, eventType = EventType.ROAM,
                clientMac = "aa:bb:cc:dd:ee:ff", apName = "AP-1", channel = 36,
                rssi = -65, vendor = Vendor.CISCO, rawMessage = "raw1", sessionId = "s1"
            ),
            NetworkEvent(
                id = 2, timestamp = 1710000001000L, eventType = EventType.DEAUTH,
                clientMac = "aa:bb:cc:dd:ee:ff", apName = "AP-2", reasonCode = 8,
                vendor = Vendor.CISCO, rawMessage = "raw2", sessionId = "s1"
            ),
        )
        val csv = SessionExporter.toCsv(events)
        assertTrue(csv.startsWith("timestamp,event_type,client_mac,ap_name,bssid,channel,rssi,reason_code,vendor"))
        assertTrue(csv.contains("ROAM,aa:bb:cc:dd:ee:ff,AP-1"))
        assertTrue(csv.contains("DEAUTH,aa:bb:cc:dd:ee:ff,AP-2"))
        assertEquals(3, csv.lines().size) // header + 2 events
    }

    @Test
    fun `export events as JSON`() {
        val events = listOf(
            NetworkEvent(
                id = 1, timestamp = 1710000000000L, eventType = EventType.ROAM,
                clientMac = "aa:bb:cc:dd:ee:ff", apName = "AP-1",
                vendor = Vendor.CISCO, rawMessage = "raw1", sessionId = "s1"
            ),
        )
        val json = SessionExporter.toJson(events)
        assertTrue(json.contains("\"eventType\": \"ROAM\""))
        assertTrue(json.contains("\"clientMac\": \"aa:bb:cc:dd:ee:ff\""))
    }
}
