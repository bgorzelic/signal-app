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
import androidx.compose.ui.Modifier
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
fun ClientDetailSheet(
    clientMac: String,
    events: List<NetworkEvent>,
    onDismiss: () -> Unit,
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }
    val colors = SignalTheme.colors
    val clientEvents = events.filter { it.clientMac == clientMac }.sortedBy { it.timestamp }

    val roamCount = clientEvents.count { it.eventType == EventType.ROAM }
    val disconnectCount = clientEvents.count { it.eventType == EventType.DEAUTH || it.eventType == EventType.DISASSOC }
    val rssiValues = clientEvents.mapNotNull { it.rssi }
    val rssiRange = if (rssiValues.isNotEmpty()) "${rssiValues.min()} to ${rssiValues.max()} dBm" else "N/A"

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
            // Header
            CopyableText(
                text = clientMac,
                label = "Client MAC",
                style = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
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
                    StatItem("Events", "${clientEvents.size}")
                    StatItem("Roams", "$roamCount")
                    StatItem("Disconnects", "$disconnectCount")
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Charcoal,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("RSSI Range", fontSize = 11.sp, color = TextTertiary)
                    Text(
                        rssiRange,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ElectricTeal,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "EVENT HISTORY",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = TextTertiary,
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Event list
            LazyColumn(
                modifier = Modifier.height(300.dp),
            ) {
                items(clientEvents, key = { "${it.id}-${it.timestamp}" }) { event ->
                    val eventColor = when (event.eventType) {
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
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            event.eventType.name,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = eventColor,
                            modifier = Modifier.weight(0.25f),
                        )
                        Text(
                            event.apName ?: "",
                            fontSize = 10.sp,
                            color = TextSecondary,
                            modifier = Modifier.weight(0.35f),
                        )
                        Text(
                            event.rssi?.let { "$it dBm" } ?: "",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary,
                            modifier = Modifier.weight(0.2f),
                        )
                        Text(
                            timeFormat.format(Date(event.timestamp)),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextTertiary,
                            modifier = Modifier.weight(0.2f),
                        )
                    }
                    HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        Text(
            value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = ElectricTeal,
        )
        Text(label, fontSize = 10.sp, color = TextTertiary)
    }
}
