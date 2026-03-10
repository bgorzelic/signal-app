package dev.aiaerial.signal.ui.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.aiaerial.signal.data.wifi.ChannelUtilization

@Composable
fun ChannelUtilizationCard(
    utilization: List<ChannelUtilization>,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Channel Utilization",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )

            // Group by band
            val byBand = utilization.groupBy { it.band }
            byBand.forEach { (band, channels) ->
                Text(
                    text = band,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    channels.forEach { ch ->
                        ChannelBar(ch, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelBar(
    ch: ChannelUtilization,
    modifier: Modifier = Modifier,
) {
    val color = when (ch.congestionLevel) {
        ChannelUtilization.CongestionLevel.LOW -> Color(0xFF4CAF50)
        ChannelUtilization.CongestionLevel.MEDIUM -> Color(0xFFFFC107)
        ChannelUtilization.CongestionLevel.HIGH -> Color(0xFFF44336)
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Bar height proportional to AP count (max 6 for scaling)
        val barHeight = (ch.apCount.coerceAtMost(6) * 6).dp
        Box(
            modifier = Modifier
                .width(20.dp)
                .height(barHeight)
                .background(color, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)),
        )
        Text(
            text = "${ch.channel}",
            style = MaterialTheme.typography.labelSmall,
        )
        Text(
            text = "${ch.apCount}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
