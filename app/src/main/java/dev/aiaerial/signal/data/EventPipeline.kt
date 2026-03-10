package dev.aiaerial.signal.data

import dev.aiaerial.signal.data.local.NetworkEventDao
import dev.aiaerial.signal.data.model.NetworkEvent
import dev.aiaerial.signal.data.parser.VendorDetector
import dev.aiaerial.signal.data.syslog.SyslogMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val BATCH_SIZE = 20
private const val FLUSH_INTERVAL_MS = 500L

@Singleton
class EventPipeline @Inject constructor(
    private val vendorDetector: VendorDetector,
    private val dao: NetworkEventDao,
) {
    @Volatile
    private var currentSessionId: String = UUID.randomUUID().toString()

    private val batchMutex = Mutex()
    private val pendingEvents = mutableListOf<NetworkEvent>()
    private var flushJob: Job? = null

    fun getSessionId(): String = currentSessionId

    fun newSession(): String {
        currentSessionId = UUID.randomUUID().toString()
        return currentSessionId
    }

    suspend fun processSyslogMessage(msg: SyslogMessage, scope: CoroutineScope? = null): NetworkEvent? {
        val event = vendorDetector.parse(msg.raw, currentSessionId) ?: return null
        batchMutex.withLock {
            pendingEvents.add(event)
            if (pendingEvents.size >= BATCH_SIZE) {
                flushLocked()
            } else if (flushJob == null && scope != null) {
                flushJob = scope.launch {
                    delay(FLUSH_INTERVAL_MS)
                    flush()
                }
            }
        }
        return event
    }

    suspend fun flush() {
        batchMutex.withLock { flushLocked() }
    }

    private suspend fun flushLocked() {
        if (pendingEvents.isNotEmpty()) {
            dao.insertAll(pendingEvents.toList())
            pendingEvents.clear()
        }
        flushJob?.cancel()
        flushJob = null
    }

    suspend fun processLogBlock(text: String): List<NetworkEvent> {
        val events = text.lines()
            .mapNotNull { line -> vendorDetector.parse(line.trim(), currentSessionId) }
        if (events.isNotEmpty()) {
            dao.insertAll(events)
        }
        return events
    }

    fun eventsForCurrentSession(): Flow<List<NetworkEvent>> = dao.getBySession(currentSessionId)

    fun clientJourney(mac: String): Flow<List<NetworkEvent>> =
        dao.getClientJourney(currentSessionId, mac)

    fun distinctClients(): Flow<List<String>> = dao.getDistinctClients(currentSessionId)

    fun eventCount(): Flow<Int> = dao.getEventCount(currentSessionId)
}
