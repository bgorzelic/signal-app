package dev.aiaerial.signal.ui.settings

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.aiaerial.signal.data.demo.DemoScenario
import dev.aiaerial.signal.data.openclaw.OpenClawStatus
import dev.aiaerial.signal.ui.theme.AlertRed
import dev.aiaerial.signal.ui.theme.BorderSubtle
import dev.aiaerial.signal.ui.theme.Charcoal
import dev.aiaerial.signal.ui.theme.ElectricTeal
import dev.aiaerial.signal.ui.theme.Graphite
import dev.aiaerial.signal.ui.theme.SignalGreen
import dev.aiaerial.signal.ui.theme.TextPrimary
import dev.aiaerial.signal.ui.theme.TextSecondary
import dev.aiaerial.signal.ui.theme.TextTertiary
import dev.aiaerial.signal.ui.theme.Void

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val openClawStatus by viewModel.openClawStatus.collectAsState()
    val openClawUrl by viewModel.openClawUrl.collectAsState()
    val syslogPort by viewModel.syslogPort.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Void)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // Section label
        Text(
            "GATEWAY CONNECTION",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = TextTertiary,
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Graphite),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Status row with live dot
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Status", color = TextSecondary, fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val (statusText, statusColor) = when (openClawStatus) {
                            OpenClawStatus.CONNECTED -> "CONNECTED" to SignalGreen
                            OpenClawStatus.DISCONNECTED -> "OFFLINE" to AlertRed
                            OpenClawStatus.CHECKING -> "CHECKING" to ElectricTeal
                        }
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusColor),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            statusText,
                            color = statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                var urlDraft by remember { mutableStateOf(openClawUrl) }
                OutlinedTextField(
                    value = urlDraft,
                    onValueChange = { urlDraft = it },
                    label = { Text("Gateway URL", color = TextTertiary, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = BorderSubtle,
                        focusedBorderColor = ElectricTeal,
                        unfocusedContainerColor = Charcoal,
                        focusedContainerColor = Charcoal,
                        cursorColor = ElectricTeal,
                    ),
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary,
                    ),
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        viewModel.setOpenClawUrl(urlDraft)
                        viewModel.checkOpenClawHealth()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricTeal,
                        contentColor = Void,
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "TEST CONNECTION",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontSize = 12.sp,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Syslog section
        Text(
            "SYSLOG RECEIVER",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = TextTertiary,
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Graphite),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = syslogPort.toString(),
                    onValueChange = { it.toIntOrNull()?.let { p -> viewModel.setSyslogPort(p) } },
                    label = { Text("UDP Port", color = TextTertiary, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = BorderSubtle,
                        focusedBorderColor = ElectricTeal,
                        unfocusedContainerColor = Charcoal,
                        focusedContainerColor = Charcoal,
                        cursorColor = ElectricTeal,
                    ),
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary,
                    ),
                )
                Text(
                    "Configure your WLC to send syslog to this device's IP on this port.",
                    fontSize = 11.sp,
                    color = TextTertiary,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        // Demo mode
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "DEMO MODE",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = TextTertiary,
        )
        Spacer(modifier = Modifier.height(8.dp))

        val demoMode by viewModel.demoMode.collectAsState()
        val demoScenarioIndex by viewModel.demoScenarioIndex.collectAsState()

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Graphite),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Sample Data Mode", fontSize = 13.sp)
                        Text(
                            "Use seeded data without live infrastructure",
                            fontSize = 11.sp,
                            color = TextTertiary,
                        )
                    }
                    Switch(
                        checked = demoMode,
                        onCheckedChange = { viewModel.setDemoMode(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Void,
                            checkedTrackColor = ElectricTeal,
                            uncheckedThumbColor = TextTertiary,
                            uncheckedTrackColor = Graphite,
                        ),
                    )
                }

                if (demoMode) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "SCENARIO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = TextTertiary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    DemoScenario.entries.forEachIndexed { index, scenario ->
                        val selected = index == demoScenarioIndex
                        androidx.compose.material3.Surface(
                            onClick = { viewModel.setDemoScenario(index) },
                            color = androidx.compose.ui.graphics.Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            if (selected) ElectricTeal else TextTertiary,
                                            CircleShape,
                                        ),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        scenario.label,
                                        fontSize = 13.sp,
                                        color = if (selected) ElectricTeal else TextSecondary,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    )
                                    Text(scenario.description, fontSize = 10.sp, color = TextTertiary)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Restart app after changing scenario",
                        fontSize = 10.sp,
                        color = TextTertiary,
                    )
                }
            }
        }

        // Alert thresholds
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "ALERT THRESHOLDS",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = TextTertiary,
        )
        Spacer(modifier = Modifier.height(8.dp))

        val rssiThreshold by viewModel.alertRssiThreshold.collectAsState()
        val roamChurnCount by viewModel.alertRoamChurnCount.collectAsState()
        val roamWindow by viewModel.alertRoamWindowMinutes.collectAsState()
        val authFailCount by viewModel.alertAuthFailureCount.collectAsState()

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Graphite),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                ThresholdRow("Weak Signal", "${rssiThreshold} dBm",
                    onMinus = { if (rssiThreshold > -90) viewModel.setAlertRssiThreshold(rssiThreshold - 5) },
                    onPlus = { if (rssiThreshold < -20) viewModel.setAlertRssiThreshold(rssiThreshold + 5) })
                ThresholdRow("Roam Churn", "$roamChurnCount roams",
                    onMinus = { if (roamChurnCount > 1) viewModel.setAlertRoamChurnCount(roamChurnCount - 1) },
                    onPlus = { viewModel.setAlertRoamChurnCount(roamChurnCount + 1) })
                ThresholdRow("Roam Window", "${roamWindow} min",
                    onMinus = { if (roamWindow > 5) viewModel.setAlertRoamWindowMinutes(roamWindow - 5) },
                    onPlus = { viewModel.setAlertRoamWindowMinutes(roamWindow + 5) })
                ThresholdRow("Auth Failures", "$authFailCount failures",
                    onMinus = { if (authFailCount > 1) viewModel.setAlertAuthFailureCount(authFailCount - 1) },
                    onPlus = { viewModel.setAlertAuthFailureCount(authFailCount + 1) })
            }
        }

        // Setup guide (when disconnected)
        if (openClawStatus == OpenClawStatus.DISCONNECTED) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "SETUP GUIDE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = TextTertiary,
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AlertRed.copy(alpha = 0.08f)),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "OpenClaw gateway is not reachable.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = AlertRed,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val steps = listOf(
                        "Install Termux from F-Droid",
                        "pkg install nodejs-lts",
                        "npx openclaw@latest init",
                        "npx openclaw gateway start",
                        "Return here → Test Connection",
                    )
                    steps.forEachIndexed { i, step ->
                        Text(
                            "${i + 1}. $step",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontFamily = if (i in 1..3) FontFamily.Monospace else FontFamily.Default,
                            modifier = Modifier.padding(vertical = 1.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "github.com/bgorzelic/openclaw-android-edge",
                        fontSize = 11.sp,
                        color = ElectricTeal,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // About — minimal
        Text(
            "ABOUT",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = TextTertiary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text("SIGNAL v0.5.0", fontSize = 12.sp, color = TextTertiary, fontFamily = FontFamily.Monospace)
        Text("AI Aerial Solutions", fontSize = 11.sp, color = TextTertiary)
    }
}

@Composable
private fun ThresholdRow(
    label: String,
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 12.sp, color = TextSecondary, modifier = Modifier.weight(1f))
        Text(
            value,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = ElectricTeal,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.width(8.dp))
        androidx.compose.material3.IconButton(
            onClick = onMinus,
            modifier = Modifier.size(28.dp),
        ) {
            Text("-", fontSize = 16.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
        }
        androidx.compose.material3.IconButton(
            onClick = onPlus,
            modifier = Modifier.size(28.dp),
        ) {
            Text("+", fontSize = 16.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
        }
    }
}
