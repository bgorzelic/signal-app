package dev.aiaerial.signal.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.aiaerial.signal.data.EventPipeline
import dev.aiaerial.signal.data.alert.Alert
import dev.aiaerial.signal.data.alert.AlertEngine
import dev.aiaerial.signal.data.demo.DemoDataProvider
import dev.aiaerial.signal.data.demo.DemoScenario
import dev.aiaerial.signal.data.export.SessionExporter
import dev.aiaerial.signal.data.export.SessionReportBuilder
import dev.aiaerial.signal.data.local.SessionSummary
import dev.aiaerial.signal.data.model.ApAssociation
import dev.aiaerial.signal.data.model.NetworkEvent
import dev.aiaerial.signal.data.prefs.SignalPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val pipeline: EventPipeline,
    private val prefs: SignalPreferences,
) : ViewModel() {

    private val _sessions = MutableStateFlow<List<SessionSummary>>(emptyList())
    val sessions: StateFlow<List<SessionSummary>> = _sessions.asStateFlow()

    private val _selectedSessionId = MutableStateFlow(pipeline.getSessionId())
    val selectedSessionId: StateFlow<String> = _selectedSessionId.asStateFlow()

    private val _selectedClient = MutableStateFlow<String?>(null)
    val selectedClient: StateFlow<String?> = _selectedClient.asStateFlow()

    private val _exportResult = MutableStateFlow<Pair<String, String>?>(null)
    val exportResult: StateFlow<Pair<String, String>?> = _exportResult.asStateFlow()

    private val _alerts = MutableStateFlow<List<Alert>>(emptyList())
    val alerts: StateFlow<List<Alert>> = _alerts.asStateFlow()

    // Demo mode: static data fallback
    private val _demoEvents = MutableStateFlow<List<NetworkEvent>>(emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val clients: StateFlow<List<String>> = if (prefs.demoMode) {
        _demoEvents.map { events -> events.mapNotNull { it.clientMac }.distinct() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    } else {
        _selectedSessionId
            .flatMapLatest { sid -> pipeline.distinctClients(sid) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val clientEvents: StateFlow<List<NetworkEvent>> = if (prefs.demoMode) {
        _selectedClient.map { mac ->
            if (mac != null) _demoEvents.value.filter { it.clientMac == mac }.sortedBy { it.timestamp }
            else emptyList()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    } else {
        _selectedClient
            .flatMapLatest { mac ->
                val sid = _selectedSessionId.value
                if (mac != null) pipeline.clientJourney(sid, mac) else flowOf(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val allSessionEvents: StateFlow<List<NetworkEvent>> = if (prefs.demoMode) {
        _demoEvents.asStateFlow()
    } else {
        _selectedSessionId
            .flatMapLatest { sid -> pipeline.eventsForSession(sid) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val apAssociations: StateFlow<List<ApAssociation>> = if (prefs.demoMode) {
        _demoEvents.map { events -> ApAssociation.fromEvents(events) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    } else {
        _selectedSessionId
            .flatMapLatest { sid ->
                pipeline.eventsForSession(sid).map { events -> ApAssociation.fromEvents(events) }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    init {
        if (prefs.demoMode) {
            loadDemoData()
        } else {
            loadSessions()
        }
    }

    private fun loadDemoData() {
        val scenario = DemoScenario.entries.getOrElse(prefs.demoScenarioIndex) { DemoScenario.HEALTHY_ROAMING }
        val events = DemoDataProvider.networkEvents(scenario)
        _demoEvents.value = events
        _sessions.value = listOf(
            SessionSummary(
                sessionId = "demo-session-001",
                timestamp = events.firstOrNull()?.timestamp ?: System.currentTimeMillis(),
                eventCount = events.size,
            )
        )
        _selectedSessionId.value = "demo-session-001"
        // Auto-select first client
        val firstClient = events.firstOrNull()?.clientMac
        if (firstClient != null) _selectedClient.value = firstClient
        // Run alert analysis
        _alerts.value = AlertEngine.analyzeEvents(
            events,
            rssiThreshold = prefs.alertRssiThreshold,
            roamChurnCount = prefs.alertRoamChurnCount,
            roamWindowMinutes = prefs.alertRoamWindowMinutes,
            authFailureCount = prefs.alertAuthFailureCount,
        )
    }

    fun loadSessions() {
        viewModelScope.launch {
            _sessions.value = pipeline.sessionSummaries()
        }
    }

    fun selectSession(sessionId: String) {
        _selectedSessionId.value = sessionId
        _selectedClient.value = null
    }

    fun selectClient(mac: String) {
        _selectedClient.value = mac
    }

    fun exportCsv() {
        viewModelScope.launch {
            val events = allSessionEvents.value
            if (events.isNotEmpty()) {
                _exportResult.value = "text/csv" to SessionExporter.toCsv(events)
            }
        }
    }

    fun exportJson() {
        viewModelScope.launch {
            val events = allSessionEvents.value
            if (events.isNotEmpty()) {
                _exportResult.value = "application/json" to SessionExporter.toJson(events)
            }
        }
    }

    fun exportMarkdownReport() {
        viewModelScope.launch {
            val events = allSessionEvents.value
            val sessionId = _selectedSessionId.value
            if (events.isNotEmpty()) {
                val report = SessionReportBuilder.buildMarkdownReport(
                    sessionId = sessionId,
                    events = events,
                    alerts = _alerts.value,
                )
                _exportResult.value = "text/markdown" to report
            }
        }
    }

    fun clearExportResult() {
        _exportResult.value = null
    }

    fun newSession() {
        if (prefs.demoMode) return
        viewModelScope.launch {
            pipeline.newSession()
            loadSessions()
            _selectedSessionId.value = pipeline.getSessionId()
            _selectedClient.value = null
        }
    }

    fun deleteSession(sessionId: String) {
        if (prefs.demoMode) return
        viewModelScope.launch {
            pipeline.deleteSession(sessionId)
            loadSessions()
            // If we deleted the active session, switch to the current pipeline session
            if (_selectedSessionId.value == sessionId) {
                _selectedSessionId.value = pipeline.getSessionId()
                _selectedClient.value = null
            }
        }
    }
}
