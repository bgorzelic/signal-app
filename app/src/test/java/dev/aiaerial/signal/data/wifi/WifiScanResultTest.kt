package dev.aiaerial.signal.data.wifi

import org.junit.Assert.assertEquals
import org.junit.Test

class WifiScanResultTest {

    private fun result(frequency: Int) = WifiScanResult(
        ssid = "TestNetwork",
        bssid = "AA:BB:CC:DD:EE:FF",
        rssi = -55,
        frequency = frequency,
        channelWidth = 20,
        security = "WPA2",
        timestamp = System.currentTimeMillis(),
    )

    // --- Channel derivation ---

    @Test
    fun `channel derivation for 2_4 GHz - 2437 MHz is channel 6`() {
        assertEquals(6, result(2437).channel)
    }

    @Test
    fun `channel derivation for 2_4 GHz - 2412 MHz is channel 1`() {
        assertEquals(1, result(2412).channel)
    }

    @Test
    fun `channel derivation for 5 GHz - 5180 MHz is channel 36`() {
        assertEquals(36, result(5180).channel)
    }

    @Test
    fun `channel derivation for 5 GHz - 5745 MHz is channel 149`() {
        assertEquals(149, result(5745).channel)
    }

    @Test
    fun `channel derivation for 6 GHz - 5955 MHz is channel 1`() {
        assertEquals(1, result(5955).channel)
    }

    @Test
    fun `channel derivation for 6 GHz - 5975 MHz is channel 5`() {
        assertEquals(5, result(5975).channel)
    }

    // --- Band string ---

    @Test
    fun `band string for 2_4 GHz frequency`() {
        assertEquals("2.4 GHz", result(2437).band)
    }

    @Test
    fun `band string for 5 GHz frequency`() {
        assertEquals("5 GHz", result(5180).band)
    }

    @Test
    fun `band string for 6 GHz frequency`() {
        assertEquals("6 GHz", result(5955).band)
    }

    @Test
    fun `band string for unknown frequency`() {
        assertEquals("Unknown", result(900).band)
    }
}
