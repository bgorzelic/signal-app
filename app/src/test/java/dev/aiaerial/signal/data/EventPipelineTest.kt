package dev.aiaerial.signal.data

import dev.aiaerial.signal.data.local.NetworkEventDao
import dev.aiaerial.signal.data.model.EventType
import dev.aiaerial.signal.data.model.NetworkEvent
import dev.aiaerial.signal.data.model.Vendor
import dev.aiaerial.signal.data.parser.VendorDetector
import dev.aiaerial.signal.data.syslog.SyslogMessage
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class EventPipelineTest {

    private lateinit var dao: NetworkEventDao
    private lateinit var vendorDetector: VendorDetector
    private lateinit var pipeline: EventPipeline

    @Before
    fun setup() {
        dao = mockk(relaxed = true)
        vendorDetector = mockk()
        pipeline = EventPipeline(vendorDetector, dao)
    }

    @Test
    fun `getSessionId returns a valid UUID`() {
        val id = pipeline.getSessionId()
        assertNotNull(id)
        assertTrue(id.matches(Regex("[0-9a-f-]{36}")))
    }

    @Test
    fun `newSession generates a different session ID`() = runTest {
        val first = pipeline.getSessionId()
        val second = pipeline.newSession()
        assertNotEquals(first, second)
    }

    @Test
    fun `newSession flushes pending events before switching`() = runTest {
        val event = makeEvent(sessionId = pipeline.getSessionId())
        every { vendorDetector.parse(any(), any()) } returns event
        coEvery { dao.insertAll(any()) } returns listOf(1L)

        val msg = SyslogMessage.parse("test syslog line")
        pipeline.processSyslogMessage(msg)

        pipeline.newSession()

        // flush should have been called, inserting the pending event
        coVerify { dao.insertAll(match { it.size == 1 }) }
    }

    @Test
    fun `processSyslogMessage returns null for unparseable messages`() = runTest {
        every { vendorDetector.parse(any(), any()) } returns null

        val msg = SyslogMessage.parse("random noise")
        val result = pipeline.processSyslogMessage(msg)

        assertNull(result)
    }

    @Test
    fun `processSyslogMessage batches events and flushes at threshold`() = runTest {
        val sessionId = pipeline.getSessionId()
        coEvery { dao.insertAll(any()) } returns (1L..20L).toList()

        // Return a new event for each call
        every { vendorDetector.parse(any(), any()) } answers {
            makeEvent(sessionId = secondArg())
        }

        // Process exactly BATCH_SIZE (20) messages to trigger flush
        repeat(20) {
            val msg = SyslogMessage.parse("test line $it")
            pipeline.processSyslogMessage(msg)
        }

        coVerify(exactly = 1) { dao.insertAll(match { it.size == 20 }) }
    }

    @Test
    fun `processLogBlock parses all lines and inserts immediately`() = runTest {
        val sessionId = pipeline.getSessionId()
        every { vendorDetector.parse(any(), any()) } answers {
            makeEvent(sessionId = secondArg())
        }
        coEvery { dao.insertAll(any()) } returns listOf(1L, 2L, 3L)

        val text = "line1\nline2\nline3"
        val events = pipeline.processLogBlock(text)

        assertEquals(3, events.size)
        coVerify { dao.insertAll(match { it.size == 3 }) }
    }

    @Test
    fun `processLogBlock skips unparseable lines`() = runTest {
        every { vendorDetector.parse("good", any()) } returns makeEvent(sessionId = pipeline.getSessionId())
        every { vendorDetector.parse("bad", any()) } returns null
        coEvery { dao.insertAll(any()) } returns listOf(1L)

        val events = pipeline.processLogBlock("good\nbad\ngood")

        // "bad" is parsed but "good" appears twice — but trim() makes "good" match both
        assertEquals(2, events.size)
    }

    @Test
    fun `session ID used in parse matches current session`() = runTest {
        val sessionId = pipeline.getSessionId()
        every { vendorDetector.parse(any(), any()) } returns null

        val msg = SyslogMessage.parse("test")
        pipeline.processSyslogMessage(msg)

        verify { vendorDetector.parse(any(), eq(sessionId)) }
    }

    private fun makeEvent(sessionId: String) = NetworkEvent(
        timestamp = System.currentTimeMillis(),
        eventType = EventType.ROAM,
        clientMac = "aa:bb:cc:dd:ee:ff",
        apName = "AP-Test",
        vendor = Vendor.CISCO,
        rawMessage = "test raw message",
        sessionId = sessionId,
    )
}
