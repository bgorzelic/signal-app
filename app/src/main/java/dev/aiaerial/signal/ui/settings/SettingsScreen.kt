package dev.aiaerial.signal.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.aiaerial.signal.data.openclaw.OpenClawStatus

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val openClawStatus by viewModel.openClawStatus.collectAsState()
    val openClawUrl by viewModel.openClawUrl.collectAsState()
    val syslogPort by viewModel.syslogPort.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // OpenClaw Connection
        Text("OpenClaw Connection", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Status")
                    val (statusText, statusColor) = when (openClawStatus) {
                        OpenClawStatus.CONNECTED -> "Connected" to MaterialTheme.colorScheme.primary
                        OpenClawStatus.DISCONNECTED -> "Disconnected" to MaterialTheme.colorScheme.error
                        OpenClawStatus.CHECKING -> "Checking..." to MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Text(statusText, color = statusColor)
                }

                Spacer(modifier = Modifier.height(8.dp))

                var urlDraft by remember { mutableStateOf(openClawUrl) }
                OutlinedTextField(
                    value = urlDraft,
                    onValueChange = { urlDraft = it },
                    label = { Text("Gateway URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = {
                    viewModel.setOpenClawUrl(urlDraft)
                    viewModel.checkOpenClawHealth()
                }) {
                    Text("Save & Test Connection")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Syslog Settings
        Text("Syslog Receiver", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = syslogPort.toString(),
                    onValueChange = { it.toIntOrNull()?.let { p -> viewModel.setSyslogPort(p) } },
                    label = { Text("UDP Port") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Text(
                    "Configure your WLC to send syslog to this device's IP on this port.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Setup guide (shown when disconnected)
        if (openClawStatus == OpenClawStatus.DISCONNECTED) {
            Text("Setup Guide", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "OpenClaw is not running. To enable AI features:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("1. Install Termux from F-Droid")
                    Text("2. In Termux, run: pkg install nodejs-lts")
                    Text("3. Run: npx openclaw@latest init")
                    Text("4. Run: npx openclaw gateway start")
                    Text("5. Come back here and tap 'Test Connection'")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Full guide: github.com/bgorzelic/openclaw-android-edge",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // App info
        Text("About", style = MaterialTheme.typography.titleMedium)
        Text("SIGNAL v0.1.0", style = MaterialTheme.typography.bodySmall)
        Text("AI Aerial Solutions", style = MaterialTheme.typography.bodySmall)
    }
}
