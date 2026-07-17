package dev.aiaerial.signal.data.wifi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RadioAnalysisTest {
    @Test
    fun emptyScanHasNoAssessment() {
        assertEquals(null, RadioAnalysis.from(emptyList()))
    }

    @Test
    fun congestedWeakScanProducesActionableStatistics() {
        val results = listOf(
            ap("field", "00:00:00:00:00:01", -82, 2412, "WPA2"),
            ap("field", "00:00:00:00:00:02", -76, 2412, "WPA2"),
            ap("guest", "00:00:00:00:00:03", -71, 2412, "OPEN"),
            ap("iot", "00:00:00:00:00:04", -65, 2412, "WPA2"),
        )

        val analysis = RadioAnalysis.from(results)
        assertNotNull(analysis)
        analysis!!
        assertEquals(4, analysis.networkCount)
        assertEquals(3, analysis.ssidCount)
        assertEquals(1, analysis.openNetworkCount)
        assertEquals(1, analysis.highCongestionChannelCount)
        assertTrue(analysis.healthScore < 75)
        assertTrue(analysis.findings.any { it.contains("co-channel") })
    }

    private fun ap(ssid: String, bssid: String, rssi: Int, frequency: Int, security: String) =
        WifiScanResult(ssid, bssid, rssi, frequency, 0, security, 0L)
}
