package dev.aiaerial.signal.data.wifi

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Basic download speed test using a known large file.
 * Measures throughput over current WiFi connection.
 */
@Singleton
class SpeedTest @Inject constructor() {

    // Lightweight client with generous timeout for speed tests
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    data class Result(
        val downloadMbps: Double,
        val bytesDownloaded: Long,
        val durationMs: Long,
        val error: String? = null,
    )

    /**
     * Run a download speed test by fetching a known URL and measuring throughput.
     * Uses Cloudflare's speed test endpoint (100MB file, widely available).
     */
    suspend fun runDownloadTest(
        url: String = TEST_URL,
        maxBytes: Long = MAX_DOWNLOAD_BYTES,
    ): Result = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).get().build()
            val startTime = System.nanoTime()
            var totalBytes = 0L

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result(0.0, 0, 0, "HTTP ${response.code}")
                }

                val source = response.body?.source()
                    ?: return@withContext Result(0.0, 0, 0, "Empty response")

                val buffer = ByteArray(8192)
                while (totalBytes < maxBytes) {
                    val read = source.inputStream().read(buffer)
                    if (read == -1) break
                    totalBytes += read
                }
            }

            val durationNs = System.nanoTime() - startTime
            val durationMs = durationNs / 1_000_000
            val durationSec = durationNs / 1_000_000_000.0
            val bitsDownloaded = totalBytes * 8.0
            val mbps = if (durationSec > 0) bitsDownloaded / durationSec / 1_000_000.0 else 0.0

            Result(
                downloadMbps = (mbps * 100).toLong() / 100.0, // 2 decimal places
                bytesDownloaded = totalBytes,
                durationMs = durationMs,
            )
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result(0.0, 0, 0, e.message ?: "Unknown error")
        }
    }

    companion object {
        // Cloudflare speed test — returns random data, no caching
        const val TEST_URL = "https://speed.cloudflare.com/__down?bytes=10000000"
        const val MAX_DOWNLOAD_BYTES = 10_000_000L // 10MB cap
    }
}
