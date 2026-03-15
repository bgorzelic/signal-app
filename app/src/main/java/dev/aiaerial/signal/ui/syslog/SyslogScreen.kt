package dev.aiaerial.signal.ui.syslog

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import dev.aiaerial.signal.data.model.EventType
import dev.aiaerial.signal.data.model.NetworkEvent
import dev.aiaerial.signal.data.model.Vendor
import dev.aiaerial.signal.data.openclaw.OpenClawClient
import dev.aiaerial.signal.data.syslog.SyslogMessage
import dev.aiaerial.signal.ui.theme.AlertRed
import dev.aiaerial.signal.ui.theme.BorderSubtle
import dev.aiaerial.signal.ui.theme.ElectricTeal
import dev.aiaerial.signal.ui.theme.Ember
import dev.aiaerial.signal.ui.theme.GhostGreen
import dev.aiaerial.signal.ui.theme.Graphite
import dev.aiaerial.signal.ui.theme.SignalTheme
import dev.aiaerial.signal.ui.theme.TextPrimary
import dev.aiaerial.signal.ui.theme.TextSecondary
import dev.aiaerial.signal.ui.theme.TextTertiary
import dev.aiaerial.signal.ui.theme.Void
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
    ) {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Void),
    ) {
        // Status strip — live indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Graphite)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Live pulse dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isRunning) GhostGreen.copy(alpha = 0.8f) else TextTertiary),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isRunning) "LIVE" else "IDLE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = if (isRunning) GhostGreen else TextTertiary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isRunning) "UDP :1514" else "Stopped",
                fontSize = 10.sp,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "$parsedEventCount parsed",
                fontSize = 10.sp,
                color = TextTertiary,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "${messages.size}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ElectricTeal,
                fontFamily = FontFamily.Monospace,
            )
        }

        // Controls row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = {
                    if (isRunning) viewModel.stopListening() else startWithPermissionCheck()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) AlertRed.copy(alpha = 0.15f) else ElectricTeal,
                    contentColor = if (isRunning) AlertRed else Void,
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = if (isRunning) "STOP" else "START",
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontSize = 12.sp,
                )
            }
            OutlinedButton(
                onClick = onImportClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextSecondary,
                ),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true),
            ) {
                Text(
                    "IMPORT",
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                    fontSize = 12.sp,
                )
            }
        }

        // Filter
        OutlinedTextField(
            value = filterText,
            onValueChange = { viewModel.setFilter(it) },
            placeholder = {
                Text(
                    "Filter messages...",
                    color = TextTertiary,
                    fontSize = 13.sp,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = BorderSubtle,
                focusedBorderColor = ElectricTeal,
                unfocusedContainerColor = Graphite,
                focusedContainerColor = Graphite,
                cursorColor = ElectricTeal,
            ),
            textStyle = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                color = TextPrimary,
            ),
        )

        Spacer(modifier = Modifier.height(4.dp))

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
        msg.severity <= 3 -> AlertRed
        msg.severity == 4 -> Ember
        else -> TextPrimary
    }

    Surface(
        onClick = onClick,
        color = Void,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 5.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Severity dot
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(severityColor.copy(alpha = 0.7f)),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = timeFormat.format(Date(msg.receivedAt)),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextTertiary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = msg.severityLabel.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = severityColor,
                )
                msg.hostname?.let { host ->
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = host,
                        fontSize = 10.sp,
                        color = ElectricTeal,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Text(
                text = msg.message,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary,
                maxLines = 3,
                lineHeight = 15.sp,
                modifier = Modifier.padding(start = 12.dp, top = 2.dp),
            )
        }
    }
    HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
}
