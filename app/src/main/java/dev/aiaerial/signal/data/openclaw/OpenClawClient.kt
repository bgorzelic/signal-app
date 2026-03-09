package dev.aiaerial.signal.data.openclaw

import dev.aiaerial.signal.data.model.NetworkEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
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
        val prompt = buildTriagePrompt(event)
        chat(
            systemPrompt = TRIAGE_SYSTEM_PROMPT,
            userMessage = prompt,
        )
    }

    suspend fun analyzeLogBlock(text: String): String = withContext(Dispatchers.IO) {
        chat(
            systemPrompt = LOG_ANALYSIS_SYSTEM_PROMPT,
            userMessage = "Analyze these wireless network log entries:\n\n$text",
        )
    }

    private fun chat(systemPrompt: String, userMessage: String): String {
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

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return "OpenClaw error: HTTP ${response.code}"
            }
            val responseBody = response.body?.string() ?: return "No response from OpenClaw"

            return try {
                val json = Json.parseToJsonElement(responseBody).jsonObject
                json["choices"]?.jsonArray?.firstOrNull()
                    ?.jsonObject?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content
                    ?: responseBody
            } catch (e: Exception) {
                if (e is kotlin.coroutines.cancellation.CancellationException) throw e
                responseBody
            }
        }
    }

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

        private const val TRIAGE_SYSTEM_PROMPT = """You are SIGNAL's wireless network analysis engine. You help wireless network engineers understand network events.

When given a wireless event, provide:
1. A brief plain-English explanation of what happened
2. The likely root cause (top 2-3 possibilities)
3. Recommended next steps for the engineer

Keep responses concise (under 200 words). Use technical wireless terminology but explain acronyms on first use. Reference IEEE 802.11 reason/status codes when applicable."""

        private const val LOG_ANALYSIS_SYSTEM_PROMPT = """You are SIGNAL's wireless log analysis engine. Analyze wireless controller/AP log entries and identify:

1. Key events (roams, auths, deauths, RF changes)
2. Patterns or anomalies
3. Potential issues and root causes
4. Recommended actions

Group related events. Highlight anything unusual. Keep the analysis structured and actionable."""
    }
}
