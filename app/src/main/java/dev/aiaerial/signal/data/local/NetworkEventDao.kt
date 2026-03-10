package dev.aiaerial.signal.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import dev.aiaerial.signal.data.model.EventType
import dev.aiaerial.signal.data.model.NetworkEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkEventDao {

    @Insert
    suspend fun insert(event: NetworkEvent): Long

    @Insert
    suspend fun insertAll(events: List<NetworkEvent>): List<Long>

    @Query("SELECT * FROM network_events WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    fun getBySession(sessionId: String): Flow<List<NetworkEvent>>

    @Query("SELECT * FROM network_events WHERE sessionId = :sessionId AND eventType = :type ORDER BY timestamp DESC")
    fun getBySessionAndType(sessionId: String, type: EventType): Flow<List<NetworkEvent>>

    @Query("SELECT * FROM network_events WHERE sessionId = :sessionId AND clientMac = :mac ORDER BY timestamp ASC")
    fun getClientJourney(sessionId: String, mac: String): Flow<List<NetworkEvent>>

    @Query("SELECT * FROM network_events WHERE id = :id")
    suspend fun getById(id: Long): NetworkEvent?

    @Query("DELETE FROM network_events WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Query("SELECT DISTINCT clientMac FROM network_events WHERE sessionId = :sessionId AND clientMac IS NOT NULL")
    fun getDistinctClients(sessionId: String): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM network_events WHERE sessionId = :sessionId")
    fun getEventCount(sessionId: String): Flow<Int>

    /** Get all distinct sessions with their oldest timestamp, ordered newest first. */
    @Query("""
        SELECT sessionId, MIN(timestamp) AS timestamp, COUNT(*) AS eventCount
        FROM network_events
        GROUP BY sessionId
        ORDER BY MIN(timestamp) DESC
    """)
    suspend fun getSessionSummaries(): List<SessionSummary>

    /** Delete all events older than the given timestamp. */
    @Query("DELETE FROM network_events WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOlderThan(cutoffTimestamp: Long): Int

    /** Get total row count across all sessions. */
    @Query("SELECT COUNT(*) FROM network_events")
    suspend fun getTotalEventCount(): Int
}
