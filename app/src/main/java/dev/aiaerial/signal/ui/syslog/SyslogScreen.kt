package dev.aiaerial.signal.ui.syslog

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import dev.aiaerial.signal.data.model.EventType
import dev.aiaerial.signal.data.model.NetworkEvent
import dev.aiaerial.signal.data.model.Vendor
import dev.aiaerial.signal.data.openclaw.OpenClawClient
import dev.aiaerial.signal.data.syslog.SyslogMessage
import dev.aiaerial.signal.ui.triage.TriageBottomSheet
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SyslogScreen(
    viewModel: SyslogViewModel = hiltViewModel(),
    openClawClient: OpenClawClient,
    onImportClick: () -> Unit = {},
) {
    var selectedMessage by remember { mutableStateOf<SyslogMessage?>(null) }
    val messages by viewModel.messages.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val filterText by viewModel.filterText.collectAsState()
    val parsedEventCount by viewModel.parsedEventCount.collectAsState()

    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not, start the service either way — notification just won't show */
        viewModel.startListening()
    }

    fun startWithPermissionCheck() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.startListening()
        }
    }

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
                    if (isRunning) viewModel.stopListening() else startWithPermissionCheck()
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
                SyslogMessageRow(msg = msg, onClick = { selectedMessage = msg })
            }
        }
    }

    selectedMessage?.let { msg ->
        val networkEvent = remember(msg.id) {
            NetworkEvent(
                timestamp = msg.receivedAt,
                eventType = EventType.UNKNOWN,
                apName = msg.hostname,
                vendor = Vendor.GENERIC,
                rawMessage = msg.raw,
                sessionId = "",
            )
        }
        TriageBottomSheet(
            event = networkEvent,
            openClawClient = openClawClient,
            onDismiss = { selectedMessage = null },
        )
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
