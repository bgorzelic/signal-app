package dev.aiaerial.signal.data.parser

import dev.aiaerial.signal.data.model.EventType
import dev.aiaerial.signal.data.model.Vendor
import org.junit.Assert.*
import org.junit.Test

class MerakiParserTest {

    private val parser = MerakiParser()

    @Test
    fun `parse association event`() {
        val line = "1 1678400000.123456 MR-Floor2 events type=association radio=1 vap=0 " +
            "channel=36 rssi=28 aid=1234567890 mac=AA:BB:CC:DD:EE:FF"
        val event = parser.parse(line, "session-1")
        assertNotNull(event)
        assertEquals(EventType.ASSOC, event!!.eventType)
        assertEquals("AA:BB:CC:DD:EE:FF", event.clientMac)
        assertEquals("MR-Floor2", event.apName)
        assertEquals(36, event.channel)
        assertEquals(28, event.rssi)
        assertEquals(Vendor.MERAKI, event.vendor)
    }

    @Test
    fun `parse disassociation with reason`() {
        val line = "1 1678400010.654321 MR-Floor2 events type=disassociation radio=1 vap=0 " +
            "channel=36 aid=1234567890 mac=AA:BB:CC:DD:EE:FF reason=8"
        val event = parser.parse(line, "session-1")
        assertNotNull(event)
        assertEquals(EventType.DISASSOC, event!!.eventType)
        assertEquals(8, event.reasonCode)
    }

    @Test
    fun `parse reassociation as roam`() {
        val line = "1 1678400005.111111 MR-Floor3 events type=reassociation radio=0 vap=0 " +
            "channel=149 rssi=22 mac=AA:BB:CC:DD:EE:FF"
        val event = parser.parse(line, "session-1")
        assertNotNull(event)
        assertEquals(EventType.ROAM, event!!.eventType)
        assertEquals("MR-Floor3", event.apName)
        assertEquals(149, event.channel)
    }

    @Test
    fun `parse 802_1X auth`() {
        val line = "1 1678400020.111111 MR-Lobby events type=8021x_auth identity=user@corp " +
            "mac=AA:BB:CC:DD:EE:FF"
        val event = parser.parse(line, "session-1")
        assertNotNull(event)
        assertEquals(EventType.AUTH, event!!.eventType)
        assertEquals("AA:BB:CC:DD:EE:FF", event.clientMac)
    }

    @Test
    fun `parse 802_1X deauth`() {
        val line = "1 1678400030.222222 MR-Lobby events type=8021x_deauth identity=user@corp " +
            "mac=AA:BB:CC:DD:EE:FF"
        val event = parser.parse(line, "session-1")
        assertNotNull(event)
        assertEquals(EventType.DEAUTH, event!!.eventType)
    }

    @Test
    fun `parse splash auth`() {
        val line = "1 1678400040.333333 MR-Guest events type=splash_auth " +
            "mac=11:22:33:44:55:66"
        val event = parser.parse(line, "session-1")
        assertNotNull(event)
        assertEquals(EventType.AUTH, event!!.eventType)
    }

    @Test
    fun `return null for non-Meraki syslog`() {
        val line = "<134>Mar 10 12:00:00 switch: %SYS-5-CONFIG_I: Configured from console"
        assertNull(parser.parse(line, "session-1"))
    }

    @Test
    fun `return null for unknown Meraki event type`() {
        val line = "1 1678400000.000000 MR-Floor1 events type=flows src=10.0.0.1 dst=10.0.0.2"
        assertNull(parser.parse(line, "session-1"))
    }

    @Test
    fun `canParse detects Meraki event patterns`() {
        assertTrue(parser.canParse("events type=association radio=1"))
        assertTrue(parser.canParse("type=disassociation mac=AA:BB:CC:DD:EE:FF"))
        assertTrue(parser.canParse("type=8021x_auth identity=user"))
        assertFalse(parser.canParse("wncd: cisco message"))
        assertFalse(parser.canParse("stm[1234]: aruba message"))
    }

    @Test
    fun `session ID is propagated`() {
        val line = "1 1678400000.123456 MR-Test events type=association " +
            "channel=1 mac=AA:BB:CC:DD:EE:FF"
        val event = parser.parse(line, "test-session-77")
        assertNotNull(event)
        assertEquals("test-session-77", event!!.sessionId)
    }

    @Test
    fun `AP name extracted from hostname before events keyword`() {
        val line = "1 1678400000.123456 AP-Conference-Room events type=association " +
            "channel=6 mac=AA:BB:CC:DD:EE:FF"
        val event = parser.parse(line, "s1")
        assertNotNull(event)
        assertEquals("AP-Conference-Room", event!!.apName)
    }
}
