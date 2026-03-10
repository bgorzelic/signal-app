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

    @Test
    fun `parse DEAUTH event`() {
        val line = "*apfMsConnTask_3: Mar 09 10:30:00: %DOT11-4-DEAUTH: " +
            "Station aa:bb:cc:dd:ee:ff Deauthenticated reason 1"
        val event = parser.parse(line, "session-1")
        assertNotNull(event)
        assertEquals(EventType.DEAUTH, event!!.eventType)
        assertEquals("aa:bb:cc:dd:ee:ff", event.clientMac)
        assertEquals(1, event.reasonCode)
    }

    @Test
    fun `canParse returns true for wncd capwap lines`() {
        assertTrue(parser.canParse("wncd: some client message"))
        assertTrue(parser.canParse("capwap: AP joined"))
        assertFalse(parser.canParse("random line with no cisco indicators"))
    }

    @Test
    fun `parse extracts rssi with parentheses`() {
        val line = "*apfMsConnTask_6: %CLIENT_ORCH_LOG-6-CLIENT_ADDED_TO_RUN_STATE: " +
            "wncd: client aa:bb:cc:dd:ee:ff joined with ssid CorpWiFi " +
            "AP name (AP-Test) rssi (-75) channel (149)"
        val event = parser.parse(line, "s1")
        assertNotNull(event)
        assertEquals(-75, event!!.rssi)
        assertEquals(149, event.channel)
        assertEquals("AP-Test", event.apName)
    }

    @Test
    fun `returns null when cisco indicator present but no event type matches`() {
        val line = "wncd: some informational message about radio resource management"
        val event = parser.parse(line, "s1")
        assertNull(event)
    }

    @Test
    fun `session ID is propagated to event`() {
        val line = "*apfMsConnTask_0: %DOT11-6-ASSOC: Station aa:bb:cc:dd:ee:ff Associated MAP AP-1 slot 0"
        val event = parser.parse(line, "my-session-42")
        assertNotNull(event)
        assertEquals("my-session-42", event!!.sessionId)
    }

    @Test
    fun `MAC address extraction is case insensitive`() {
        val line = "*apfMsConnTask_0: %DOT11-6-ASSOC: Station AA:BB:CC:DD:EE:FF Associated MAP AP-1 slot 0"
        val event = parser.parse(line, "s1")
        assertNotNull(event)
        assertEquals("AA:BB:CC:DD:EE:FF", event!!.clientMac)
    }
}
