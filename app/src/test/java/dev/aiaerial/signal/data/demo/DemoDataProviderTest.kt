package dev.aiaerial.signal.data.demo

import org.junit.Assert.*
import org.junit.Test

class DemoDataProviderTest {

    @Test
    fun `healthy roaming produces scan results`() {
        val results = DemoDataProvider.wifiScanResults(DemoScenario.HEALTHY_ROAMING)
        assertTrue(results.isNotEmpty())
        assertTrue(results.size >= 5)
        assertTrue(results.all { it.ssid.isNotBlank() })
    }

    @Test
    fun `all scenarios produce non-empty data`() {
        DemoScenario.entries.forEach { scenario ->
            assertTrue("$scenario scans", DemoDataProvider.wifiScanResults(scenario).isNotEmpty())
            assertTrue("$scenario syslog", DemoDataProvider.syslogMessages(scenario).isNotEmpty())
            assertTrue("$scenario events", DemoDataProvider.networkEvents(scenario).isNotEmpty())
            assertNotNull("$scenario connection", DemoDataProvider.connectionInfo(scenario))
            assertTrue("$scenario history", DemoDataProvider.rssiHistory(scenario).isNotEmpty())
        }
    }

    @Test
    fun `syslog messages have valid severity`() {
        DemoScenario.entries.forEach { scenario ->
            DemoDataProvider.syslogMessages(scenario).forEach { msg ->
                assertTrue("severity in range", msg.severity in 0..7)
                assertTrue("has id", msg.id.isNotBlank())
            }
        }
    }

    @Test
    fun `network events have session ID`() {
        val events = DemoDataProvider.networkEvents(DemoScenario.STICKY_CLIENT)
        assertTrue(events.all { it.sessionId == "demo-session-001" })
    }

    @Test
    fun `sticky client scenario has deauth events`() {
        val events = DemoDataProvider.networkEvents(DemoScenario.STICKY_CLIENT)
        assertTrue(events.any { it.eventType.name == "DEAUTH" })
    }

    @Test
    fun `sample log block is parseable text`() {
        val log = DemoDataProvider.sampleLogBlock()
        assertTrue(log.isNotBlank())
        assertTrue(log.contains("apfMsConnTask") || log.contains("DOT11"))
    }

    @Test
    fun `rssi history has 60 data points`() {
        DemoScenario.entries.forEach { scenario ->
            assertEquals(60, DemoDataProvider.rssiHistory(scenario).size)
        }
    }
}
