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
            // For each client, find their most recent event
            val latestByClient = events
                .filter { it.clientMac != null }
                .groupBy { it.clientMac!! }
                .mapValues { (_, clientEvents) -> clientEvents.maxByOrNull { it.timestamp }!! }

            // Only keep clients that are currently associated (last event is ASSOC or ROAM)
            val associatedClients = latestByClient.values
                .filter { it.eventType in setOf(EventType.ASSOC, EventType.ROAM, EventType.AUTH) }
                .filter { it.apName != null }

            // Group by AP
            return associatedClients
                .groupBy { it.apName!! }
                .map { (ap, events) ->
                    ApAssociation(
                        apName = ap,
                        clients = events.map { e ->
                            ClientState(
                                clientMac = e.clientMac!!,
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
