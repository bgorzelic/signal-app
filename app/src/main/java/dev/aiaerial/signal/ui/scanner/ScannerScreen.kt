package dev.aiaerial.signal.ui.scanner

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel = hiltViewModel(),
) {
    val scanResults by viewModel.scanResults.collectAsStateWithLifecycle()
    val connectionInfo by viewModel.connectionInfo.collectAsStateWithLifecycle()
    val rssiHistory by viewModel.rssiHistory.collectAsStateWithLifecycle()
    val smoothedHistory by viewModel.smoothedRssiHistory.collectAsStateWithLifecycle()

    var permissionsGranted by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        permissionsGranted = grants.values.all { it }
        if (permissionsGranted) {
            viewModel.triggerScan()
        }
    }

    val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.CHANGE_WIFI_STATE,
    )

    val context = LocalContext.current

    // Only prompt for permissions if not already granted (avoids dialog flash on every app start)
    LaunchedEffect(Unit) {
        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            permissionsGranted = true
            viewModel.triggerScan()
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    if (!permissionsGranted && scanResults.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Location permission is required to scan WiFi networks.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { permissionLauncher.launch(permissions) }) {
                Text("Grant Permission")
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Connection info card
        item {
            Spacer(modifier = Modifier.height(8.dp))
            connectionInfo?.let { info ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Connected to ${info.ssid}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "BSSID: ${info.bssid}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            text = "RSSI: ${info.rssi} dBm  |  ${info.linkSpeed} Mbps  |  ${info.frequency} MHz",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            text = "IP: ${info.ipAddress}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }

        // Signal chart
        item {
            if (rssiHistory.isNotEmpty()) {
                Text(
                    text = "Signal Strength",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                SignalChart(dataPoints = rssiHistory, smoothedPoints = smoothedHistory)
            }
        }

        // Scan button
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = { viewModel.triggerScan() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Scan WiFi Networks")
            }
        }

        // Results count
        item {
            Text(
                text = "${scanResults.size} networks found",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        // Network list
        items(
            items = scanResults,
            key = { it.bssid },
        ) { result ->
            WifiNetworkCard(result = result)
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}
