package dev.aiaerial.signal.data.alert

import dev.aiaerial.signal.data.model.EventType
import dev.aiaerial.signal.data.model.NetworkEvent
import dev.aiaerial.signal.data.wifi.WifiScanResult

/**
 * Heuristic anomaly detection for field alerts.
 * Stateless — operates on event lists and scan snapshots.
 * Thresholds are passed in, not read from prefs (testable).
 */
object AlertEngine {

    /**
     * Analyze a set of session events and return detected anomalies.
     */
    fun analyzeEvents(
        events: List<NetworkEvent>,
        rssiThreshold: Int = -70,
        roamChurnCount: Int = 5,
        roamWindowMinutes: Int = 10,
        authFailureCount: Int = 3,
    ): List<Alert> {
        val alerts = mutableListOf<Alert>()

        // --- Weak signal detection ---
        val weakSignalEvents = events.filter { it.rssi != null && it.rssi < rssiThreshold }
        if (weakSignalEvents.isNotEmpty()) {
            val weakClients = weakSignalEvents.mapNotNull { it.clientMac }.distinct()
            val worstRssi = weakSignalEvents.minOf { it.rssi!! }
            alerts.add(
                Alert(
                    type = AlertType.WEAK_SIGNAL,
                    severity = if (worstRssi < rssiThreshold - 15) AlertSeverity.CRITICAL else AlertSeverity.WARNING,
                    title = "Weak signal detected",
                    detail = "${weakSignalEvents.size} events below ${rssiThreshold}dBm (worst: ${worstRssi}dBm, ${weakClients.size} client${if (weakClients.size != 1) "s" else ""})",
                    affectedClients = weakClients,
                )
            )
        }

        // --- Roam churn (excessive roaming) ---
        val roamWindowMs = roamWindowMinutes * 60_000L
        val roamEvents = events.filter { it.eventType == EventType.ROAM }
        val clientRoams = roamEvents.groupBy { it.clientMac }
        for ((mac, roams) in clientRoams) {
            if (mac == null) continue
            val sorted = roams.sortedBy { it.timestamp }
            // Sliding window: count roams within any roamWindowMs period
            for (i in sorted.indices) {
                val windowEnd = sorted[i].timestamp + roamWindowMs
                val inWindow = sorted.count { it.timestamp in sorted[i].timestamp..windowEnd }
                if (inWindow >= roamChurnCount) {
                    alerts.add(
                        Alert(
                            type = AlertType.ROAM_CHURN,
                            severity = AlertSeverity.WARNING,
                            title = "Excessive roaming",
                            detail = "$inWindow roams in ${roamWindowMinutes}min for $mac",
                            affectedClients = listOf(mac),
                        )
                    )
                    break // One alert per client
                }
            }
        }

        // --- Auth/deauth failure storms ---
        val authFailures = events.filter {
            it.eventType == EventType.DEAUTH || it.eventType == EventType.DISASSOC
        }
        val clientFailures = authFailures.groupBy { it.clientMac }
        for ((mac, failures) in clientFailures) {
            if (mac == null) continue
            if (failures.size >= authFailureCount) {
                val reasons = failures.mapNotNull { it.reasonCode }.distinct()
                alerts.add(
                    Alert(
                        type = AlertType.AUTH_FAILURE_STORM,
                        severity = if (failures.size >= authFailureCount * 2) AlertSeverity.CRITICAL else AlertSeverity.WARNING,
                        title = "Repeated disconnections",
                        detail = "${failures.size} deauth/disassoc for $mac${if (reasons.isNotEmpty()) " (reasons: ${reasons.joinToString()})" else ""}",
                        affectedClients = listOf(mac),
                    )
                )
            }
        }

        // --- Rapid RSSI degradation ---
        val clientEvents = events.filter { it.rssi != null && it.clientMac != null }
            .groupBy { it.clientMac }
        for ((mac, evts) in clientEvents) {
            if (mac == null) continue
            val sorted = evts.sortedBy { it.timestamp }
            if (sorted.size >= 3) {
                val first = sorted.first().rssi!!
                val last = sorted.last().rssi!!
                val drop = first - last
                if (drop >= 20) {
                    alerts.add(
                        Alert(
                            type = AlertType.RSSI_DEGRADATION,
                            severity = if (drop >= 30) AlertSeverity.CRITICAL else AlertSeverity.WARNING,
                            title = "Signal degradation",
                            detail = "$mac dropped ${drop}dBm (${first}→${last}dBm)",
                            affectedClients = listOf(mac),
                        )
                    )
                }
            }
        }

        return alerts.distinctBy { "${it.type}:${it.affectedClients.sorted()}" }
    }

    /**
     * Analyze WiFi scan results for channel congestion.
     */
    fun analyzeCongestion(scanResults: List<WifiScanResult>): List<Alert> {
        val alerts = mutableListOf<Alert>()
        val byChannel = scanResults.groupBy { it.channel }

        for ((channel, aps) in byChannel) {
            if (channel == 0) continue
            val strongAps = aps.filter { it.rssi >= -65 }
            if (aps.size >= 5 || (strongAps.size >= 3)) {
                alerts.add(
                    Alert(
                        type = AlertType.CHANNEL_CONGESTION,
                        severity = if (aps.size >= 8) AlertSeverity.CRITICAL else AlertSeverity.WARNING,
                        title = "Channel $channel congested",
                        detail = "${aps.size} APs on channel $channel (${strongAps.size} strong)",
                        affectedClients = emptyList(),
                    )
                )
            }
        }

        return alerts
    }
}

enum class AlertType {
    WEAK_SIGNAL,
    ROAM_CHURN,
    AUTH_FAILURE_STORM,
    RSSI_DEGRADATION,
    CHANNEL_CONGESTION,
}

enum class AlertSeverity {
    INFO,
    WARNING,
    CRITICAL,
}

data class Alert(
    val type: AlertType,
    val severity: AlertSeverity,
    val title: String,
    val detail: String,
    val affectedClients: List<String> = emptyList(),
)
