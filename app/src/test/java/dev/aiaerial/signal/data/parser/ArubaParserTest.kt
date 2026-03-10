package dev.aiaerial.signal.data.parser

import dev.aiaerial.signal.data.model.EventType
import dev.aiaerial.signal.data.model.Vendor
import org.junit.Assert.*
import org.junit.Test

class ArubaParserTest {

    private val parser = ArubaParser()

    @Test
    fun `parse station association`() {
        val line = "<134>Mar 10 14:00:01 mc-aruba stm[1234]: <501051> Station aa:bb:cc:dd:ee:ff " +
            "Associated to AP ap-floor2 BSSID 00:11:22:33:44:55 SSID CorpWiFi channel 36"
        val event = parser.parse(line, "session-1")
        assertNotNull(event)
        assertEquals(EventType.ASSOC, event!!.eventType)
        assertEquals("aa:bb:cc:dd:ee:ff", event.clientMac)
        assertEquals("ap-floor2", event.apName)
        assertEquals(36, event.channel)
        assertEquals(Vendor.ARUBA, event.vendor)
    }

    @Test
    fun `parse station roam`() {
        val line = "<134>Mar 10 14:01:00 mc-aruba stm[1234]: <501058> Station aa:bb:cc:dd:ee:ff " +
            "Roamed to AP ap-floor3 BSSID 00:11:22:33:44:99 channel 149"
        val event = parser.parse(line, "session-1")
        assertNotNull(event)
        assertEquals(EventType.ROAM, event!!.eventType)
        assertEquals("aa:bb:cc:dd:ee:ff", event.clientMac)
        assertEquals("ap-floor3", event.apName)
        assertEquals(149, event.channel)
    }

    @Test
    fun `parse station deauth with reason`() {
        val line = "<134>Mar 10 14:02:00 mc-aruba stm[1234]: <501080> Station aa:bb:cc:dd:ee:ff " +
            "Deauthenticated from AP ap-lobby reason 1"
        val event = parser.parse(line, "session-1")
        assertNotNull(event)
        assertEquals(EventType.DEAUTH, event!!.eventType)
        assertEquals("ap-lobby", event.apName)
        assertEquals(1, event.reasonCode)
    }

    @Test
    fun `parse station disassociation with reason`() {
        val line = "<134>Mar 10 14:03:00 mc-aruba stm[1234]: <501093> Station aa:bb:cc:dd:ee:ff " +
            "Disassociated from AP ap-floor1 reason 8"
        val event = parser.parse(line, "session-1")
        assertNotNull(event)
        assertEquals(EventType.DISASSOC, event!!.eventType)
        assertEquals("ap-floor1", event.apName)
        assertEquals(8, event.reasonCode)
    }

    @Test
    fun `parse auth failure`() {
        val line = "<134>Mar 10 14:04:00 mc-aruba authmgr[5678]: <522006> User Authentication Failed: " +
            "MAC=aa:bb:cc:dd:ee:ff reason=timeout"
        val event = parser.parse(line, "session-1")
        assertNotNull(event)
        assertEquals(EventType.AUTH, event!!.eventType)
        assertEquals("aa:bb:cc:dd:ee:ff", event.clientMac)
    }

    @Test
    fun `parse auth success`() {
        val line = "<134>Mar 10 14:05:00 mc-aruba authmgr[5678]: <522008> User Authentication Successful: " +
            "MAC=aa:bb:cc:dd:ee:ff"
        val event = parser.parse(line, "session-1")
        assertNotNull(event)
        assertEquals(EventType.AUTH, event!!.eventType)
        assertEquals("aa:bb:cc:dd:ee:ff", event.clientMac)
    }

    @Test
    fun `return null for non-Aruba syslog`() {
        val line = "<134>Mar 10 12:00:00 switch: %SYS-5-CONFIG_I: Configured from console"
        val event = parser.parse(line, "session-1")
        assertNull(event)
    }

    @Test
    fun `return null for Aruba indicator but no event type`() {
        val line = "<134>Mar 10 12:00:00 mc-aruba stm[1234]: <501000> Radio status update"
        val event = parser.parse(line, "session-1")
        assertNull(event)
    }

    @Test
    fun `canParse detects stm and authmgr indicators`() {
        assertTrue(parser.canParse("stm[1234]: some station message"))
        assertTrue(parser.canParse("authmgr[5678]: auth event"))
        assertTrue(parser.canParse("sapd[9999]: AP event"))
        assertFalse(parser.canParse("wncd: cisco message"))
        assertFalse(parser.canParse("random syslog line"))
    }

    @Test
    fun `MAC extraction handles dot-separated format`() {
        val line = "<134>Mar 10 14:00:01 mc-aruba stm[1234]: <501051> Station aabb.ccdd.eeff " +
            "Associated to AP ap-test channel 6"
        val event = parser.parse(line, "session-1")
        assertNotNull(event)
        assertEquals("aa:bb:cc:dd:ee:ff", event!!.clientMac)
    }

    @Test
    fun `session ID is propagated`() {
        val line = "<134>Mar 10 14:00:01 mc-aruba stm[1234]: <501051> Station aa:bb:cc:dd:ee:ff " +
            "Associated to AP ap-test channel 1"
        val event = parser.parse(line, "my-session-99")
        assertNotNull(event)
        assertEquals("my-session-99", event!!.sessionId)
    }

    @Test
    fun `parse extracts RSSI when present`() {
        val line = "<134>Mar 10 14:00:01 mc-aruba stm[1234]: <501058> Station aa:bb:cc:dd:ee:ff " +
            "Roamed to AP ap-floor3 channel 149 rssi -68"
        val event = parser.parse(line, "session-1")
        assertNotNull(event)
        assertEquals(-68, event!!.rssi)
    }
}
