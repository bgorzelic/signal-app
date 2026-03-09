package dev.aiaerial.signal.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.aiaerial.signal.data.EventPipeline
import dev.aiaerial.signal.data.model.NetworkEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val pipeline: EventPipeline,
) : ViewModel() {

    val clients: StateFlow<List<String>> = pipeline.distinctClients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedClient = MutableStateFlow<String?>(null)
    val selectedClient: StateFlow<String?> = _selectedClient.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val clientEvents: StateFlow<List<NetworkEvent>> = _selectedClient
        .flatMapLatest { mac ->
            if (mac != null) pipeline.clientJourney(mac) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectClient(mac: String) {
        _selectedClient.value = mac
    }
}
