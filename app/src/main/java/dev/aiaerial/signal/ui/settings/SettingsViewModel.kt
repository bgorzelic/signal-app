package dev.aiaerial.signal.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.aiaerial.signal.data.demo.DemoScenario
import dev.aiaerial.signal.data.openclaw.OpenClawClient
import dev.aiaerial.signal.data.openclaw.OpenClawStatus
import dev.aiaerial.signal.data.prefs.SignalPreferences
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: SignalPreferences,
    private val openClawClient: OpenClawClient,
) : ViewModel() {

    private val _openClawStatus = MutableStateFlow(OpenClawStatus.CHECKING)
    val openClawStatus: StateFlow<OpenClawStatus> = _openClawStatus.asStateFlow()

    private val _openClawUrl = MutableStateFlow(prefs.openClawUrl)
    val openClawUrl: StateFlow<String> = _openClawUrl.asStateFlow()

    private val _syslogPort = MutableStateFlow(prefs.syslogPort)
    val syslogPort: StateFlow<Int> = _syslogPort.asStateFlow()

    private val _demoMode = MutableStateFlow(prefs.demoMode)
    val demoMode: StateFlow<Boolean> = _demoMode.asStateFlow()

    private val _demoScenarioIndex = MutableStateFlow(prefs.demoScenarioIndex)
    val demoScenarioIndex: StateFlow<Int> = _demoScenarioIndex.asStateFlow()

    val demoScenarios = DemoScenario.entries

    private val _alertRssiThreshold = MutableStateFlow(prefs.alertRssiThreshold)
    val alertRssiThreshold: StateFlow<Int> = _alertRssiThreshold.asStateFlow()

    private val _alertRoamChurnCount = MutableStateFlow(prefs.alertRoamChurnCount)
    val alertRoamChurnCount: StateFlow<Int> = _alertRoamChurnCount.asStateFlow()

    private val _alertRoamWindowMinutes = MutableStateFlow(prefs.alertRoamWindowMinutes)
    val alertRoamWindowMinutes: StateFlow<Int> = _alertRoamWindowMinutes.asStateFlow()

    private val _alertAuthFailureCount = MutableStateFlow(prefs.alertAuthFailureCount)
    val alertAuthFailureCount: StateFlow<Int> = _alertAuthFailureCount.asStateFlow()

    private var healthCheckJob: Job? = null

    init {
        checkOpenClawHealth()
    }

    fun checkOpenClawHealth() {
        healthCheckJob?.cancel()
        healthCheckJob = viewModelScope.launch {
            _openClawStatus.value = OpenClawStatus.CHECKING
            _openClawStatus.value = openClawClient.healthCheck()
        }
    }

    fun setOpenClawUrl(url: String) {
        _openClawUrl.value = url
        prefs.openClawUrl = url
        openClawClient.setBaseUrl(url)
    }

    fun setSyslogPort(port: Int) {
        _syslogPort.value = port
        prefs.syslogPort = port
    }

    fun setDemoMode(enabled: Boolean) {
        _demoMode.value = enabled
        prefs.demoMode = enabled
    }

    fun setDemoScenario(index: Int) {
        _demoScenarioIndex.value = index
        prefs.demoScenarioIndex = index
    }

    fun setAlertRssiThreshold(value: Int) {
        _alertRssiThreshold.value = value
        prefs.alertRssiThreshold = value
    }

    fun setAlertRoamChurnCount(value: Int) {
        _alertRoamChurnCount.value = value
        prefs.alertRoamChurnCount = value
    }

    fun setAlertRoamWindowMinutes(value: Int) {
        _alertRoamWindowMinutes.value = value
        prefs.alertRoamWindowMinutes = value
    }

    fun setAlertAuthFailureCount(value: Int) {
        _alertAuthFailureCount.value = value
        prefs.alertAuthFailureCount = value
    }

    fun markSetupComplete() {
        prefs.setupComplete = true
    }
}
