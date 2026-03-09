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
import dev.aiaerial.signal.data.syslog.SyslogMessage
import dev.aiaerial.signal.service.SyslogService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyslogViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _messages = MutableStateFlow<List<SyslogMessage>>(emptyList())
    val messages: StateFlow<List<SyslogMessage>> = _messages.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _filterText = MutableStateFlow("")
    val filterText: StateFlow<String> = _filterText.asStateFlow()

    private var service: SyslogService? = null
    private val allMessages = mutableListOf<SyslogMessage>()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            service = (binder as SyslogService.LocalBinder).service
            _isRunning.value = true
            viewModelScope.launch {
                service?.messages?.collect { msg ->
                    allMessages.add(0, msg) // newest first
                    if (allMessages.size > 5000) allMessages.removeLast()
                    applyFilter()
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            service = null
            _isRunning.value = false
        }
    }

    fun startListening() {
        val intent = Intent(context, SyslogService::class.java)
        context.startForegroundService(intent)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun stopListening() {
        context.unbindService(connection)
        context.stopService(Intent(context, SyslogService::class.java))
        service = null
        _isRunning.value = false
    }

    fun setFilter(text: String) {
        _filterText.value = text
        applyFilter()
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
