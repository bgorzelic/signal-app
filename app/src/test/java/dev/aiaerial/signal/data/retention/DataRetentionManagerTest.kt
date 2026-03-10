package dev.aiaerial.signal.data.retention

import dev.aiaerial.signal.data.local.NetworkEventDao
import dev.aiaerial.signal.data.prefs.SignalPreferences
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DataRetentionManagerTest {

    private lateinit var dao: NetworkEventDao
    private lateinit var prefs: SignalPreferences
    private lateinit var manager: DataRetentionManager

    @Before
    fun setup() {
        dao = mockk(relaxed = true)
        prefs = mockk()
        manager = DataRetentionManager(dao, prefs)
    }

    @Test
    fun `cleanup deletes events older than retention period`() = runTest {
        every { prefs.retentionDays } returns 30
        coEvery { dao.deleteOlderThan(any()) } returns 42

        val deleted = manager.cleanup()

        assertEquals(42, deleted)
        coVerify { dao.deleteOlderThan(match { it > 0 }) }
    }

    @Test
    fun `cleanup is skipped when retention is disabled`() = runTest {
        every { prefs.retentionDays } returns 0

        val deleted = manager.cleanup()

        assertEquals(0, deleted)
        coVerify(exactly = 0) { dao.deleteOlderThan(any()) }
    }

    @Test
    fun `cleanup cutoff is calculated correctly`() = runTest {
        every { prefs.retentionDays } returns 7
        coEvery { dao.deleteOlderThan(any()) } returns 0

        manager.cleanup()

        val sevenDaysMs = 7 * 24L * 60L * 60L * 1000L
        coVerify {
            dao.deleteOlderThan(match { cutoff ->
                val now = System.currentTimeMillis()
                // cutoff should be approximately (now - 7 days), within 1 second tolerance
                val expected = now - sevenDaysMs
                kotlin.math.abs(cutoff - expected) < 1000
            })
        }
    }

    @Test
    fun `totalEventCount delegates to dao`() = runTest {
        coEvery { dao.getTotalEventCount() } returns 1234

        assertEquals(1234, manager.totalEventCount())
    }
}
