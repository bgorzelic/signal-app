package dev.aiaerial.signal.ui.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import dev.aiaerial.signal.data.wifi.WifiScanResult

@Composable
fun WifiNetworkCard(
    result: WifiScanResult,
    modifier: Modifier = Modifier,
) {
    val signalColor = when {
        result.rssi >= -50 -> Color(0xFF4CAF50) // Green — excellent
        result.rssi >= -60 -> MaterialTheme.colorScheme.primary
        result.rssi >= -70 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Signal strength indicator
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(signalColor, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${result.rssi}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.ssid.ifEmpty { "(Hidden)" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = result.bssid,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Ch ${result.channel}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = result.band,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = "${result.channelWidthMhz}MHz",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = result.security,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
