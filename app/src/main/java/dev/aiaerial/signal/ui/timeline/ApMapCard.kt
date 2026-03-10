package dev.aiaerial.signal.ui.timeline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.aiaerial.signal.data.model.ApAssociation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ApMapCard(
    association: ApAssociation,
    modifier: Modifier = Modifier,
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = association.apName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${association.clients.size} client${if (association.clients.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            association.clients.forEach { client ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = client.clientMac,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f),
                    )
                    client.rssi?.let {
                        Text(
                            text = "$it dBm",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    client.channel?.let {
                        Text(
                            text = "Ch $it",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Text(
                        text = timeFormat.format(Date(client.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
