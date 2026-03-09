package dev.aiaerial.signal.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.aiaerial.signal.data.openclaw.OpenClawClient
import dev.aiaerial.signal.data.openclaw.OpenClawStatus
import dev.aiaerial.signal.data.prefs.SignalPreferences
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

    init {
        checkOpenClawHealth()
    }

    fun checkOpenClawHealth() {
        viewModelScope.launch {
            _openClawStatus.value = OpenClawStatus.CHECKING
            _openClawStatus.value = openClawClient.healthCheck()
        }
    }

    fun setOpenClawUrl(url: String) {
        _openClawUrl.value = url
        prefs.openClawUrl = url
        openClawClient.setBaseUrl(url)
        checkOpenClawHealth()
    }

    fun setSyslogPort(port: Int) {
        _syslogPort.value = port
        prefs.syslogPort = port
    }

    fun markSetupComplete() {
        prefs.setupComplete = true
    }
}
