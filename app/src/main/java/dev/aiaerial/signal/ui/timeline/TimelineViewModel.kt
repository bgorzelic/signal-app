package dev.aiaerial.signal.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.aiaerial.signal.data.EventPipeline
import dev.aiaerial.signal.data.export.SessionExporter
import dev.aiaerial.signal.data.local.SessionSummary
import dev.aiaerial.signal.data.model.NetworkEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
) : ViewModel() {

    private val _sessions = MutableStateFlow<List<SessionSummary>>(emptyList())
    val sessions: StateFlow<List<SessionSummary>> = _sessions.asStateFlow()

    private val _selectedSessionId = MutableStateFlow(pipeline.getSessionId())
    val selectedSessionId: StateFlow<String> = _selectedSessionId.asStateFlow()

    private val _selectedClient = MutableStateFlow<String?>(null)
    val selectedClient: StateFlow<String?> = _selectedClient.asStateFlow()

    /** Export result: pair of (format label, content string), or null. */
    private val _exportResult = MutableStateFlow<Pair<String, String>?>(null)
    val exportResult: StateFlow<Pair<String, String>?> = _exportResult.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val clients: StateFlow<List<String>> = _selectedSessionId
        .flatMapLatest { sid -> pipeline.distinctClients(sid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val clientEvents: StateFlow<List<NetworkEvent>> = _selectedClient
        .flatMapLatest { mac ->
            val sid = _selectedSessionId.value
            if (mac != null) pipeline.clientJourney(sid, mac) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allSessionEvents: StateFlow<List<NetworkEvent>> = _selectedSessionId
        .flatMapLatest { sid -> pipeline.eventsForSession(sid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadSessions()
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

    fun clearExportResult() {
        _exportResult.value = null
    }
}
