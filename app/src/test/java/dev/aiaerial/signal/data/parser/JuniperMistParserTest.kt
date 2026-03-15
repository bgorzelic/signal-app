package dev.aiaerial.signal.data.parser

import dev.aiaerial.signal.data.model.EventType
import dev.aiaerial.signal.data.model.Vendor
import org.junit.Assert.*
import org.junit.Test

class JuniperMistParserTest {

    private val parser = JuniperMistParser()
    private val session = "test-session"

    @Test
    fun `parses client_assoc event`() {
        val line = "mist: event_type=client_assoc client_mac=aa:bb:cc:dd:ee:ff ap_name=AP-Floor1 channel=36 rssi=-48"
        val event = parser.parse(line, session)
        assertNotNull(event)
        assertEquals(EventType.ASSOC, event!!.eventType)
        assertEquals("aa:bb:cc:dd:ee:ff", event.clientMac)
        assertEquals("AP-Floor1", event.apName)
        assertEquals(36, event.channel)
        assertEquals(-48, event.rssi)
        assertEquals(Vendor.JUNIPER, event.vendor)
    }

    @Test
    fun `parses client_roam event`() {
        val line = "mist: event_type=client_roam client_mac=aa:bb:cc:dd:ee:ff ap_name=AP-Floor2 channel=149 rssi=-52"
        val event = parser.parse(line, session)
        assertNotNull(event)
        assertEquals(EventType.ROAM, event!!.eventType)
        assertEquals("AP-Floor2", event.apName)
        assertEquals(149, event.channel)
    }

    @Test
    fun `parses client_deauth with reason`() {
        val line = "mist: event_type=client_deauth client_mac=aa:bb:cc:dd:ee:ff ap_name=AP-Floor1 reason=4"
        val event = parser.parse(line, session)
        assertNotNull(event)
        assertEquals(EventType.DEAUTH, event!!.eventType)
        assertEquals(4, event.reasonCode)
    }

    @Test
    fun `parses client_auth_fail`() {
        val line = "mist: event_type=client_auth_fail client_mac=aa:bb:cc:dd:ee:ff ap_name=AP-Floor2"
        val event = parser.parse(line, session)
        assertNotNull(event)
        assertEquals(EventType.AUTH, event!!.eventType)
    }

    @Test
    fun `parses client_disassoc`() {
        val line = "mist: event_type=client_disassoc client_mac=11:22:33:44:55:66 ap_name=AP-Lobby reason=8"
        val event = parser.parse(line, session)
        assertNotNull(event)
        assertEquals(EventType.DISASSOC, event!!.eventType)
        assertEquals(8, event.reasonCode)
    }

    @Test
    fun `canParse detects mist indicators`() {
        assertTrue(parser.canParse("mist: event_type=client_assoc"))
        assertTrue(parser.canParse("event_type=client_roam client_mac=aa:bb"))
        assertTrue(parser.canParse("juniper something auth"))
        assertFalse(parser.canParse("apfMsConnTask: cisco stuff"))
    }

    @Test
    fun `rejects non-juniper syslog`() {
        assertNull(parser.parse("random log message", session))
    }

    @Test
    fun `propagates session ID`() {
        val line = "mist: event_type=client_assoc client_mac=aa:bb:cc:dd:ee:ff ap_name=AP-1"
        val event = parser.parse(line, "sess-456")
        assertEquals("sess-456", event!!.sessionId)
    }
}
