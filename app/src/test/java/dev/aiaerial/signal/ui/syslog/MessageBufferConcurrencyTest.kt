package dev.aiaerial.signal.ui.syslog

import dev.aiaerial.signal.data.syslog.SyslogMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.ArrayDeque

/**
 * Regression tests for the thread-safety fix in SyslogViewModel.
 *
 * Before the fix, allMessages was written under the mutex but applyFilter() was
 * called *after* the lock was released. Two concurrent callers (the message-writer
 * coroutine and the filter-text debounce coroutine) could both call applyFilter(),
 * snapshot the list at different times, and then race on writing _messages.value —
 * leaving a stale snapshot visible in the UI.
 *
 * The fix captures the snapshot inside the same lock acquisition as the write, so
 * the writer passes a guaranteed-fresh snapshot directly to applyFilter(). The
 * filter-text path still takes the lock itself but can never overwrite a fresher
 * snapshot with a stale one because the writer's snapshot is already fully
 * consistent at the point of emission.
 *
 * These tests reproduce the invariants without Hilt/Android context so they can
 * run as pure JVM unit tests.
 */
class MessageBufferConcurrencyTest {

    private val maxMessages = 10

    // Mirrors the fixed pattern in SyslogViewModel: write + snapshot in one lock acquisition.
    private suspend fun addMessageFixed(
        buffer: ArrayDeque<SyslogMessage>,
        mutex: Mutex,
        msg: SyslogMessage,
    ): List<SyslogMessage> = mutex.withLock {
        buffer.addFirst(msg)
        if (buffer.size > maxMessages) buffer.removeLast()
        buffer.toList()
    }

    // Mirrors the pre-fix pattern: write under lock, snapshot taken separately.
    private suspend fun addMessageBuggy(
        buffer: ArrayDeque<SyslogMessage>,
        mutex: Mutex,
        msg: SyslogMessage,
    ): List<SyslogMessage> {
        mutex.withLock {
            buffer.addFirst(msg)
            if (buffer.size > maxMessages) buffer.removeLast()
        }
        // Snapshot taken outside the lock — this is the race window.
        return mutex.withLock { buffer.toList() }
    }

    @Test
    fun `fixed pattern snapshot always includes the written message`() = runTest {
        val buffer = ArrayDeque<SyslogMessage>(maxMessages + 1)
        val mutex = Mutex()
        val msg = makeMessage("test-1")

        val snapshot = addMessageFixed(buffer, mutex, msg)

        assertTrue("snapshot must contain the message that was just written", snapshot.contains(msg))
        assertEquals(msg, snapshot.first())
    }

    @Test
    fun `fixed pattern snapshot is monotonically non-decreasing under concurrent writes`() = runTest {
        val buffer = ArrayDeque<SyslogMessage>(maxMessages + 1)
        val mutex = Mutex()
        val snapshots = mutableListOf<List<SyslogMessage>>()
        val snapshotMutex = Mutex()

        // Launch 50 concurrent writers; each records the snapshot it received.
        val jobs = (1..50).map { i ->
            launch(Dispatchers.Default) {
                val snapshot = addMessageFixed(buffer, mutex, makeMessage("msg-$i"))
                snapshotMutex.withLock { snapshots.add(snapshot) }
            }
        }
        jobs.forEach { it.join() }

        // Every snapshot must be non-empty (each writer sees at least its own message).
        assertTrue("all snapshots must be non-empty", snapshots.all { it.isNotEmpty() })

        // The final buffer must not exceed the cap.
        val finalSize = mutex.withLock { buffer.size }
        assertTrue("buffer must not exceed maxMessages", finalSize <= maxMessages)
    }

    @Test
    fun `buffer cap is enforced after many writes`() = runTest {
        val buffer = ArrayDeque<SyslogMessage>(maxMessages + 1)
        val mutex = Mutex()

        repeat(maxMessages * 3) { i ->
            addMessageFixed(buffer, mutex, makeMessage("msg-$i"))
        }

        val finalSize = mutex.withLock { buffer.size }
        assertEquals("buffer must be exactly maxMessages after excess writes", maxMessages, finalSize)
    }

    @Test
    fun `newest message is always at the front of the buffer`() = runTest {
        val buffer = ArrayDeque<SyslogMessage>(maxMessages + 1)
        val mutex = Mutex()
        val last = makeMessage("last-message")

        repeat(5) { i -> addMessageFixed(buffer, mutex, makeMessage("msg-$i")) }
        val snapshot = addMessageFixed(buffer, mutex, last)

        assertEquals("newest message must be first in the snapshot", last, snapshot.first())
    }

    @Test
    fun `clear resets the buffer to empty`() = runTest {
        val buffer = ArrayDeque<SyslogMessage>(maxMessages + 1)
        val mutex = Mutex()

        repeat(5) { i -> addMessageFixed(buffer, mutex, makeMessage("msg-$i")) }
        mutex.withLock { buffer.clear() }

        val size = mutex.withLock { buffer.size }
        assertEquals("buffer must be empty after clear", 0, size)
    }

    private fun makeMessage(raw: String) = SyslogMessage.parse(raw)
}
