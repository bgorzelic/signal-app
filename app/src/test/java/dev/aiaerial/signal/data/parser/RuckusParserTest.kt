package dev.aiaerial.signal.data.parser

import dev.aiaerial.signal.data.model.EventType
import dev.aiaerial.signal.data.model.Vendor
import org.junit.Assert.*
import org.junit.Test

class RuckusParserTest {

    private val parser = RuckusParser()
    private val session = "test-session"

    @Test
    fun `parses STA-ASSOC event`() {
        val line = "ruckus: STA-ASSOC aa:bb:cc:dd:ee:ff on AP[AP-Lobby] ssid[CorpNet] channel[36] rssi[-45]"
        val event = parser.parse(line, session)
        assertNotNull(event)
        assertEquals(EventType.ASSOC, event!!.eventType)
        assertEquals("aa:bb:cc:dd:ee:ff", event.clientMac)
        assertEquals("AP-Lobby", event.apName)
        assertEquals(36, event.channel)
        assertEquals(-45, event.rssi)
        assertEquals(Vendor.RUCKUS, event.vendor)
    }

    @Test
    fun `parses STA-ROAM event with target AP`() {
        val line = "ruckus: STA-ROAM aa:bb:cc:dd:ee:ff from AP[AP-Floor1] to AP[AP-Floor2] channel[149] rssi[-52]"
        val event = parser.parse(line, session)
        assertNotNull(event)
        assertEquals(EventType.ROAM, event!!.eventType)
        assertEquals("AP-Floor2", event.apName) // should pick "to" AP
        assertEquals(149, event.channel)
        assertEquals(-52, event.rssi)
    }

    @Test
    fun `parses STA-LEAVE as DISASSOC`() {
        val line = "ruckus: STA-LEAVE aa:bb:cc:dd:ee:ff on AP[AP-Lobby] reason[1]"
        val event = parser.parse(line, session)
        assertNotNull(event)
        assertEquals(EventType.DISASSOC, event!!.eventType)
        assertEquals(1, event.reasonCode)
    }

    @Test
    fun `parses STA-DEAUTH event`() {
        val line = "ruckus: STA-DEAUTH aa:bb:cc:dd:ee:ff on AP[AP-Lobby] reason[4]"
        val event = parser.parse(line, session)
        assertNotNull(event)
        assertEquals(EventType.DEAUTH, event!!.eventType)
        assertEquals(4, event.reasonCode)
    }

    @Test
    fun `parses STA-AUTH-FAIL`() {
        val line = "ruckus: STA-AUTH-FAIL aa:bb:cc:dd:ee:ff on AP[AP-Floor2] reason[auth-timeout]"
        val event = parser.parse(line, session)
        assertNotNull(event)
        assertEquals(EventType.AUTH, event!!.eventType)
        assertNull(event.reasonCode) // "auth-timeout" is not numeric
    }

    @Test
    fun `canParse detects ruckus indicator`() {
        assertTrue(parser.canParse("ruckus: STA-ASSOC test"))
        assertTrue(parser.canParse("STA-ROAM aa:bb:cc:dd:ee:ff"))
        assertFalse(parser.canParse("apfMsConnTask: cisco stuff"))
    }

    @Test
    fun `rejects non-ruckus syslog`() {
        assertNull(parser.parse("random log message about wifi", session))
    }

    @Test
    fun `propagates session ID`() {
        val line = "ruckus: STA-ASSOC aa:bb:cc:dd:ee:ff on AP[AP-Test] channel[1]"
        val event = parser.parse(line, "my-session-123")
        assertEquals("my-session-123", event!!.sessionId)
    }
}
