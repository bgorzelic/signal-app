package dev.aiaerial.signal.data.parser

import dev.aiaerial.signal.data.model.EventType
import dev.aiaerial.signal.data.model.Vendor
import org.junit.Assert.*
import org.junit.Test

class CiscoWlcParserTest {

    private val parser = CiscoWlcParser()

    @Test
    fun `parse 9800 client roam event`() {
        val line = "*apfMsConnTask_6: Jun 15 14:23:45.123: %CLIENT_ORCH_LOG-6-CLIENT_ADDED_TO_RUN_STATE: " +
            "R0/0: wncd: Username entry (aa:bb:cc:dd:ee:ff) joined with ssid (CorpWiFi) " +
            "for device with AP name (AP-Floor3-East) AP mac (00:11:22:33:44:55) " +
            "channel (36) rssi (-62)"
        val event = parser.parse(line, "session-1")
        assertNotNull(event)
        assertEquals(EventType.ROAM, event!!.eventType)
        assertEquals("aa:bb:cc:dd:ee:ff", event.clientMac)
        assertEquals("AP-Floor3-East", event.apName)
        assertEquals(36, event.channel)
        assertEquals(-62, event.rssi)
        assertEquals(Vendor.CISCO, event.vendor)
    }

    @Test
    fun `parse AireOS client association`() {
        val line = "*apfMsConnTask_0: Mar 09 10:15:30.456: %DOT11-6-ASSOC: " +
            "Station aa:bb:cc:dd:ee:ff Associated MAP AP-Lobby slot 1"
        val event = parser.parse(line, "session-1")
        assertNotNull(event)
        assertEquals(EventType.ASSOC, event!!.eventType)
        assertEquals("aa:bb:cc:dd:ee:ff", event.clientMac)
        assertEquals("AP-Lobby", event.apName)
    }

    @Test
    fun `parse client deauthentication`() {
        val line = "*apfMsConnTask_2: Mar 09 10:20:00.789: %DOT11-6-DISASSOC: " +
            "Station aa:bb:cc:dd:ee:ff Disassociated MAP AP-Floor2 reason 8"
        val event = parser.parse(line, "session-1")
        assertNotNull(event)
        assertEquals(EventType.DISASSOC, event!!.eventType)
        assertEquals(8, event.reasonCode)
    }

    @Test
    fun `parse 802_1X auth failure`() {
        val line = "*emWeb: Mar 09 11:00:00.000: %DOT1X-3-AUTH_FAIL: " +
            "Authentication failed for client aa:bb:cc:dd:ee:ff reason TIMEOUT"
        val event = parser.parse(line, "session-1")
        assertNotNull(event)
        assertEquals(EventType.AUTH, event!!.eventType)
        assertEquals("aa:bb:cc:dd:ee:ff", event.clientMac)
    }

    @Test
    fun `return null for non-wifi syslog`() {
        val line = "<134>Mar 09 12:00:00 switch: %SYS-5-CONFIG_I: Configured from console"
        val event = parser.parse(line, "session-1")
        assertNull(event)
    }
}
