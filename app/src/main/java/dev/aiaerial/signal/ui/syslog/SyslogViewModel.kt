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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    // Service binding state — accessed from ServiceConnection callbacks (main thread)
    // and ViewModel public methods (main thread). Both run on the main thread so
    // @Volatile is sufficient; no mutex needed for these two fields.
    @Volatile
    private var service: SyslogService? = null

    @Volatile
    private var isBound = false

    // Job for the message collection coroutine — cancelled on unbind to prevent
    // collecting from a stale service reference after onServiceDisconnected.
    private var collectionJob: Job? = null

    // ArrayDeque: O(1) addFirst, newest messages at the front.
    // All access must go through messagesMutex to prevent concurrent modification.
    private val allMessages = ArrayDeque<SyslogMessage>(MAX_MESSAGES + 1)
    private val messagesMutex = Mutex()

    init {
        // Debounce filter text to avoid O(n) scans on every keystroke
        viewModelScope.launch {
            _filterText.debounce(300).collect { applyFilter() }
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            val svc = (binder as SyslogService.LocalBinder).service
            service = svc
            _isRunning.value = true
            collectionJob?.cancel()
            collectionJob = viewModelScope.launch {
                svc.messages?.collect { msg ->
                    // Capture the snapshot inside the lock so that the write and
                    // the read that feeds applyFilter are a single atomic operation.
                    // This prevents a concurrent applyFilter() call (e.g. from the
                    // filter-text debounce) from overwriting a fresher snapshot with
                    // a stale one.
                    val snapshot = messagesMutex.withLock {
                        allMessages.addFirst(msg)
                        if (allMessages.size > MAX_MESSAGES) allMessages.removeLast()
                        allMessages.toList()
                    }
                    applyFilter(snapshot)
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            collectionJob?.cancel()
            collectionJob = null
            service = null
            isBound = false
            _isRunning.value = false
        }
    }

    fun startListening() {
        if (isBound) return // already bound — avoid double-bind
        val intent = Intent(context, SyslogService::class.java)
        context.startForegroundService(intent)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        isBound = true
    }

    fun stopListening() {
        unbindIfNeeded()
        context.stopService(Intent(context, SyslogService::class.java))
        service = null
        _isRunning.value = false
    }

    override fun onCleared() {
        unbindIfNeeded()
        super.onCleared()
    }

    private fun unbindIfNeeded() {
        if (isBound) {
            collectionJob?.cancel()
            collectionJob = null
            context.unbindService(connection)
            isBound = false
        }
    }

    fun setFilter(text: String) {
        _filterText.value = text
    }

    // snapshot — when the caller has already captured allMessages under the mutex
    //   (the message-writer path), pass it here to avoid a second lock acquisition
    //   and eliminate the TOCTOU window between write and read.
    // When null (the filter-text debounce path), the snapshot is taken under the mutex here.
    private suspend fun applyFilter(snapshot: List<SyslogMessage>? = null) {
        val filter = _filterText.value
        val data = snapshot ?: messagesMutex.withLock { allMessages.toList() }
        _messages.value = if (filter.isBlank()) {
            data
        } else {
            data.filter { it.raw.contains(filter, ignoreCase = true) }
        }
    }

    fun clearMessages() {
        viewModelScope.launch {
            messagesMutex.withLock {
                allMessages.clear()
            }
            _messages.value = emptyList()
        }
    }
}
