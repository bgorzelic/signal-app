package dev.aiaerial.signal.ui.timeline

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.clickable
import dev.aiaerial.signal.ui.components.AlertSection
import dev.aiaerial.signal.ui.theme.BorderSubtle
import dev.aiaerial.signal.ui.theme.Charcoal
import dev.aiaerial.signal.ui.theme.ElectricTeal
import dev.aiaerial.signal.ui.theme.Graphite
import dev.aiaerial.signal.ui.theme.TextPrimary
import dev.aiaerial.signal.ui.theme.TextSecondary
import dev.aiaerial.signal.ui.theme.TextTertiary
import dev.aiaerial.signal.ui.theme.Void
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(viewModel: TimelineViewModel = hiltViewModel()) {
    val sessions by viewModel.sessions.collectAsState()
    val selectedSessionId by viewModel.selectedSessionId.collectAsState()
    val clients by viewModel.clients.collectAsState()
    val selectedClient by viewModel.selectedClient.collectAsState()
    val events by viewModel.clientEvents.collectAsState()
    val allEvents by viewModel.allSessionEvents.collectAsState()
    val apAssociations by viewModel.apAssociations.collectAsState()
    val exportResult by viewModel.exportResult.collectAsState()
    val alerts by viewModel.alerts.collectAsState()
    var clientForDetail by remember { mutableStateOf<String?>(null) }
    var apForDetail by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.US) }

    LaunchedEffect(exportResult) {
        exportResult?.let { (mimeType, content) ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_TEXT, content)
                putExtra(Intent.EXTRA_SUBJECT, "SIGNAL session export")
            }
            context.startActivity(Intent.createChooser(intent, "Export session data"))
            viewModel.clearExportResult()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Void),
    ) {
        // Session picker
        if (sessions.size > 1) {
            SessionPicker(
                sessions = sessions,
                selectedSessionId = selectedSessionId,
                dateFormat = dateFormat,
                onSelect = { viewModel.selectSession(it) },
                onDelete = { viewModel.deleteSession(it) },
            )
        }

        if (clients.isEmpty() && allEvents.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "No events captured",
                        fontSize = 14.sp,
                        color = TextSecondary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Start the syslog receiver to capture roaming events.",
                        fontSize = 12.sp,
                        color = TextTertiary,
                    )
                }
            }
        } else {
            // Export row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = { viewModel.newSession() }) {
                    Text(
                        "NEW SESSION",
                        fontSize = 11.sp,
                        color = ElectricTeal,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    )
                }
                Row {
                    TextButton(onClick = { viewModel.exportCsv() }) {
                        Icon(
                            Icons.Outlined.Share,
                            contentDescription = null,
                            modifier = Modifier.height(14.dp),
                            tint = TextSecondary,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CSV", fontSize = 11.sp, color = TextSecondary)
                    }
                    TextButton(onClick = { viewModel.exportJson() }) {
                        Icon(
                            Icons.Outlined.Share,
                            contentDescription = null,
                            modifier = Modifier.height(14.dp),
                            tint = TextSecondary,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("JSON", fontSize = 11.sp, color = TextSecondary)
                    }
                    TextButton(onClick = { viewModel.exportMarkdownReport() }) {
                        Icon(
                            Icons.Outlined.Share,
                            contentDescription = null,
                            modifier = Modifier.height(14.dp),
                            tint = ElectricTeal,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("REPORT", fontSize = 11.sp, color = ElectricTeal)
                    }
                }
            }

            // Alerts
            if (alerts.isNotEmpty()) {
                AlertSection(
                    alerts = alerts,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            // View mode tabs
            var selectedTab by remember { mutableStateOf(0) }
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Void,
                contentColor = ElectricTeal,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = ElectricTeal,
                            height = 2.dp,
                        )
                    }
                },
                divider = {},
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    selectedContentColor = ElectricTeal,
                    unselectedContentColor = TextTertiary,
                ) {
                    Text(
                        "CLIENT JOURNEY",
                        modifier = Modifier.padding(12.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                }
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    selectedContentColor = ElectricTeal,
                    unselectedContentColor = TextTertiary,
                ) {
                    Text(
                        "AP MAP",
                        modifier = Modifier.padding(12.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    Text(
                        "SELECT CLIENT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = TextTertiary,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                    )

                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        OutlinedTextField(
                            value = selectedClient ?: "Select a client...",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = BorderSubtle,
                                focusedBorderColor = ElectricTeal,
                                unfocusedContainerColor = Graphite,
                                focusedContainerColor = Graphite,
                            ),
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = TextPrimary),
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            clients.forEach { mac ->
                                DropdownMenuItem(
                                    text = { Text(mac, color = TextPrimary) },
                                    onClick = {
                                        viewModel.selectClient(mac)
                                        expanded = false
                                    },
                                )
                            }
                        }
                    }

                    if (events.isNotEmpty()) {
                        Text(
                            "${events.size} events for ${selectedClient ?: ""}",
                            fontSize = 10.sp,
                            color = TextTertiary,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(events, key = { "${it.id}-${it.timestamp}" }) { event ->
                            RoamingTimelineCard(
                                event = event,
                                modifier = Modifier.clickable {
                                    event.clientMac?.let { clientForDetail = it }
                                },
                                onApClick = { apName -> apForDetail = apName },
                            )
                        }
                    }
                }

                1 -> {
                    if (apAssociations.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "No active AP associations.",
                                fontSize = 12.sp,
                                color = TextTertiary,
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        ) {
                            items(
                                items = apAssociations,
                                key = { it.apName },
                            ) { association ->
                                ApMapCard(
                                    association = association,
                                    modifier = Modifier.clickable {
                                        apForDetail = association.apName
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Client detail bottom sheet
    clientForDetail?.let { mac ->
        ClientDetailSheet(
            clientMac = mac,
            events = allEvents,
            onDismiss = { clientForDetail = null },
        )
    }

    // AP detail bottom sheet
    apForDetail?.let { ap ->
        ApDetailSheet(
            apName = ap,
            events = allEvents,
            onDismiss = { apForDetail = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun SessionPicker(
    sessions: List<dev.aiaerial.signal.data.local.SessionSummary>,
    selectedSessionId: String,
    dateFormat: SimpleDateFormat,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var sessionToDelete by remember { mutableStateOf<dev.aiaerial.signal.data.local.SessionSummary?>(null) }
    val selectedSession = sessions.find { it.sessionId == selectedSessionId }
    val label = selectedSession?.let {
        "${dateFormat.format(Date(it.timestamp))} (${it.eventCount} events)"
    } ?: "Current session"

    // Delete confirmation dialog
    sessionToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = {
                Text(
                    "Delete session?",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            },
            text = {
                Text(
                    "${dateFormat.format(Date(target.timestamp))} — ${target.eventCount} events will be permanently deleted.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(target.sessionId)
                        sessionToDelete = null
                    },
                ) {
                    Text("DELETE", color = androidx.compose.ui.graphics.Color(0xFFFF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("CANCEL", color = TextTertiary)
                }
            },
            containerColor = Charcoal,
        )
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Session", color = TextTertiary, fontSize = 11.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = BorderSubtle,
                focusedBorderColor = ElectricTeal,
                unfocusedContainerColor = Graphite,
                focusedContainerColor = Graphite,
            ),
            textStyle = MaterialTheme.typography.bodySmall.copy(color = TextPrimary),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            sessions.forEach { session ->
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    dateFormat.format(Date(session.timestamp)),
                                    fontSize = 13.sp,
                                    color = TextPrimary,
                                )
                                Text(
                                    "${session.eventCount} events",
                                    fontSize = 10.sp,
                                    color = TextTertiary,
                                )
                            }
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = "Delete session",
                                modifier = Modifier
                                    .height(18.dp)
                                    .combinedClickable(
                                        onClick = {
                                            sessionToDelete = session
                                            expanded = false
                                        },
                                    ),
                                tint = TextTertiary,
                            )
                        }
                    },
                    onClick = {
                        onSelect(session.sessionId)
                        expanded = false
                    },
                )
            }
        }
    }
}
