package dev.aiaerial.signal.ui.syslog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.aiaerial.signal.data.syslog.SyslogMessage
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SyslogScreen(
    viewModel: SyslogViewModel = hiltViewModel(),
    onEventTap: (SyslogMessage) -> Unit = {},
    onImportClick: () -> Unit = {},
) {
    val messages by viewModel.messages.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val filterText by viewModel.filterText.collectAsState()
    val parsedEventCount by viewModel.parsedEventCount.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Controls row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    if (isRunning) viewModel.stopListening() else viewModel.startListening()
                }
            ) {
                Text(if (isRunning) "Stop" else "Start Listening")
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(onClick = onImportClick) {
                Text("Import")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isRunning) "UDP :1514 active" else "Stopped",
                style = MaterialTheme.typography.bodySmall,
                color = if (isRunning) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            Text("$parsedEventCount parsed", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(8.dp))
            Text("${messages.size}", style = MaterialTheme.typography.labelLarge)
        }

        // Filter
        OutlinedTextField(
            value = filterText,
            onValueChange = { viewModel.setFilter(it) },
            placeholder = { Text("Filter messages...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Messages list
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(messages, key = { it.id }) { msg ->
                SyslogMessageRow(msg = msg, onClick = { onEventTap(msg) })
            }
        }
    }
}

@Composable
private fun SyslogMessageRow(msg: SyslogMessage, onClick: () -> Unit) {
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }
    val severityColor = when {
        msg.severity <= 3 -> MaterialTheme.colorScheme.error
        msg.severity == 4 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(onClick = onClick) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Row {
                Text(
                    text = timeFormat.format(Date(msg.receivedAt)),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = msg.severityLabel.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = severityColor
                )
                msg.hostname?.let { host ->
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = host,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = msg.message,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                maxLines = 3,
            )
        }
    }
    HorizontalDivider()
}
