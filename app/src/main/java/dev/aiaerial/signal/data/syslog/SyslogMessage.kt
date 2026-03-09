package dev.aiaerial.signal.data.syslog

data class SyslogMessage(
    val priority: Int,
    val facility: Int,
    val severity: Int,
    val hostname: String?,
    val message: String,
    val raw: String,
    val receivedAt: Long = System.currentTimeMillis(),
    val id: String = java.util.UUID.randomUUID().toString(),
) {
    val severityLabel: String get() = severityName(severity)

    companion object {
        private val RFC3164 = Regex("""^<(\d{1,3})>(\w{3}\s+\d{1,2}\s\d{2}:\d{2}:\d{2})\s+(\S+)\s+(.*)$""")

        fun parse(raw: String): SyslogMessage {
            val match = RFC3164.find(raw)
            return if (match != null) {
                val pri = match.groupValues[1].toInt()
                SyslogMessage(
                    priority = pri,
                    facility = pri / 8,
                    severity = pri % 8,
                    hostname = match.groupValues[3],
                    message = match.groupValues[4],
                    raw = raw,
                )
            } else {
                SyslogMessage(
                    priority = -1,
                    facility = -1,
                    severity = -1,
                    hostname = null,
                    message = raw,
                    raw = raw,
                )
            }
        }

        fun severityName(severity: Int): String = when (severity) {
            0 -> "emergency"
            1 -> "alert"
            2 -> "critical"
            3 -> "error"
            4 -> "warning"
            5 -> "notice"
            6 -> "info"
            7 -> "debug"
            else -> "unknown"
        }
    }
}
