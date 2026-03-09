package dev.aiaerial.signal.ui.logimport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.aiaerial.signal.data.EventPipeline
import dev.aiaerial.signal.data.model.NetworkEvent
import dev.aiaerial.signal.data.openclaw.OpenClawClient
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LogImportViewModel @Inject constructor(
    private val pipeline: EventPipeline,
    private val openClawClient: OpenClawClient,
) : ViewModel() {

    private val _logText = MutableStateFlow("")
    val logText: StateFlow<String> = _logText.asStateFlow()

    private val _parsedEvents = MutableStateFlow<List<NetworkEvent>>(emptyList())
    val parsedEvents: StateFlow<List<NetworkEvent>> = _parsedEvents.asStateFlow()

    private val _aiAnalysis = MutableStateFlow<String?>(null)
    val aiAnalysis: StateFlow<String?> = _aiAnalysis.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    fun setLogText(text: String) {
        _logText.value = text
    }

    fun parseLog() {
        viewModelScope.launch {
            val events = pipeline.processLogBlock(_logText.value)
            _parsedEvents.value = events
        }
    }

    fun analyzeWithAi() {
        viewModelScope.launch {
            _isAnalyzing.value = true
            try {
                _aiAnalysis.value = openClawClient.analyzeLogBlock(_logText.value)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiAnalysis.value = "Error: ${e.message}"
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    fun clear() {
        _logText.value = ""
        _parsedEvents.value = emptyList()
        _aiAnalysis.value = null
    }
}
