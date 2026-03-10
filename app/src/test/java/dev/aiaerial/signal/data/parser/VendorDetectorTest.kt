package dev.aiaerial.signal.data.parser

import dev.aiaerial.signal.data.model.Vendor
import org.junit.Assert.*
import org.junit.Test

class VendorDetectorTest {

    private val detector = VendorDetector()

    @Test
    fun `detects Cisco and routes to CiscoWlcParser`() {
        val line = "*apfMsConnTask_6: Jun 15 14:23:45: %DOT11-6-ASSOC: " +
            "Station aa:bb:cc:dd:ee:ff Associated MAP AP-Lobby slot 1"
        val event = detector.parse(line, "s1")
        assertNotNull(event)
        assertEquals(Vendor.CISCO, event!!.vendor)
    }

    @Test
    fun `detects Aruba and routes to ArubaParser`() {
        val line = "<134>Mar 10 14:00:01 mc-aruba stm[1234]: <501051> Station aa:bb:cc:dd:ee:ff " +
            "Associated to AP ap-floor2 BSSID 00:11:22:33:44:55 SSID CorpWiFi channel 36"
        val event = detector.parse(line, "s1")
        assertNotNull(event)
        assertEquals(Vendor.ARUBA, event!!.vendor)
    }

    @Test
    fun `returns null for unknown vendor`() {
        val line = "some random log line that is not from any known vendor"
        assertNull(detector.parse(line, "s1"))
    }
}
