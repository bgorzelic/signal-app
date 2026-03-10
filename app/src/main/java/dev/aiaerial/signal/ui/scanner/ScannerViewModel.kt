package dev.aiaerial.signal.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.aiaerial.signal.data.wifi.ChannelUtilization
import dev.aiaerial.signal.data.wifi.WifiConnectionInfo
import dev.aiaerial.signal.data.wifi.WifiScanResult
import dev.aiaerial.signal.data.wifi.WifiScanner
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val wifiScanner: WifiScanner,
) : ViewModel() {

    private val _scanResults = MutableStateFlow<List<WifiScanResult>>(emptyList())
    val scanResults: StateFlow<List<WifiScanResult>> = _scanResults.asStateFlow()

    private val _channelUtilization = MutableStateFlow<List<ChannelUtilization>>(emptyList())
    val channelUtilization: StateFlow<List<ChannelUtilization>> = _channelUtilization.asStateFlow()

    private val _connectionInfo = MutableStateFlow<WifiConnectionInfo?>(null)
    val connectionInfo: StateFlow<WifiConnectionInfo?> = _connectionInfo.asStateFlow()

    private val rssiRingBuffer = ArrayDeque<Pair<Long, Int>>(61)
    private val _rssiHistory = MutableStateFlow<List<Pair<Long, Int>>>(emptyList())
    val rssiHistory: StateFlow<List<Pair<Long, Int>>> = _rssiHistory.asStateFlow()

    private val smoothedRingBuffer = ArrayDeque<Pair<Long, Int>>(61)
    private val _smoothedRssiHistory = MutableStateFlow<List<Pair<Long, Int>>>(emptyList())
    val smoothedRssiHistory: StateFlow<List<Pair<Long, Int>>> = _smoothedRssiHistory.asStateFlow()

    private var emaValue: Double? = null

    init {
        collectScanResults()
        pollConnectionInfo()
    }

    fun triggerScan() {
        wifiScanner.triggerScan()
    }

    private fun collectScanResults() {
        viewModelScope.launch {
            wifiScanner.scanResults().collect { results ->
                _scanResults.value = results.sortedByDescending { it.rssi }
                _channelUtilization.value = ChannelUtilization.fromScanResults(results)
            }
        }
    }

    private fun pollConnectionInfo() {
        viewModelScope.launch {
            while (currentCoroutineContext().isActive) {
                val info = wifiScanner.connectionInfo()
                _connectionInfo.value = info
                if (info != null) {
                    val now = System.currentTimeMillis()

                    // Raw RSSI
                    rssiRingBuffer.addLast(now to info.rssi)
                    if (rssiRingBuffer.size > 60) rssiRingBuffer.removeFirst()
                    _rssiHistory.value = rssiRingBuffer.toList()

                    // EMA-smoothed RSSI (alpha = 0.3 balances responsiveness and smoothing)
                    val smoothed = emaSmooth(info.rssi.toDouble())
                    smoothedRingBuffer.addLast(now to smoothed.toInt())
                    if (smoothedRingBuffer.size > 60) smoothedRingBuffer.removeFirst()
                    _smoothedRssiHistory.value = smoothedRingBuffer.toList()
                }
                delay(2_000L)
            }
        }
    }

    /**
     * Exponential Moving Average smoothing.
     * Alpha = 0.3: reacts to real changes within 3-4 samples while filtering jitter.
     * Lower alpha = smoother but slower response. Higher = more responsive but noisier.
     */
    private fun emaSmooth(rawRssi: Double): Double {
        val current = emaValue
        return if (current == null) {
            emaValue = rawRssi
            rawRssi
        } else {
            val smoothed = EMA_ALPHA * rawRssi + (1 - EMA_ALPHA) * current
            emaValue = smoothed
            smoothed
        }
    }

    companion object {
        const val EMA_ALPHA = 0.3
    }
}
