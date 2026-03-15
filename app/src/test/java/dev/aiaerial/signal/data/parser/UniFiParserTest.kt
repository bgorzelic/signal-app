package dev.aiaerial.signal.data.parser

import dev.aiaerial.signal.data.model.EventType
import org.junit.Assert.*
import org.junit.Test

class UniFiParserTest {

    private val parser = UniFiParser()
    private val session = "test-session"

    @Test
    fun `parses hostapd association`() {
        val line = "AP1 BZ2,f0:9f:c2:aa:bb:cc,v6.6.55: hostapd: ath0: STA aa:bb:cc:dd:ee:ff IEEE 802.11: associated"
        val event = parser.parse(line, session)
        assertNotNull(event)
        assertEquals(EventType.ASSOC, event!!.eventType)
        assertEquals("aa:bb:cc:dd:ee:ff", event.clientMac)
    }

    @Test
    fun `parses deauthentication`() {
        val line = "AP1 kernel: [12345.678] wlan0: STA aa:bb:cc:dd:ee:ff IEEE 802.11: deauthenticated reason 3"
        val event = parser.parse(line, session)
        assertNotNull(event)
        assertEquals(EventType.DEAUTH, event!!.eventType)
        assertEquals(3, event.reasonCode)
    }

    @Test
    fun `parses UniFi OS format with AP name`() {
        val line = "[UAP-AC-Pro] STA aa:bb:cc:dd:ee:ff associated to SSID \"CorpNet\" on channel 36"
        val event = parser.parse(line, session)
        assertNotNull(event)
        assertEquals(EventType.ASSOC, event!!.eventType)
        assertEquals("UAP-AC-Pro", event.apName)
        assertEquals(36, event.channel)
    }

    @Test
    fun `parses disassociation with reason`() {
        val line = "[USW-Pro] STA 11:22:33:44:55:66 disassociated from SSID \"CorpNet\" reason 8"
        val event = parser.parse(line, session)
        assertNotNull(event)
        assertEquals(EventType.DISASSOC, event!!.eventType)
        assertEquals(8, event.reasonCode)
    }

    @Test
    fun `parses reassociation as roam`() {
        val line = "[UAP-AC-LR] STA aa:bb:cc:dd:ee:ff reassociated to SSID \"Guest\" channel 149"
        val event = parser.parse(line, session)
        assertNotNull(event)
        assertEquals(EventType.ROAM, event!!.eventType)
        assertEquals(149, event.channel)
    }

    @Test
    fun `canParse detects UniFi indicators`() {
        assertTrue(parser.canParse("hostapd: STA aa:bb:cc:dd:ee:ff associated"))
        assertTrue(parser.canParse("[UAP-AC-Pro] STA aa:bb:cc:dd:ee:ff deauthenticated"))
        assertTrue(parser.canParse("BZ2,f0:9f:c2:aa:bb:cc: STA 11:22:33:44:55:66 associated"))
        assertFalse(parser.canParse("apfMsConnTask: cisco stuff"))
    }

    @Test
    fun `rejects non-UniFi syslog`() {
        assertNull(parser.parse("random log message about wifi", session))
    }

    @Test
    fun `extracts STA MAC not AP MAC`() {
        // AP MAC is f0:9f:c2:aa:bb:cc, STA MAC is 11:22:33:44:55:66
        val line = "AP1 BZ2,f0:9f:c2:aa:bb:cc,v6.6.55: hostapd: STA 11:22:33:44:55:66 IEEE 802.11: associated"
        val event = parser.parse(line, session)
        assertNotNull(event)
        assertEquals("11:22:33:44:55:66", event!!.clientMac)
    }

    @Test
    fun `propagates session ID`() {
        val line = "[UAP-AC-Pro] STA aa:bb:cc:dd:ee:ff associated channel 36"
        val event = parser.parse(line, "my-session")
        assertEquals("my-session", event!!.sessionId)
    }
}
