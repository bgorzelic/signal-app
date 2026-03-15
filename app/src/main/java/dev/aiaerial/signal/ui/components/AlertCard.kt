package dev.aiaerial.signal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aiaerial.signal.data.alert.Alert
import dev.aiaerial.signal.data.alert.AlertSeverity
import dev.aiaerial.signal.ui.theme.AlertRed
import dev.aiaerial.signal.ui.theme.Ember
import dev.aiaerial.signal.ui.theme.Graphite
import dev.aiaerial.signal.ui.theme.IceBlue
import dev.aiaerial.signal.ui.theme.TextSecondary

@Composable
fun AlertCard(
    alert: Alert,
    modifier: Modifier = Modifier,
) {
    val accentColor = when (alert.severity) {
        AlertSeverity.CRITICAL -> AlertRed
        AlertSeverity.WARNING -> Ember
        AlertSeverity.INFO -> IceBlue
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.08f)),
    ) {
        Row(modifier = Modifier.padding(start = 0.dp)) {
            // Left accent bar
            Spacer(
                modifier = Modifier
                    .width(3.dp)
                    .height(52.dp)
                    .background(accentColor, RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp)),
            )
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(
                    text = alert.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                )
                Text(
                    text = alert.detail,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 15.sp,
                )
            }
        }
    }
}

@Composable
fun AlertSection(
    alerts: List<Alert>,
    modifier: Modifier = Modifier,
) {
    if (alerts.isEmpty()) return
    Column(modifier = modifier) {
        Text(
            text = "${alerts.size} ALERT${if (alerts.size != 1) "S" else ""}",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = when {
                alerts.any { it.severity == AlertSeverity.CRITICAL } -> AlertRed
                else -> Ember
            },
            modifier = Modifier.padding(bottom = 6.dp),
        )
        alerts.forEachIndexed { index, alert ->
            AlertCard(alert = alert)
            if (index < alerts.lastIndex) {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}
