package dev.aiaerial.signal.data.local

/** Projection for session listing queries. Not a Room entity. */
data class SessionSummary(
    val sessionId: String,
    val timestamp: Long,
    val eventCount: Int,
)
