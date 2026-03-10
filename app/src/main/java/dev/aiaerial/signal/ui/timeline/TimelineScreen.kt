package dev.aiaerial.signal.ui.timeline

import android.content.Intent
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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

    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.US) }

    // Handle export result via Android Share sheet
    LaunchedEffect(exportResult) {
        exportResult?.let { (mimeType, content) ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_TEXT, content)
                putExtra(
                    Intent.EXTRA_SUBJECT,
                    "SIGNAL session export"
                )
            }
            context.startActivity(Intent.createChooser(intent, "Export session data"))
            viewModel.clearExportResult()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Session picker
        if (sessions.size > 1) {
            SessionPicker(
                sessions = sessions,
                selectedSessionId = selectedSessionId,
                dateFormat = dateFormat,
                onSelect = { viewModel.selectSession(it) },
            )
        }

        if (clients.isEmpty() && allEvents.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text("No clients detected yet.\nStart the syslog receiver to capture roaming events.")
            }
        } else {
            // Export row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "${allEvents.size} events in session",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row {
                    TextButton(onClick = { viewModel.exportCsv() }) {
                        Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.height(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CSV")
                    }
                    TextButton(onClick = { viewModel.exportJson() }) {
                        Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.height(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("JSON")
                    }
                }
            }

            // View mode tabs
            var selectedTab by remember { mutableStateOf(0) }
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("Client Journey", modifier = Modifier.padding(12.dp))
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("AP Map", modifier = Modifier.padding(12.dp))
                }
            }

            when (selectedTab) {
                0 -> {
                    // Client picker
                    Text(
                        "Select client MAC:",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                    )

                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    ) {
                        OutlinedTextField(
                            value = selectedClient ?: "Select a client...",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            clients.forEach { mac ->
                                DropdownMenuItem(
                                    text = { Text(mac) },
                                    onClick = {
                                        viewModel.selectClient(mac)
                                        expanded = false
                                    },
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (events.isNotEmpty()) {
                        Text(
                            "${events.size} events for ${selectedClient ?: ""}",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(events, key = { it.id }) { event ->
                            RoamingTimelineCard(event = event)
                        }
                    }
                }

                1 -> {
                    // AP association map
                    if (apAssociations.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("No active AP associations in this session.")
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
                                ApMapCard(association = association)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionPicker(
    sessions: List<dev.aiaerial.signal.data.local.SessionSummary>,
    selectedSessionId: String,
    dateFormat: SimpleDateFormat,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedSession = sessions.find { it.sessionId == selectedSessionId }
    val label = selectedSession?.let {
        "${dateFormat.format(Date(it.timestamp))} (${it.eventCount} events)"
    } ?: "Current session"

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
            label = { Text("Session") },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            sessions.forEach { session ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                dateFormat.format(Date(session.timestamp)),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                "${session.eventCount} events",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
