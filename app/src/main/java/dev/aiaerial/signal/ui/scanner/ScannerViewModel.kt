package dev.aiaerial.signal.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.aiaerial.signal.data.alert.Alert
import dev.aiaerial.signal.data.alert.AlertEngine
import dev.aiaerial.signal.data.demo.DemoDataProvider
import dev.aiaerial.signal.data.demo.DemoScenario
import dev.aiaerial.signal.data.local.ScanSnapshot
import dev.aiaerial.signal.data.local.ScanSnapshotDao
import dev.aiaerial.signal.data.prefs.SignalPreferences
import dev.aiaerial.signal.data.wifi.ChannelUtilization
import dev.aiaerial.signal.data.wifi.ScanSnapshotSerializer
import dev.aiaerial.signal.data.wifi.SpeedTest
import dev.aiaerial.signal.data.wifi.WifiConnectionInfo
import dev.aiaerial.signal.data.wifi.WifiScanResult
import dev.aiaerial.signal.data.wifi.WifiScanner
import kotlinx.coroutines.Job
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
    private val prefs: SignalPreferences,
    private val snapshotDao: ScanSnapshotDao,
    private val speedTest: SpeedTest,
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

    private val _alerts = MutableStateFlow<List<Alert>>(emptyList())
    val alerts: StateFlow<List<Alert>> = _alerts.asStateFlow()

    private val _autoScanEnabled = MutableStateFlow(false)
    val autoScanEnabled: StateFlow<Boolean> = _autoScanEnabled.asStateFlow()

    private val _autoScanIntervalSec = MutableStateFlow(10)
    val autoScanIntervalSec: StateFlow<Int> = _autoScanIntervalSec.asStateFlow()

    private val _savedSnapshots = MutableStateFlow<List<ScanSnapshot>>(emptyList())
    val savedSnapshots: StateFlow<List<ScanSnapshot>> = _savedSnapshots.asStateFlow()

    private val _snapshotSaved = MutableStateFlow(false)
    val snapshotSaved: StateFlow<Boolean> = _snapshotSaved.asStateFlow()

    private val _speedTestResult = MutableStateFlow<SpeedTest.Result?>(null)
    val speedTestResult: StateFlow<SpeedTest.Result?> = _speedTestResult.asStateFlow()

    private val _isTestingSpeed = MutableStateFlow(false)
    val isTestingSpeed: StateFlow<Boolean> = _isTestingSpeed.asStateFlow()

    private var emaValue: Double? = null
    private var autoScanJob: Job? = null

    init {
        if (prefs.demoMode) {
            loadDemoData()
        } else {
            collectScanResults()
            pollConnectionInfo()
        }
        loadSnapshots()
    }

    fun triggerScan() {
        if (prefs.demoMode) {
            loadDemoData() // refresh demo data
        } else {
            wifiScanner.triggerScan()
        }
    }

    fun setAutoScan(enabled: Boolean) {
        _autoScanEnabled.value = enabled
        if (enabled) {
            startAutoScan()
        } else {
            stopAutoScan()
        }
    }

    fun setAutoScanInterval(sec: Int) {
        _autoScanIntervalSec.value = sec
        // Restart the auto-scan loop so the new interval takes effect immediately
        if (_autoScanEnabled.value) {
            startAutoScan()
        }
    }

    private fun startAutoScan() {
        // Guard: never auto-scan in demo mode
        if (prefs.demoMode) return
        autoScanJob?.cancel()
        autoScanJob = viewModelScope.launch {
            while (isActive) {
                triggerScan()
                delay(_autoScanIntervalSec.value * 1_000L)
            }
        }
    }

    private fun stopAutoScan() {
        autoScanJob?.cancel()
        autoScanJob = null
    }

    fun runSpeedTest() {
        if (_isTestingSpeed.value) return
        viewModelScope.launch {
            _isTestingSpeed.value = true
            _speedTestResult.value = null
            _speedTestResult.value = speedTest.runDownloadTest()
            _isTestingSpeed.value = false
        }
    }

    fun saveSnapshot(label: String = "") {
        viewModelScope.launch {
            val results = _scanResults.value
            if (results.isEmpty()) return@launch
            val info = _connectionInfo.value
            val autoLabel = label.ifBlank {
                val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
                    .format(java.util.Date())
                "${info?.ssid ?: "Scan"} @ $time"
            }
            val snapshot = ScanSnapshot(
                timestamp = System.currentTimeMillis(),
                label = autoLabel,
                ssid = info?.ssid,
                bssid = info?.bssid,
                rssi = info?.rssi,
                networkCount = results.size,
                dataJson = ScanSnapshotSerializer.serialize(results),
            )
            snapshotDao.insert(snapshot)
            _snapshotSaved.value = true
            delay(2000)
            _snapshotSaved.value = false
            loadSnapshots()
        }
    }

    fun loadSnapshots() {
        viewModelScope.launch {
            snapshotDao.getRecent(20).collect { _savedSnapshots.value = it }
        }
    }

    fun deleteSnapshot(id: Long) {
        viewModelScope.launch {
            snapshotDao.delete(id)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAutoScan()
    }

    private fun loadDemoData() {
        val scenario = DemoScenario.entries.getOrElse(prefs.demoScenarioIndex) { DemoScenario.HEALTHY_ROAMING }
        val results = DemoDataProvider.wifiScanResults(scenario)
        _scanResults.value = results.sortedByDescending { it.rssi }
        _channelUtilization.value = ChannelUtilization.fromScanResults(results)
        _connectionInfo.value = DemoDataProvider.connectionInfo(scenario)
        _rssiHistory.value = DemoDataProvider.rssiHistory(scenario)
        // Simple smoothing of demo data
        _smoothedRssiHistory.value = _rssiHistory.value.runningFold(null as Pair<Long, Int>?) { prev, (ts, rssi) ->
            val smoothed = if (prev == null) rssi else ((0.3 * rssi + 0.7 * prev.second).toInt())
            ts to smoothed
        }.filterNotNull()
        // Congestion alerts
        _alerts.value = AlertEngine.analyzeCongestion(results)
    }

    private fun collectScanResults() {
        viewModelScope.launch {
            wifiScanner.scanResults().collect { results ->
                _scanResults.value = results.sortedByDescending { it.rssi }
                _channelUtilization.value = ChannelUtilization.fromScanResults(results)
                _alerts.value = AlertEngine.analyzeCongestion(results)
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

                    rssiRingBuffer.addLast(now to info.rssi)
                    if (rssiRingBuffer.size > 60) rssiRingBuffer.removeFirst()
                    _rssiHistory.value = rssiRingBuffer.toList()

                    val smoothed = emaSmooth(info.rssi.toDouble())
                    smoothedRingBuffer.addLast(now to smoothed.toInt())
                    if (smoothedRingBuffer.size > 60) smoothedRingBuffer.removeFirst()
                    _smoothedRssiHistory.value = smoothedRingBuffer.toList()
                }
                delay(2_000L)
            }
        }
    }

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
