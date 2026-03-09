package dev.aiaerial.signal.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.aiaerial.signal.data.wifi.WifiConnectionInfo
import dev.aiaerial.signal.data.wifi.WifiScanResult
import dev.aiaerial.signal.data.wifi.WifiScanner
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
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
            while (true) {
                val info = wifiScanner.connectionInfo()
                _connectionInfo.value = info
                if (info != null) {
                    val history = _rssiHistory.value.toMutableList()
                    history.add(System.currentTimeMillis() to info.rssi)
                    // Keep last 60 data points
                    if (history.size > 60) {
                        _rssiHistory.value = history.takeLast(60)
                    } else {
                        _rssiHistory.value = history
                    }
                }
                delay(2_000L)
            }
        }
    }
}
