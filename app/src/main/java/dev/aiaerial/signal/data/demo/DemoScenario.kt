package dev.aiaerial.signal.data.demo

/**
 * Enumeration of built-in demo scenarios.
 * Each scenario represents a realistic field situation.
 */
enum class DemoScenario(
    val label: String,
    val description: String,
) {
    HEALTHY_ROAMING(
        label = "Healthy Office Roaming",
        description = "Normal enterprise roaming across 4 APs, good RSSI, clean handoffs",
    ),
    STICKY_CLIENT(
        label = "Sticky Client / Poor Roaming",
        description = "Client holds onto weak AP instead of roaming, repeated deauths",
    ),
    CHANNEL_CONGESTION(
        label = "High Channel Congestion",
        description = "Dense 2.4 GHz environment with co-channel interference",
    ),
}
