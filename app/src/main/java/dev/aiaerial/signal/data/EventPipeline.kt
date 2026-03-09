package dev.aiaerial.signal.data

import dev.aiaerial.signal.data.local.NetworkEventDao
import dev.aiaerial.signal.data.model.NetworkEvent
import dev.aiaerial.signal.data.parser.VendorDetector
import dev.aiaerial.signal.data.syslog.SyslogMessage
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventPipeline @Inject constructor(
    private val vendorDetector: VendorDetector,
    private val dao: NetworkEventDao,
) {
    private var currentSessionId: String = UUID.randomUUID().toString()

    fun getSessionId(): String = currentSessionId

    fun newSession(): String {
        currentSessionId = UUID.randomUUID().toString()
        return currentSessionId
    }

    suspend fun processSyslogMessage(msg: SyslogMessage): NetworkEvent? {
        val event = vendorDetector.parse(msg.raw, currentSessionId)
        if (event != null) {
            dao.insert(event)
        }
        return event
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
