package dev.aiaerial.signal.ui.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aiaerial.signal.data.model.EventType
import dev.aiaerial.signal.data.model.NetworkEvent
import dev.aiaerial.signal.ui.theme.ElectricTeal
import dev.aiaerial.signal.ui.theme.Graphite
import dev.aiaerial.signal.ui.theme.SignalTheme
import dev.aiaerial.signal.ui.theme.TextSecondary
import dev.aiaerial.signal.ui.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RoamingTimelineCard(
    event: NetworkEvent,
    modifier: Modifier = Modifier,
    onApClick: ((String) -> Unit)? = null,
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }
    val colors = SignalTheme.colors

    val color = when (event.eventType) {
        EventType.ROAM -> colors.eventRoam
        EventType.ASSOC -> colors.eventAssoc
        EventType.DISASSOC, EventType.DEAUTH -> colors.eventDeauth
        EventType.AUTH -> colors.eventAuth
        else -> colors.eventUnknown
    }

    Row(
        modifier = modifier
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
                color = color.copy(alpha = 0.2f),
                start = Offset(centerX, 0f),
                end = Offset(centerX, size.height),
                strokeWidth = 2f,
            )
            drawCircle(color = color, radius = 5f, center = Offset(centerX, size.height / 2))
            // Glow ring
            drawCircle(
                color = color.copy(alpha = 0.15f),
                radius = 10f,
                center = Offset(centerX, size.height / 2),
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Card(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Graphite),
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = event.eventType.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = color,
                    )
                    Text(
                        text = timeFormat.format(Date(event.timestamp)),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextTertiary,
                    )
                }
                event.apName?.let { ap ->
                    val apModifier = if (onApClick != null) {
                        Modifier.clickable { onApClick(ap) }
                    } else {
                        Modifier
                    }
                    Text(
                        text = ap,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = if (onApClick != null) ElectricTeal else MaterialTheme.colorScheme.onSurface,
                        modifier = apModifier,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    event.channel?.let {
                        Text("Ch $it", fontSize = 11.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                    }
                    event.rssi?.let {
                        Text("$it dBm", fontSize = 11.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                    }
                    event.reasonCode?.let {
                        Text("Reason: $it", fontSize = 11.sp, color = TextTertiary)
                    }
                }
            }
        }
    }
}
