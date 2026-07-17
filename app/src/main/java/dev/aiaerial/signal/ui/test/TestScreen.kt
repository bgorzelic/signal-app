package dev.aiaerial.signal.ui.test

import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.aiaerial.signal.ui.scanner.ScannerViewModel
import dev.aiaerial.signal.ui.theme.TextSecondary
import dev.aiaerial.signal.ui.theme.TextTertiary
import dev.aiaerial.signal.ui.theme.Void

private val ElectricGreen = Color(0xFF65F23B)
private val Surface = Color(0xFF0D100E)
private val SurfaceRaised = Color(0xFF141815)
private val Line = Color(0xFF283028)
private val HeadingFont = FontFamily(Typeface.create("sans-serif-rounded", Typeface.BOLD))
private val BodyFont = FontFamily(Typeface.create("sans-serif", Typeface.NORMAL))
private val ValueFont = FontFamily.Monospace

@Composable
fun TestScreen(
    onStartInvestigation: () -> Unit,
    viewModel: ScannerViewModel = hiltViewModel(),
) {
    val analysis by viewModel.radioAnalysis.collectAsStateWithLifecycle()
    val connection by viewModel.connectionInfo.collectAsStateWithLifecycle()
    val speed by viewModel.speedTestResult.collectAsStateWithLifecycle()
    val speedRunning by viewModel.isTestingSpeed.collectAsStateWithLifecycle()
    var engineerMode by remember { mutableStateOf(false) }
    var evidenceExpanded by remember { mutableStateOf(false) }

    val score = analysis?.healthScore
    val verdict = when {
        score == null -> "Ready to test"
        score >= 75 -> "Looking good"
        else -> "Needs attention"
    }
    val cause = when {
        analysis == null -> "Run a quick check at this spot."
        analysis!!.highCongestionChannelCount > 0 -> "Channel contention is affecting this area."
        analysis!!.medianRssi < -67 -> "Coverage is weak at this spot."
        else -> "No immediate RF issue crossed your thresholds."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Void)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
    ) {
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("SIGNAL & FLOW", color = Color.White, fontFamily = HeadingFont, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text("WIRELESS FIELD INTELLIGENCE", color = ElectricGreen, fontFamily = BodyFont, fontSize = 9.sp, letterSpacing = 1.4.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Wifi, null, tint = ElectricGreen)
                Column(Modifier.padding(start = 7.dp)) {
                    Text(if (connection == null) "Not connected" else "Connected", color = Color.White, fontSize = 12.sp)
                    Text(connection?.ssid ?: "No active SSID", color = TextSecondary, fontSize = 11.sp, fontFamily = ValueFont)
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        ModeSwitch(engineerMode) { engineerMode = it }
        Text("Same measurements. Different detail.", color = TextTertiary, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 7.dp))

        Spacer(Modifier.height(18.dp))
        Text(verdict, color = if (score != null && score >= 75) ElectricGreen else Color.White, fontFamily = HeadingFont, fontSize = 38.sp, lineHeight = 40.sp)
        Text(cause, color = TextSecondary, fontFamily = BodyFont, fontSize = 16.sp, modifier = Modifier.padding(top = 4.dp))

        Button(
            onClick = { viewModel.triggerScan(); viewModel.runSpeedTest() },
            enabled = !speedRunning,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(58.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ElectricGreen, contentColor = Color.Black),
        ) {
            Icon(if (speedRunning) Icons.Outlined.Refresh else Icons.Outlined.Bolt, null)
            Text(if (speedRunning) "CHECKING…" else "RUN QUICK CHECK", fontFamily = HeadingFont, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 9.dp))
        }
        Text(
            if (analysis == null) "No current measurement" else "Measured on this phone • ${analysis!!.networkCount} AP observations",
            color = TextTertiary,
            fontSize = 10.sp,
            modifier = Modifier.padding(vertical = 12.dp),
        )

        ResultRow(Icons.Outlined.Wifi, "Signal", analysis?.let { "${it.medianRssi} dBm median" } ?: "Not measured", analysis?.let { if (it.medianRssi >= -67) "PASS" else "CHECK" } ?: "READY")
        ResultRow(Icons.Outlined.Public, "Internet", speed?.downloadMbps?.let { "$it Mbps download" } ?: "Run active test", if (speed?.error == null && speed != null) "PASS" else "READY")
        ResultRow(Icons.Outlined.Router, "Roaming", "Connected client session", "OBSERVE")

        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp).clickable { evidenceExpanded = !evidenceExpanded },
            colors = CardDefaults.cardColors(containerColor = Surface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (engineerMode) ElectricGreen else Line),
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Analytics, null, tint = ElectricGreen)
                    Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                        Text("ENGINEER EVIDENCE", color = Color.White, fontFamily = HeadingFont, fontSize = 14.sp)
                        Text("Thresholds, sample context and provenance", color = TextTertiary, fontSize = 10.sp)
                    }
                    Icon(if (evidenceExpanded || engineerMode) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, tint = TextSecondary)
                }
                if (evidenceExpanded || engineerMode) {
                    Spacer(Modifier.height(12.dp))
                    EvidenceRow("RF CONDITIONS", "${analysis?.highCongestionChannelCount ?: 0} high-contention channels", "Android scan")
                    EvidenceRow("SIGNAL DISTRIBUTION", "Median ${analysis?.medianRssi ?: 0} dBm • σ ${analysis?.rssiStdDev ?: 0} dB", "${analysis?.networkCount ?: 0} samples")
                    EvidenceRow("PERFORMANCE", speed?.downloadMbps?.let { "$it Mbps down" } ?: "Not tested", "Active download test")
                    OutlinedButton(
                        onClick = onStartInvestigation,
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricGreen),
                    ) {
                        Text("START INVESTIGATION", color = ElectricGreen, fontFamily = HeadingFont)
                        Icon(Icons.Outlined.ChevronRight, null, tint = ElectricGreen)
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable private fun ModeSwitch(engineer: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().height(54.dp).background(Surface, RoundedCornerShape(27.dp)).border(1.dp, Line, RoundedCornerShape(27.dp))) {
        ModeChoice("SIMPLE", !engineer, Modifier.weight(1f)) { onChange(false) }
        ModeChoice("WIRELESS ENGINEER", engineer, Modifier.weight(1f)) { onChange(true) }
    }
}

