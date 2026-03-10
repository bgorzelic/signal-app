package dev.aiaerial.signal.ui.syslog

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.aiaerial.signal.data.EventPipeline
import dev.aiaerial.signal.data.syslog.SyslogMessage
import dev.aiaerial.signal.service.SyslogService
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.ArrayDeque
import javax.inject.Inject

private const val MAX_MESSAGES = 5000

@OptIn(FlowPreview::class)
@HiltViewModel
class SyslogViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val eventPipeline: EventPipeline,
) : ViewModel() {

    private val _messages = MutableStateFlow<List<SyslogMessage>>(emptyList())
    val messages: StateFlow<List<SyslogMessage>> = _messages.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _filterText = MutableStateFlow("")
    val filterText: StateFlow<String> = _filterText.asStateFlow()

    val parsedEventCount: StateFlow<Int> = eventPipeline.eventCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private var service: SyslogService? = null
    private var isBound = false

    // ArrayDeque: O(1) addFirst, newest messages at the front
    private val allMessages = ArrayDeque<SyslogMessage>(MAX_MESSAGES + 1)

    init {
        // Debounce filter text to avoid O(n) scans on every keystroke
        viewModelScope.launch {
            _filterText.debounce(300).collect { applyFilter() }
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            service = (binder as SyslogService.LocalBinder).service
            _isRunning.value = true
            viewModelScope.launch {
                service?.messages?.collect { msg ->
                    allMessages.addFirst(msg)
                    if (allMessages.size > MAX_MESSAGES) allMessages.removeLast()
                    applyFilter()
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            service = null
            isBound = false
            _isRunning.value = false
        }
    }

    fun startListening() {
        val intent = Intent(context, SyslogService::class.java)
        context.startForegroundService(intent)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        isBound = true
    }

    fun stopListening() {
        if (isBound) {
            context.unbindService(connection)
            isBound = false
        }
        context.stopService(Intent(context, SyslogService::class.java))
        service = null
        _isRunning.value = false
    }

    override fun onCleared() {
        if (isBound) {
            context.unbindService(connection)
            isBound = false
        }
        super.onCleared()
    }

    fun setFilter(text: String) {
        _filterText.value = text
    }

    private fun applyFilter() {
        val filter = _filterText.value
        _messages.value = if (filter.isBlank()) {
            allMessages.toList()
        } else {
            allMessages.filter { it.raw.contains(filter, ignoreCase = true) }
        }
    }

    fun clearMessages() {
        allMessages.clear()
        _messages.value = emptyList()
    }
}
