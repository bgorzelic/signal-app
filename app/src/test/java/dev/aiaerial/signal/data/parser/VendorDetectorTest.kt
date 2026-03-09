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
    fun `returns null for unknown vendor`() {
        val line = "some random log line that is not from any known vendor"
        assertNull(detector.parse(line, "s1"))
    }
}
