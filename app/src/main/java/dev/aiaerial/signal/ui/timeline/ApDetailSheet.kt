package dev.aiaerial.signal.ui.timeline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aiaerial.signal.data.model.EventType
import dev.aiaerial.signal.data.model.NetworkEvent
import dev.aiaerial.signal.ui.components.CopyableText
import dev.aiaerial.signal.ui.theme.BorderSubtle
import dev.aiaerial.signal.ui.theme.Charcoal
import dev.aiaerial.signal.ui.theme.ElectricTeal
import dev.aiaerial.signal.ui.theme.Graphite
import dev.aiaerial.signal.ui.theme.SignalTheme
import dev.aiaerial.signal.ui.theme.TextSecondary
import dev.aiaerial.signal.ui.theme.TextTertiary
import dev.aiaerial.signal.ui.theme.Void
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApDetailSheet(
    apName: String,
    events: List<NetworkEvent>,
    onDismiss: () -> Unit,
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }
    val colors = SignalTheme.colors

    val apEvents = events.filter { it.apName == apName }.sortedBy { it.timestamp }

    val totalEvents = apEvents.size
    val uniqueClients = apEvents.mapNotNull { it.clientMac }.distinct().count()
    val channelsUsed = apEvents.mapNotNull { it.channel }.distinct().sorted()

    // Per-client summary: last event for each MAC seen at this AP
    data class ClientSummary(
        val mac: String,
        val lastEventType: EventType,
        val lastRssi: Int?,
        val lastSeen: Long,
    )

    val clientSummaries: List<ClientSummary> = apEvents
        .mapNotNull { e -> e.clientMac?.let { mac -> mac to e } }
        .groupBy({ it.first }, { it.second })
        .map { (mac, clientEvents) ->
            val latest = clientEvents.maxBy { it.timestamp }
            ClientSummary(
                mac = mac,
                lastEventType = latest.eventType,
                lastRssi = latest.rssi,
                lastSeen = latest.timestamp,
            )
        }
        .sortedByDescending { it.lastSeen }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Graphite,
        scrimColor = Void.copy(alpha = 0.6f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
        ) {
            // Header — AP name, long-pressable to copy
            CopyableText(
                text = apName,
                label = "AP Name",
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricTeal,
                ),
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Stats card
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Charcoal,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    ApStatItem(label = "Events", value = "$totalEvents")
                    ApStatItem(label = "Clients", value = "$uniqueClients")
                    ApStatItem(
                        label = "Channels",
                        value = if (channelsUsed.isEmpty()) "—" else channelsUsed.joinToString(", "),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "CLIENTS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = TextTertiary,
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Client list
            LazyColumn(
                modifier = Modifier.height(300.dp),
            ) {
                items(clientSummaries, key = { it.mac }) { client ->
                    val eventColor = when (client.lastEventType) {
                        EventType.ROAM -> colors.eventRoam
                        EventType.ASSOC -> colors.eventAssoc
                        EventType.DEAUTH -> colors.eventDeauth
                        EventType.DISASSOC -> colors.eventDisassoc
                        EventType.AUTH -> colors.eventAuth
                        else -> colors.eventUnknown
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Client MAC — monospace, takes available space
                        Text(
                            text = client.mac,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f),
                        )
                        // Last event type — color-coded
                        Text(
                            text = client.lastEventType.name,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = eventColor,
                        )
                        // Last RSSI
                        Text(
                            text = client.lastRssi?.let { "$it dBm" } ?: "—",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary,
                        )
                        // Last seen time
                        Text(
                            text = timeFormat.format(Date(client.lastSeen)),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextTertiary,
                        )
                    }
                    HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun ApStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = ElectricTeal,
        )
        Text(text = label, fontSize = 10.sp, color = TextTertiary)
    }
}
