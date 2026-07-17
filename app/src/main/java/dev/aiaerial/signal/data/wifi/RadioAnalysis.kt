package dev.aiaerial.signal.data.wifi

import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Fast, deterministic RF assessment that remains useful with no AI connection. */
data class RadioAnalysis(
    val healthScore: Int,
    val networkCount: Int,
    val ssidCount: Int,
    val medianRssi: Int,
    val rssiStdDev: Int,
    val strongNetworkCount: Int,
    val openNetworkCount: Int,
    val highCongestionChannelCount: Int,
    val bandCounts: Map<String, Int>,
    val findings: List<String>,
) {
    val grade: String
        get() = when {
            healthScore >= 90 -> "EXCELLENT"
            healthScore >= 75 -> "GOOD"
            healthScore >= 55 -> "DEGRADED"
            else -> "CRITICAL"
        }

    companion object {
        fun from(
            results: List<WifiScanResult>,
            channels: List<ChannelUtilization> = ChannelUtilization.fromScanResults(results),
        ): RadioAnalysis? {
            if (results.isEmpty()) return null

            val orderedRssi = results.map { it.rssi }.sorted()
            val median = percentile(orderedRssi, 0.5)
            val mean = orderedRssi.average()
            val deviation = sqrt(orderedRssi.map { (it - mean) * (it - mean) }.average()).roundToInt()
            val open = results.count { result ->
                val security = result.security.uppercase()
                security.isBlank() || security == "OPEN" || security == "NONE" || security == "[ESS]"
            }
            val strong = results.count { it.rssi >= -60 }
            val congested = channels.filter { it.congestionLevel == ChannelUtilization.CongestionLevel.HIGH }
            val bandCounts = results.groupingBy { it.band }.eachCount()

            var score = 100
            score -= (congested.size * 8).coerceAtMost(32)
            score -= (open * 5).coerceAtMost(20)
            if (median < -75) score -= 18 else if (median < -67) score -= 8
            if (deviation >= 18) score -= 10
            if (results.count { it.band == "2.4 GHz" } > results.size * 0.6) score -= 8

            val findings = buildList {
                if (congested.isNotEmpty()) {
                    add("High co-channel contention on ${congested.joinToString { "${it.band} ch ${it.channel}" }}.")
                }
                if (median < -67) add("Median observed signal is weak at $median dBm.")
                if (deviation >= 18) add("RSSI spread is wide (σ $deviation dB); coverage is uneven.")
                if (open > 0) add("$open open network${if (open == 1) "" else "s"} detected.")
                if ((bandCounts["6 GHz"] ?: 0) == 0) add("No 6 GHz cells were visible in this scan.")
                if (isEmpty()) add("No immediate RF risk crossed the local thresholds.")
            }

            return RadioAnalysis(
                healthScore = score.coerceIn(0, 100),
                networkCount = results.size,
                ssidCount = results.map { it.ssid }.filter { it.isNotBlank() }.toSet().size,
                medianRssi = median,
                rssiStdDev = deviation,
                strongNetworkCount = strong,
                openNetworkCount = open,
                highCongestionChannelCount = congested.size,
                bandCounts = bandCounts,
                findings = findings,
            )
        }

        private fun percentile(sorted: List<Int>, fraction: Double): Int {
            if (sorted.size == 1) return sorted.first()
            val position = fraction.coerceIn(0.0, 1.0) * (sorted.size - 1)
            val lower = position.toInt()
            val upper = (lower + 1).coerceAtMost(sorted.lastIndex)
            val weight = position - lower
            return (sorted[lower] * (1 - weight) + sorted[upper] * weight).roundToInt()
        }
    }
}
