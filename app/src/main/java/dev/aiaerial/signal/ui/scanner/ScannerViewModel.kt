package dev.aiaerial.signal.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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

    private val _connectionInfo = MutableStateFlow<WifiConnectionInfo?>(null)
    val connectionInfo: StateFlow<WifiConnectionInfo?> = _connectionInfo.asStateFlow()

    private val rssiRingBuffer = ArrayDeque<Pair<Long, Int>>(61)
    private val _rssiHistory = MutableStateFlow<List<Pair<Long, Int>>>(emptyList())
    val rssiHistory: StateFlow<List<Pair<Long, Int>>> = _rssiHistory.asStateFlow()

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
            }
        }
    }

    private fun pollConnectionInfo() {
        viewModelScope.launch {
            while (currentCoroutineContext().isActive) {
                val info = wifiScanner.connectionInfo()
                _connectionInfo.value = info
                if (info != null) {
                    rssiRingBuffer.addLast(System.currentTimeMillis() to info.rssi)
                    if (rssiRingBuffer.size > 60) rssiRingBuffer.removeFirst()
                    _rssiHistory.value = rssiRingBuffer.toList()
                }
                delay(2_000L)
            }
        }
    }
}
