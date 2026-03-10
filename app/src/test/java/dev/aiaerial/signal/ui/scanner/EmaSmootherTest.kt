package dev.aiaerial.signal.ui.scanner

import org.junit.Assert.*
import org.junit.Test

class EmaSmootherTest {

    @Test
    fun `first value passes through unchanged`() {
        val result = applyEma(listOf(-65.0), alpha = 0.3)
        assertEquals(1, result.size)
        assertEquals(-65.0, result[0], 0.001)
    }

    @Test
    fun `EMA smooths jittery values`() {
        // Raw RSSI with jitter: -65, -72, -63, -71, -64
        val raw = listOf(-65.0, -72.0, -63.0, -71.0, -64.0)
        val smoothed = applyEma(raw, alpha = 0.3)

        // Smoothed values should have less variance than raw
        val rawVariance = variance(raw)
        val smoothedVariance = variance(smoothed)
        assertTrue("Smoothed variance ($smoothedVariance) should be less than raw ($rawVariance)",
            smoothedVariance < rawVariance)
    }

    @Test
    fun `EMA converges toward new stable value`() {
        // Signal drops from -50 to -70 and stays
        val raw = listOf(-50.0, -70.0, -70.0, -70.0, -70.0, -70.0, -70.0, -70.0, -70.0, -70.0)
        val smoothed = applyEma(raw, alpha = 0.3)

        // After 10 samples, EMA should be close to -70
        assertTrue("EMA should converge to -70, got ${smoothed.last()}",
            kotlin.math.abs(smoothed.last() - (-70.0)) < 1.0)
    }

    @Test
    fun `higher alpha is more responsive`() {
        val raw = listOf(-50.0, -70.0, -70.0, -70.0, -70.0)
        val slowEma = applyEma(raw, alpha = 0.1)
        val fastEma = applyEma(raw, alpha = 0.5)

        // After the step change, fast EMA should be closer to -70
        assertTrue("Fast EMA (${fastEma[1]}) should drop more than slow (${slowEma[1]})",
            fastEma[1] < slowEma[1])
    }

    @Test
    fun `alpha 0_3 balances responsiveness and smoothing`() {
        // Simulate real RSSI: stable with occasional spikes
        val raw = listOf(-65.0, -64.0, -66.0, -45.0, -65.0, -66.0, -64.0)
        val smoothed = applyEma(raw, alpha = ScannerViewModel.EMA_ALPHA)

        // The spike at index 3 (-45) should be dampened
        assertTrue("Spike should be dampened: smoothed=${smoothed[3]}, raw=${raw[3]}",
            smoothed[3] < raw[3]) // smoothed should be more negative (lower) than -45
    }

    /** Apply EMA to a series of values. Mirrors ScannerViewModel logic. */
    private fun applyEma(values: List<Double>, alpha: Double): List<Double> {
        val result = mutableListOf<Double>()
        var ema: Double? = null
        for (v in values) {
            ema = if (ema == null) v else alpha * v + (1 - alpha) * ema
            result.add(ema)
        }
        return result
    }

    private fun variance(values: List<Double>): Double {
        val mean = values.average()
        return values.sumOf { (it - mean) * (it - mean) } / values.size
    }
}
