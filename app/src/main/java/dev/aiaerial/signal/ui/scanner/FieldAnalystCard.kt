package dev.aiaerial.signal.ui.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aiaerial.signal.data.wifi.RadioAnalysis
import dev.aiaerial.signal.ui.theme.AlertRed
import dev.aiaerial.signal.ui.theme.ElectricTeal
import dev.aiaerial.signal.ui.theme.Graphite
import dev.aiaerial.signal.ui.theme.SignalGreen
import dev.aiaerial.signal.ui.theme.TextSecondary
import dev.aiaerial.signal.ui.theme.TextTertiary

@Composable
fun FieldAnalystCard(analysis: RadioAnalysis) {
    val scoreColor = when {
        analysis.healthScore >= 75 -> SignalGreen
        analysis.healthScore >= 55 -> ElectricTeal
        else -> AlertRed
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Graphite),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("FIELD ANALYST", color = ElectricTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("LOCAL STATISTICAL ASSESSMENT", color = TextTertiary, fontSize = 9.sp, letterSpacing = 1.sp)
                }
                Column {
                    Text("${analysis.healthScore}", color = scoreColor, fontSize = 28.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                    Text(analysis.grade, color = scoreColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(scoreColor.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Metric("APS", analysis.networkCount.toString())
                Metric("SSIDS", analysis.ssidCount.toString())
                Metric("MEDIAN", "${analysis.medianRssi} dBm")
                Metric("σ RSSI", "${analysis.rssiStdDev} dB")
            }
            analysis.findings.take(3).forEach { finding ->
                Text("› $finding", style = MaterialTheme.typography.bodySmall, color = TextSecondary, lineHeight = 17.sp)
            }
            Text(
                "Computed on-device from the current scan • no cloud required",
                color = TextTertiary,
                fontSize = 9.sp,
                letterSpacing = .4.sp,
            )
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Column {
        Text(label, color = TextTertiary, fontSize = 8.sp, letterSpacing = 1.sp)
        Text(value, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}
