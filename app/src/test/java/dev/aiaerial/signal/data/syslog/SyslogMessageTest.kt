package dev.aiaerial.signal.data.syslog

import org.junit.Assert.*
import org.junit.Test

class SyslogMessageTest {

    @Test
    fun `parse RFC 3164 syslog message`() {
        val raw = "<134>Mar  9 12:00:00 wlc-9800 apfMsConnTask: Client AA:BB:CC:DD:EE:FF associated to AP-Lobby"
        val msg = SyslogMessage.parse(raw)
        assertEquals(134, msg.priority)
        assertEquals(16, msg.facility) // 134 / 8
        assertEquals(6, msg.severity) // 134 % 8
        assertEquals("wlc-9800", msg.hostname)
        assertTrue(msg.message.contains("Client AA:BB:CC:DD:EE:FF"))
    }

    @Test
    fun `parse message without priority`() {
        val raw = "Mar  9 12:00:00 meraki-ap some event happened"
        val msg = SyslogMessage.parse(raw)
        assertEquals(-1, msg.priority)
        assertEquals(raw, msg.raw)
    }

    @Test
    fun `severity level string`() {
        assertEquals("info", SyslogMessage.severityName(6))
        assertEquals("warning", SyslogMessage.severityName(4))
        assertEquals("error", SyslogMessage.severityName(3))
        assertEquals("critical", SyslogMessage.severityName(2))
        assertEquals("emergency", SyslogMessage.severityName(0))
        assertEquals("alert", SyslogMessage.severityName(1))
        assertEquals("notice", SyslogMessage.severityName(5))
        assertEquals("debug", SyslogMessage.severityName(7))
        assertEquals("unknown", SyslogMessage.severityName(-1))
    }

    @Test
    fun `parse high severity message`() {
        val raw = "<27>Mar  9 12:00:00 wlc-9800 %DOT11-3-DEAUTH: Station aa:bb:cc:dd:ee:ff"
        val msg = SyslogMessage.parse(raw)
        assertEquals(27, msg.priority)
        assertEquals(3, msg.facility) // 27 / 8
        assertEquals(3, msg.severity) // 27 % 8 = error
        assertEquals("error", msg.severityLabel)
    }

    @Test
    fun `parse empty message body`() {
        val msg = SyslogMessage.parse("")
        assertEquals(-1, msg.priority)
        assertNull(msg.hostname)
        assertEquals("", msg.message)
    }

    @Test
    fun `raw field preserved exactly`() {
        val raw = "<134>Mar  9 12:00:00 host msg with <special> chars & stuff"
        val msg = SyslogMessage.parse(raw)
        assertEquals(raw, msg.raw)
    }
}
