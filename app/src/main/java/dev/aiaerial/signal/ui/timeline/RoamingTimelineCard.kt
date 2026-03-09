package dev.aiaerial.signal.ui.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import dev.aiaerial.signal.data.model.EventType
import dev.aiaerial.signal.data.model.NetworkEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RoamingTimelineCard(event: NetworkEvent) {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
    val color = when (event.eventType) {
        EventType.ROAM -> MaterialTheme.colorScheme.primary
        EventType.ASSOC -> MaterialTheme.colorScheme.tertiary
        EventType.DISASSOC, EventType.DEAUTH -> MaterialTheme.colorScheme.error
        EventType.AUTH -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.outline
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        // Timeline line + dot
        Canvas(
            modifier = Modifier
                .width(24.dp)
                .height(72.dp),
        ) {
            val centerX = size.width / 2
            drawLine(
                color = color.copy(alpha = 0.3f),
                start = Offset(centerX, 0f),
                end = Offset(centerX, size.height),
                strokeWidth = 2f,
            )
            drawCircle(color = color, radius = 6f, center = Offset(centerX, size.height / 2))
        }

        Spacer(modifier = Modifier.width(8.dp))

        Card(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 4.dp),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = event.eventType.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = color,
                    )
                    Text(
                        text = timeFormat.format(Date(event.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                event.apName?.let { ap ->
                    Text(text = ap, style = MaterialTheme.typography.titleSmall)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    event.channel?.let { Text("Ch $it", style = MaterialTheme.typography.bodySmall) }
                    event.rssi?.let { Text("$it dBm", style = MaterialTheme.typography.bodySmall) }
                    event.reasonCode?.let { Text("Reason: $it", style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}
