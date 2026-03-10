package dev.aiaerial.signal.data.wifi

/**
 * Per-channel congestion estimate derived from WiFi scan results.
 *
 * @param channel WiFi channel number (1-14, 36-165, 1-233)
 * @param band Human-readable band (2.4 GHz, 5 GHz, 6 GHz)
 * @param apCount Number of APs detected on this channel
 * @param strongestRssi Strongest AP signal on this channel (dBm)
 * @param averageRssi Average AP signal on this channel (dBm)
 * @param congestionLevel Qualitative congestion: Low, Medium, High
 */
data class ChannelUtilization(
    val channel: Int,
    val band: String,
    val apCount: Int,
    val strongestRssi: Int,
    val averageRssi: Int,
    val congestionLevel: CongestionLevel,
) {
    enum class CongestionLevel { LOW, MEDIUM, HIGH }

    companion object {
        /**
         * Compute channel utilization from scan results.
         * Groups APs by channel, counts them, and estimates congestion.
         */
        fun fromScanResults(results: List<WifiScanResult>): List<ChannelUtilization> {
            return results
                .filter { it.channel > 0 }
                .groupBy { it.channel }
                .map { (channel, aps) ->
                    val strongest = aps.maxOf { it.rssi }
                    val average = aps.map { it.rssi }.average().toInt()
                    val band = aps.first().band
                    val congestion = estimateCongestion(aps.size, strongest)

                    ChannelUtilization(
                        channel = channel,
                        band = band,
                        apCount = aps.size,
                        strongestRssi = strongest,
                        averageRssi = average,
                        congestionLevel = congestion,
                    )
                }
                .sortedWith(compareBy({ bandOrder(it.band) }, { it.channel }))
        }

        /**
         * Heuristic congestion estimation.
         * High: 4+ APs on same channel, or 2+ with strong signal (co-channel interference).
         * Medium: 2-3 APs, or 1 very strong AP that dominates the channel.
         * Low: 1 AP or only weak signals.
         */
        private fun estimateCongestion(apCount: Int, strongestRssi: Int): CongestionLevel = when {
            apCount >= 4 -> CongestionLevel.HIGH
            apCount >= 2 && strongestRssi >= -60 -> CongestionLevel.HIGH
            apCount >= 2 -> CongestionLevel.MEDIUM
            else -> CongestionLevel.LOW
        }

        private fun bandOrder(band: String): Int = when (band) {
            "2.4 GHz" -> 0
            "5 GHz" -> 1
            "6 GHz" -> 2
            else -> 3
        }
    }
}
