package dev.aiaerial.signal.data.openclaw

import dev.aiaerial.signal.data.model.NetworkEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenClawClient @Inject constructor(
    private val httpClient: OkHttpClient,
) {
    @Volatile
    private var baseUrl = "http://127.0.0.1:18789"

    fun setBaseUrl(url: String) {
        baseUrl = url.trimEnd('/')
    }

    suspend fun healthCheck(): OpenClawStatus = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("$baseUrl/").get().build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) OpenClawStatus.CONNECTED else OpenClawStatus.DISCONNECTED
            }
        } catch (_: IOException) {
            OpenClawStatus.DISCONNECTED
        }
    }

    suspend fun triageEvent(event: NetworkEvent): String = withContext(Dispatchers.IO) {
        chatWithRetry(
            systemPrompt = TRIAGE_SYSTEM_PROMPT,
            userMessage = buildTriagePrompt(event),
        )
    }

    suspend fun analyzeLogBlock(text: String): String = withContext(Dispatchers.IO) {
        chatWithRetry(
            systemPrompt = LOG_ANALYSIS_SYSTEM_PROMPT,
            userMessage = buildLogAnalysisPrompt(text),
        )
    }

    /**
     * Chat with retry and exponential backoff.
     * Retries on transient failures (timeout, 5xx). Fails fast on 4xx.
     */
    private suspend fun chatWithRetry(
        systemPrompt: String,
        userMessage: String,
        maxRetries: Int = 2,
    ): String {
        var lastError: String? = null
        for (attempt in 0..maxRetries) {
            if (attempt > 0) {
                delay(1000L * attempt) // 1s, 2s backoff
            }
            val result = chat(systemPrompt, userMessage)
            when {
                result.isSuccess -> return result.content
                result.isRetryable && attempt < maxRetries -> {
                    lastError = result.content
                    continue
                }
                else -> return result.content // return error message to caller
            }
        }
        return lastError ?: "Unknown error after $maxRetries retries"
    }

    private fun chat(systemPrompt: String, userMessage: String): ChatResult {
        val body = buildJsonObject {
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "system")
                    put("content", systemPrompt)
                }
                addJsonObject {
                    put("role", "user")
                    put("content", userMessage)
                }
            }
            put("model", "haiku")
            put("stream", false)
        }

        val request = Request.Builder()
            .url("$baseUrl/api/v1/chat")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            httpClient.newCall(request).execute().use { response ->
                when {
                    response.isSuccessful -> {
                        val responseBody = response.body?.string()
                            ?: return ChatResult("No response body from gateway", isSuccess = false, isRetryable = false)
                        parseResponse(responseBody)
                    }
                    response.code in 500..599 -> ChatResult(
                        "Gateway error (HTTP ${response.code}). The AI model may be temporarily unavailable.",
                        isSuccess = false, isRetryable = true,
                    )
                    response.code == 401 || response.code == 403 -> ChatResult(
                        "Gateway authentication failed (HTTP ${response.code}). Check gateway token configuration.",
                        isSuccess = false, isRetryable = false,
                    )
                    response.code == 429 -> ChatResult(
                        "Rate limited by gateway or model provider. Try again in a moment.",
                        isSuccess = false, isRetryable = true,
                    )
                    else -> ChatResult(
                        "Gateway returned HTTP ${response.code}",
                        isSuccess = false, isRetryable = false,
                    )
                }
            }
        } catch (_: SocketTimeoutException) {
            ChatResult(
                "Gateway request timed out. The AI model may be slow to respond.",
                isSuccess = false, isRetryable = true,
            )
        } catch (e: IOException) {
            ChatResult(
                "Cannot reach OpenClaw gateway at $baseUrl. Is it running?",
                isSuccess = false, isRetryable = false,
            )
        }
    }

    private fun parseResponse(responseBody: String): ChatResult {
        return try {
            val json = Json.parseToJsonElement(responseBody).jsonObject

            // Check for error in response body (some gateways embed errors)
            json["error"]?.let { errorObj ->
                val errorMsg = when (errorObj) {
                    is JsonPrimitive -> errorObj.content
                    is JsonObject -> errorObj["message"]?.jsonPrimitive?.content ?: errorObj.toString()
                    else -> errorObj.toString()
                }
                return ChatResult("AI model error: $errorMsg", isSuccess = false, isRetryable = false)
            }

            val content = json["choices"]?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content

            if (content != null) {
                ChatResult(content, isSuccess = true, isRetryable = false)
            } else {
                ChatResult("Unexpected response format from gateway", isSuccess = false, isRetryable = false)
            }
        } catch (e: Exception) {
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            ChatResult("Failed to parse gateway response: ${e.message}", isSuccess = false, isRetryable = false)
        }
    }

    private data class ChatResult(
        val content: String,
        val isSuccess: Boolean,
        val isRetryable: Boolean,
    )

    companion object {
        fun buildTriagePrompt(event: NetworkEvent): String = buildString {
            appendLine("Explain this wireless network event and suggest likely root cause:")
            appendLine()
            appendLine("Event type: ${event.eventType.name}")
            event.clientMac?.let { appendLine("Client MAC: $it") }
            event.apName?.let { appendLine("AP: $it") }
            event.bssid?.let { appendLine("BSSID: $it") }
            event.channel?.let { appendLine("Channel: $it") }
            event.rssi?.let { appendLine("Signal: $it dBm") }
            event.reasonCode?.let { appendLine("IEEE 802.11 reason code $it") }
            appendLine("Vendor: ${event.vendor.name}")
            appendLine()
            appendLine("Raw syslog:")
            appendLine(event.rawMessage)
        }

        fun buildLogAnalysisPrompt(text: String): String = buildString {
            appendLine("Analyze these wireless network log entries.")
            appendLine("Identify patterns, anomalies, root causes, and recommended actions.")
            appendLine()
            val lines = text.lines()
            if (lines.size > 100) {
                appendLine("(${lines.size} lines, showing first 100)")
                appendLine()
                append(lines.take(100).joinToString("\n"))
            } else {
                append(text)
            }
        }

        internal const val TRIAGE_SYSTEM_PROMPT = """You are SIGNAL's wireless network analysis engine. You help wireless network engineers understand network events.

When given a wireless event, provide:
1. A brief plain-English explanation of what happened
2. The likely root cause (top 2-3 possibilities)
3. Recommended next steps for the engineer

Keep responses concise (under 200 words). Use technical wireless terminology but explain acronyms on first use. Reference IEEE 802.11 reason/status codes when applicable."""

        internal const val LOG_ANALYSIS_SYSTEM_PROMPT = """You are SIGNAL's wireless log analysis engine. Analyze wireless controller/AP log entries and identify:

1. Key events (roams, auths, deauths, RF changes)
2. Patterns or anomalies
3. Potential issues and root causes
4. Recommended actions

Group related events. Highlight anything unusual. Keep the analysis structured and actionable."""
    }
}
