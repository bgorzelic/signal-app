package dev.aiaerial.signal.ui.scanner

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.aiaerial.signal.ui.components.AlertSection
import dev.aiaerial.signal.ui.theme.BorderSubtle
import dev.aiaerial.signal.ui.theme.ElectricTeal
import dev.aiaerial.signal.ui.theme.SignalGreen
import dev.aiaerial.signal.ui.theme.Graphite
import dev.aiaerial.signal.ui.theme.SignalTheme
import dev.aiaerial.signal.ui.theme.TextSecondary
import dev.aiaerial.signal.ui.theme.TextTertiary
import dev.aiaerial.signal.ui.theme.Void

@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel = hiltViewModel(),
) {
    val scanResults by viewModel.scanResults.collectAsStateWithLifecycle()
    val connectionInfo by viewModel.connectionInfo.collectAsStateWithLifecycle()
    val rssiHistory by viewModel.rssiHistory.collectAsStateWithLifecycle()
    val smoothedHistory by viewModel.smoothedRssiHistory.collectAsStateWithLifecycle()
    val channelUtilization by viewModel.channelUtilization.collectAsStateWithLifecycle()
    val colors = SignalTheme.colors
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()
    val autoScanEnabled by viewModel.autoScanEnabled.collectAsStateWithLifecycle()
    val autoScanIntervalSec by viewModel.autoScanIntervalSec.collectAsStateWithLifecycle()

    val savedSnapshots by viewModel.savedSnapshots.collectAsStateWithLifecycle()
    var permissionsGranted by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

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
            modifier = Modifier
                .fillMaxSize()
                .background(Void)
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Location permission required",
                style = MaterialTheme.typography.titleSmall,
                color = TextSecondary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "WiFi scanning needs location access to discover nearby networks.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = TextTertiary,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { permissionLauncher.launch(permissions) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricTeal,
                    contentColor = Void,
                ),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    "GRANT PERMISSION",
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Void)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // Connection info card — status surface with left accent border
        item {
            connectionInfo?.let { info ->
                val rssiColor = when {
                    info.rssi >= -50 -> colors.signalExcellent
                    info.rssi >= -60 -> colors.signalGood
                    info.rssi >= -70 -> colors.signalFair
                    else -> colors.signalPoor
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Graphite),
                ) {
                    Row(modifier = Modifier.padding(start = 0.dp)) {
                        // Accent left border
                        Spacer(
                            modifier = Modifier
                                .width(3.dp)
                                .height(96.dp)
                                .background(rssiColor, RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)),
                        )
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = info.ssid,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "${info.rssi} dBm",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = rssiColor,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "${info.linkSpeed} Mbps",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                )
                                Text(
                                    text = "${info.frequency} MHz",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = info.bssid,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = TextTertiary,
                                )
                                Text(
                                    text = info.ipAddress,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = TextTertiary,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Speed test card
        item {
            val speedResult by viewModel.speedTestResult.collectAsStateWithLifecycle()
            val isTesting by viewModel.isTestingSpeed.collectAsStateWithLifecycle()

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Graphite),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "SPEED TEST",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            color = TextTertiary,
                        )
                        when {
                            isTesting -> Text(
                                "Testing...",
                                fontSize = 13.sp,
                                color = ElectricTeal,
                            )
                            speedResult != null -> {
                                val r = speedResult!!
                                if (r.error != null) {
                                    Text(r.error, fontSize = 12.sp, color = dev.aiaerial.signal.ui.theme.AlertRed)
                                } else {
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Text(
                                            "${r.downloadMbps} Mbps",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = ElectricTeal,
                                        )
                                        Text(
                                            "${r.durationMs}ms",
                                            fontSize = 12.sp,
                                            color = TextTertiary,
                                            modifier = Modifier.align(Alignment.Bottom),
                                        )
                                    }
                                }
                            }
                            else -> Text(
                                "Tap to measure download speed",
                                fontSize = 12.sp,
                                color = TextSecondary,
                            )
                        }
                    }
                    androidx.compose.material3.TextButton(
                        onClick = { viewModel.runSpeedTest() },
                        enabled = !isTesting,
                    ) {
                        Text(
                            if (isTesting) "..." else "RUN",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = if (isTesting) TextTertiary else ElectricTeal,
                        )
                    }
                }
            }
        }

        // Signal chart with section label
        item {
            if (rssiHistory.isNotEmpty()) {
                Text(
                    text = "SIGNAL STRENGTH",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 2.sp,
                    color = TextTertiary,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                SignalChart(dataPoints = rssiHistory, smoothedPoints = smoothedHistory)
            }
        }

        // Channel utilization
        item {
            if (channelUtilization.isNotEmpty()) {
                ChannelUtilizationCard(utilization = channelUtilization)
            }
        }

        // Alerts
        item {
            AlertSection(alerts = alerts)
        }

        // Scan button or auto-scanning indicator — tactical style
        item {
            if (autoScanEnabled) {
                // Pulsing AUTO-SCANNING label replaces the button
                val infiniteTransition = rememberInfiniteTransition(label = "autoScanPulse")
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 0.3f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "pulseAlpha",
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ElectricTeal.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "AUTO-SCANNING",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontSize = 13.sp,
                        color = ElectricTeal,
                        modifier = Modifier.alpha(pulseAlpha),
                    )
                }
            } else {
                Button(
                    onClick = {
                        if (!isScanning) {
                            isScanning = true
                            viewModel.triggerScan()
                            coroutineScope.launch {
                                delay(2_000L)
                                isScanning = false
                            }
                        }
                    },
                    enabled = !isScanning,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricTeal,
                        contentColor = Void,
                        disabledContainerColor = ElectricTeal.copy(alpha = 0.3f),
                        disabledContentColor = Void.copy(alpha = 0.5f),
                    ),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = if (isScanning) "SCANNING..." else "SCAN NETWORKS",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontSize = 13.sp,
                    )
                }
            }
        }

        // Auto-scan controls row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Graphite, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "AUTO",
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    fontSize = 11.sp,
                    color = if (autoScanEnabled) ElectricTeal else TextTertiary,
                )
                Switch(
                    checked = autoScanEnabled,
                    onCheckedChange = { viewModel.setAutoScan(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Void,
                        checkedTrackColor = ElectricTeal,
                        uncheckedThumbColor = TextTertiary,
                        uncheckedTrackColor = Void,
                        uncheckedBorderColor = TextTertiary,
                    ),
                )
                Text(
                    text = "${autoScanIntervalSec}s",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = if (autoScanEnabled) ElectricTeal else TextTertiary,
                )
            }
        }

        // Results count + save snapshot + history
        item {
            val snapshotSaved by viewModel.snapshotSaved.collectAsStateWithLifecycle()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${scanResults.size} networks detected",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    letterSpacing = 1.sp,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (savedSnapshots.isNotEmpty()) {
                        androidx.compose.material3.TextButton(
                            onClick = { showHistorySheet = true },
                        ) {
                            Text(
                                text = "HISTORY (${savedSnapshots.size})",
                                fontSize = 11.sp,
                                color = ElectricTeal,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                            )
                        }
                    }
                    androidx.compose.material3.TextButton(
                        onClick = { viewModel.saveSnapshot() },
                    ) {
                        Text(
                            text = if (snapshotSaved) "SAVED" else "SAVE SNAPSHOT",
                            fontSize = 11.sp,
                            color = if (snapshotSaved) SignalGreen else ElectricTeal,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                        )
                    }
                }
            }
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

    if (showHistorySheet) {
        SnapshotViewerSheet(
            snapshots = savedSnapshots,
            onDelete = { id -> viewModel.deleteSnapshot(id) },
            onDismiss = { showHistorySheet = false },
        )
    }
}
