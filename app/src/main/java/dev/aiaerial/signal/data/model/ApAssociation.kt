package dev.aiaerial.signal.data.model

/**
 * Represents the current association state of a client to an AP,
 * derived from the most recent ASSOC/ROAM event for that client.
 */
data class ApAssociation(
    val apName: String,
    val clients: List<ClientState>,
) {
    data class ClientState(
        val clientMac: String,
        val rssi: Int?,
        val channel: Int?,
        val lastEventType: EventType,
        val timestamp: Long,
    )

    companion object {
        /**
         * Build AP association map from a list of events.
         * For each client, finds the most recent ASSOC or ROAM event
         * to determine current AP. Ignores clients whose last event
         * is DEAUTH or DISASSOC (they're no longer associated).
         */
        fun fromEvents(events: List<NetworkEvent>): List<ApAssociation> {
            // For each client, find their most recent event.
            // mapNotNull eliminates nulls safely — no !! needed.
            val latestByClient = events
                .mapNotNull { event -> event.clientMac?.let { mac -> mac to event } }
                .groupBy({ it.first }, { it.second })
                .mapValues { (_, clientEvents) -> clientEvents.maxBy { it.timestamp } }

            // Only keep clients that are currently associated (last event is ASSOC or ROAM)
            val associatedClients = latestByClient.values
                .filter { it.eventType in setOf(EventType.ASSOC, EventType.ROAM, EventType.AUTH) }

            // Group by AP, skipping events with null apName
            return associatedClients
                .mapNotNull { event -> event.apName?.let { ap -> ap to event } }
                .groupBy({ it.first }, { it.second })
                .map { (ap, apEvents) ->
                    ApAssociation(
                        apName = ap,
                        clients = apEvents.map { e ->
                            ClientState(
                                // clientMac is guaranteed non-null here: we filtered
                                // on clientMac in latestByClient above, and apEvents
                                // are a subset of those values.
                                clientMac = e.clientMac ?: error("BUG: null clientMac after filtering"),
                                rssi = e.rssi,
                                channel = e.channel,
                                lastEventType = e.eventType,
                                timestamp = e.timestamp,
                            )
                        }.sortedByDescending { it.timestamp },
                    )
                }
                .sortedByDescending { it.clients.size }
        }
    }
}
