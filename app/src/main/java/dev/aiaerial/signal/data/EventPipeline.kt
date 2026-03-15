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

    // External scope used to launch the delayed flush timer. Set once by the
    // service/caller that owns this pipeline's lifecycle. Avoids passing a scope
    // into every processSyslogMessage() call.
    @Volatile
    private var flushScope: CoroutineScope? = null

    /**
     * Bind an external [CoroutineScope] for launching delayed flush timers.
     * Call this once when the pipeline's owner (e.g. SyslogService) starts up.
     * When the owner stops, call [unbindScope] or simply cancel the scope.
     */
    fun bindScope(scope: CoroutineScope) {
        flushScope = scope
    }

    /**
     * Unbind the flush scope. Pending flush jobs are cancelled.
     */
    fun unbindScope() {
        flushJob?.cancel()
        flushJob = null
        flushScope = null
    }

    fun getSessionId(): String = currentSessionId

    suspend fun newSession(): String {
        flush() // persist events from the previous session before switching
        currentSessionId = UUID.randomUUID().toString()
        return currentSessionId
    }

    /**
     * Process a syslog message: parse, batch, and flush when thresholds are met.
     *
     * The optional [scope] parameter is supported for backward compatibility but
     * callers should prefer calling [bindScope] once at startup. If [scope] is
     * provided it takes precedence over the bound scope for this call only.
     */
    suspend fun processSyslogMessage(
        msg: SyslogMessage,
        scope: CoroutineScope? = null,
    ): NetworkEvent? {
        val event = vendorDetector.parse(msg.raw, currentSessionId) ?: return null
        batchMutex.withLock {
            pendingEvents.add(event)
            if (pendingEvents.size >= BATCH_SIZE) {
                flushLocked()
            } else if (flushJob == null) {
                val timerScope = scope ?: flushScope
                if (timerScope != null) {
                    flushJob = timerScope.launch {
                        delay(FLUSH_INTERVAL_MS)
                        flush()
                    }
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

    fun eventsForSession(sessionId: String): Flow<List<NetworkEvent>> = dao.getBySession(sessionId)

    fun clientJourney(mac: String): Flow<List<NetworkEvent>> =
        dao.getClientJourney(currentSessionId, mac)

    fun clientJourney(sessionId: String, mac: String): Flow<List<NetworkEvent>> =
        dao.getClientJourney(sessionId, mac)

    fun distinctClients(): Flow<List<String>> = dao.getDistinctClients(currentSessionId)

    fun distinctClients(sessionId: String): Flow<List<String>> = dao.getDistinctClients(sessionId)

    fun eventCount(): Flow<Int> = dao.getEventCount(currentSessionId)

    fun eventCount(sessionId: String): Flow<Int> = dao.getEventCount(sessionId)

    suspend fun sessionSummaries() = dao.getSessionSummaries()

    suspend fun deleteSession(sessionId: String) {
        dao.deleteSession(sessionId)
    }
}