@Composable private fun ModeChoice(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Row(modifier.fillMaxSize().padding(3.dp).background(if (selected) ElectricGreen else Color.Transparent, RoundedCornerShape(24.dp)).clickable(onClick = onClick), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = if (selected) Color.Black else TextSecondary, fontFamily = HeadingFont, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable private fun ResultRow(icon: ImageVector, label: String, detail: String, state: String) {
    Row(Modifier.fillMaxWidth().height(62.dp).border(width = 0.dp, color = Color.Transparent).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = ElectricGreen)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(label, color = Color.White, fontFamily = HeadingFont, fontSize = 15.sp)
            Text(detail, color = TextSecondary, fontSize = 11.sp)
        }
        Text(state, color = ElectricGreen, fontFamily = ValueFont, fontSize = 11.sp)
        Icon(Icons.Outlined.ChevronRight, null, tint = TextTertiary)
    }
}

@Composable private fun EvidenceRow(label: String, value: String, source: String) {
    Row(Modifier.fillMaxWidth().background(SurfaceRaised, RoundedCornerShape(9.dp)).padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, color = ElectricGreen, fontFamily = HeadingFont, fontSize = 10.sp)
            Text(value, color = Color.White, fontFamily = ValueFont, fontSize = 12.sp)
        }
        Text(source, color = TextTertiary, fontSize = 9.sp)
    }
    Spacer(Modifier.height(6.dp))
}
