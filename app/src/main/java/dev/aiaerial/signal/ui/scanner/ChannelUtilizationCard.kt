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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aiaerial.signal.data.wifi.ChannelUtilization
import dev.aiaerial.signal.ui.theme.ElectricTeal
import dev.aiaerial.signal.ui.theme.Graphite
import dev.aiaerial.signal.ui.theme.SignalTheme
import dev.aiaerial.signal.ui.theme.TextSecondary
import dev.aiaerial.signal.ui.theme.TextTertiary

@Composable
fun ChannelUtilizationCard(
    utilization: List<ChannelUtilization>,
    modifier: Modifier = Modifier,
) {
    val colors = SignalTheme.colors

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Graphite),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "CHANNEL UTILIZATION",
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 2.sp,
                color = TextTertiary,
            )

            val byBand = utilization.groupBy { it.band }
            byBand.forEach { (band, channels) ->
                Text(
                    text = band,
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricTeal,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    channels.forEach { ch ->
                        ChannelBar(ch, colors, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelBar(
    ch: ChannelUtilization,
    colors: dev.aiaerial.signal.ui.theme.SignalColors,
    modifier: Modifier = Modifier,
) {
    val barColor = when (ch.congestionLevel) {
        ChannelUtilization.CongestionLevel.LOW -> colors.signalExcellent
        ChannelUtilization.CongestionLevel.MEDIUM -> colors.signalFair
        ChannelUtilization.CongestionLevel.HIGH -> colors.signalPoor
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val barHeight = (ch.apCount.coerceAtMost(6) * 6).dp
        Box(
            modifier = Modifier
                .width(18.dp)
                .height(barHeight)
                .background(
                    barColor.copy(alpha = 0.7f),
                    RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp),
                ),
        )
        Text(
            text = "${ch.channel}",
            fontSize = 9.sp,
            color = TextSecondary,
        )
        Text(
            text = "${ch.apCount}",
            fontSize = 9.sp,
            color = TextTertiary,
        )
    }
}
