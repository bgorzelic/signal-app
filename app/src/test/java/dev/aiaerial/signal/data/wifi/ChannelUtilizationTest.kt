package dev.aiaerial.signal.data.wifi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelUtilizationTest {

    private fun scanResult(
        frequency: Int,
        rssi: Int,
        bssid: String = "00:11:22:33:44:55",
    ) = WifiScanResult(
        ssid = "TestNet",
        bssid = bssid,
        rssi = rssi,
        frequency = frequency,
        channelWidth = 0,
        security = "WPA2",
        timestamp = System.currentTimeMillis(),
    )

    @Test
    fun `empty scan results produce empty utilization`() {
        val result = ChannelUtilization.fromScanResults(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `single AP on channel 6 is LOW congestion`() {
        val results = listOf(scanResult(frequency = 2437, rssi = -50, bssid = "aa:bb:cc:dd:ee:01"))
        val util = ChannelUtilization.fromScanResults(results)

        assertEquals(1, util.size)
        assertEquals(6, util[0].channel)
        assertEquals("2.4 GHz", util[0].band)
        assertEquals(1, util[0].apCount)
        assertEquals(-50, util[0].strongestRssi)
        assertEquals(ChannelUtilization.CongestionLevel.LOW, util[0].congestionLevel)
    }

    @Test
    fun `two APs with strong signal on same channel is HIGH`() {
        val results = listOf(
            scanResult(frequency = 2437, rssi = -45, bssid = "aa:bb:cc:dd:ee:01"),
            scanResult(frequency = 2437, rssi = -55, bssid = "aa:bb:cc:dd:ee:02"),
        )
        val util = ChannelUtilization.fromScanResults(results)

        assertEquals(1, util.size)
        assertEquals(2, util[0].apCount)
        assertEquals(-45, util[0].strongestRssi)
        assertEquals(ChannelUtilization.CongestionLevel.HIGH, util[0].congestionLevel)
    }

    @Test
    fun `two APs with weak signal on same channel is MEDIUM`() {
        val results = listOf(
            scanResult(frequency = 2437, rssi = -75, bssid = "aa:bb:cc:dd:ee:01"),
            scanResult(frequency = 2437, rssi = -80, bssid = "aa:bb:cc:dd:ee:02"),
        )
        val util = ChannelUtilization.fromScanResults(results)

        assertEquals(1, util.size)
        assertEquals(ChannelUtilization.CongestionLevel.MEDIUM, util[0].congestionLevel)
    }

    @Test
    fun `four or more APs on same channel is always HIGH`() {
        val results = (1..5).map { i ->
            scanResult(frequency = 2437, rssi = -85, bssid = "aa:bb:cc:dd:ee:%02x".format(i))
        }
        val util = ChannelUtilization.fromScanResults(results)

        assertEquals(1, util.size)
        assertEquals(5, util[0].apCount)
        assertEquals(ChannelUtilization.CongestionLevel.HIGH, util[0].congestionLevel)
    }

    @Test
    fun `multiple channels sorted by band then channel number`() {
        val results = listOf(
            scanResult(frequency = 5180, rssi = -60, bssid = "aa:bb:cc:dd:ee:01"), // 5 GHz ch 36
            scanResult(frequency = 2412, rssi = -70, bssid = "aa:bb:cc:dd:ee:02"), // 2.4 GHz ch 1
            scanResult(frequency = 2437, rssi = -65, bssid = "aa:bb:cc:dd:ee:03"), // 2.4 GHz ch 6
            scanResult(frequency = 5240, rssi = -55, bssid = "aa:bb:cc:dd:ee:04"), // 5 GHz ch 48
        )
        val util = ChannelUtilization.fromScanResults(results)

        assertEquals(4, util.size)
        assertEquals("2.4 GHz", util[0].band)
        assertEquals(1, util[0].channel)
        assertEquals("2.4 GHz", util[1].band)
        assertEquals(6, util[1].channel)
        assertEquals("5 GHz", util[2].band)
        assertEquals(36, util[2].channel)
        assertEquals("5 GHz", util[3].band)
        assertEquals(48, util[3].channel)
    }

    @Test
    fun `average rssi is computed correctly`() {
        val results = listOf(
            scanResult(frequency = 2437, rssi = -40, bssid = "aa:bb:cc:dd:ee:01"),
            scanResult(frequency = 2437, rssi = -60, bssid = "aa:bb:cc:dd:ee:02"),
        )
        val util = ChannelUtilization.fromScanResults(results)

        assertEquals(-50, util[0].averageRssi)
    }

    @Test
    fun `results with channel 0 are filtered out`() {
        val results = listOf(
            scanResult(frequency = 999, rssi = -50, bssid = "aa:bb:cc:dd:ee:01"), // unknown freq -> ch 0
        )
        val util = ChannelUtilization.fromScanResults(results)
        assertTrue(util.isEmpty())
    }
}
