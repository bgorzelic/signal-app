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
    }
}
