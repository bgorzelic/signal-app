package dev.aiaerial.signal.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApAssociationTest {

    private fun event(
        clientMac: String,
        apName: String?,
        eventType: EventType,
        timestamp: Long,
        rssi: Int? = null,
        channel: Int? = null,
    ) = NetworkEvent(
        id = 0,
        timestamp = timestamp,
        eventType = eventType,
        clientMac = clientMac,
        apName = apName,
        rawMessage = "test",
        sessionId = "test-session",
        rssi = rssi,
        channel = channel,
    )

    @Test
    fun `empty events produce empty associations`() {
        val result = ApAssociation.fromEvents(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `single ASSOC event creates one association`() {
        val events = listOf(
            event("aa:bb:cc:dd:ee:01", "AP-Lobby", EventType.ASSOC, 1000L, rssi = -55, channel = 36),
        )
        val result = ApAssociation.fromEvents(events)

        assertEquals(1, result.size)
        assertEquals("AP-Lobby", result[0].apName)
        assertEquals(1, result[0].clients.size)
        assertEquals("aa:bb:cc:dd:ee:01", result[0].clients[0].clientMac)
        assertEquals(-55, result[0].clients[0].rssi)
        assertEquals(36, result[0].clients[0].channel)
    }

    @Test
    fun `DEAUTH client is excluded`() {
        val events = listOf(
            event("aa:bb:cc:dd:ee:01", "AP-Lobby", EventType.ASSOC, 1000L),
            event("aa:bb:cc:dd:ee:01", "AP-Lobby", EventType.DEAUTH, 2000L),
        )
        val result = ApAssociation.fromEvents(events)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `DISASSOC client is excluded`() {
        val events = listOf(
            event("aa:bb:cc:dd:ee:01", "AP-Lobby", EventType.ASSOC, 1000L),
            event("aa:bb:cc:dd:ee:01", "AP-Lobby", EventType.DISASSOC, 2000L),
        )
        val result = ApAssociation.fromEvents(events)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `ROAM moves client to new AP`() {
        val events = listOf(
            event("aa:bb:cc:dd:ee:01", "AP-Lobby", EventType.ASSOC, 1000L),
            event("aa:bb:cc:dd:ee:01", "AP-Floor2", EventType.ROAM, 2000L),
        )
        val result = ApAssociation.fromEvents(events)

        assertEquals(1, result.size)
        assertEquals("AP-Floor2", result[0].apName)
    }

    @Test
    fun `multiple clients grouped by AP sorted by client count desc`() {
        val events = listOf(
            event("aa:bb:cc:dd:ee:01", "AP-Lobby", EventType.ASSOC, 1000L),
            event("aa:bb:cc:dd:ee:02", "AP-Lobby", EventType.ASSOC, 1100L),
            event("aa:bb:cc:dd:ee:03", "AP-Lobby", EventType.ASSOC, 1200L),
            event("aa:bb:cc:dd:ee:04", "AP-Floor2", EventType.ASSOC, 1300L),
        )
        val result = ApAssociation.fromEvents(events)

        assertEquals(2, result.size)
        assertEquals("AP-Lobby", result[0].apName)
        assertEquals(3, result[0].clients.size)
        assertEquals("AP-Floor2", result[1].apName)
        assertEquals(1, result[1].clients.size)
    }

    @Test
    fun `clients within AP sorted by timestamp descending`() {
        val events = listOf(
            event("aa:bb:cc:dd:ee:01", "AP-Lobby", EventType.ASSOC, 1000L),
            event("aa:bb:cc:dd:ee:02", "AP-Lobby", EventType.ASSOC, 3000L),
            event("aa:bb:cc:dd:ee:03", "AP-Lobby", EventType.ASSOC, 2000L),
        )
        val result = ApAssociation.fromEvents(events)

        assertEquals(1, result.size)
        assertEquals("aa:bb:cc:dd:ee:02", result[0].clients[0].clientMac)
        assertEquals("aa:bb:cc:dd:ee:03", result[0].clients[1].clientMac)
        assertEquals("aa:bb:cc:dd:ee:01", result[0].clients[2].clientMac)
    }

    @Test
    fun `events with null clientMac are ignored`() {
        val events = listOf(
            NetworkEvent(
                id = 0, timestamp = 1000L, eventType = EventType.ASSOC,
                clientMac = null, apName = "AP-Lobby", rawMessage = "test", sessionId = "s",
            ),
        )
        val result = ApAssociation.fromEvents(events)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `events with null apName are excluded from associations`() {
        val events = listOf(
            event("aa:bb:cc:dd:ee:01", null, EventType.ASSOC, 1000L),
        )
        val result = ApAssociation.fromEvents(events)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `AUTH event counts as associated`() {
        val events = listOf(
            event("aa:bb:cc:dd:ee:01", "AP-Lobby", EventType.AUTH, 1000L),
        )
        val result = ApAssociation.fromEvents(events)

        assertEquals(1, result.size)
        assertEquals(EventType.AUTH, result[0].clients[0].lastEventType)
    }
}
