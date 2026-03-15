package dev.aiaerial.signal.ui.triage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aiaerial.signal.data.model.NetworkEvent
import dev.aiaerial.signal.data.openclaw.OpenClawClient
import dev.aiaerial.signal.ui.theme.Charcoal
import dev.aiaerial.signal.ui.theme.ElectricTeal
import dev.aiaerial.signal.ui.theme.Graphite
import dev.aiaerial.signal.ui.theme.PhantomViolet
import dev.aiaerial.signal.ui.theme.TextSecondary
import dev.aiaerial.signal.ui.theme.TextTertiary
import dev.aiaerial.signal.ui.theme.Void
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriageBottomSheet(
    event: NetworkEvent,
    openClawClient: OpenClawClient,
    onDismiss: () -> Unit,
) {
    var analysis by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(event) {
        isLoading = true
        analysis = try {
            openClawClient.triageEvent(event)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            "Error: ${e.message}\n\nIs OpenClaw running on localhost:18789?"
        }
        isLoading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Graphite,
        scrimColor = Void.copy(alpha = 0.6f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
        ) {
            // Header with AI badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "AI TRIAGE",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = PhantomViolet,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Event card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Charcoal),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "${event.eventType.name} — ${event.clientMac ?: "unknown"}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = ElectricTeal,
                    )
                    event.apName?.let {
                        Text("AP: $it", fontSize = 12.sp, color = TextSecondary)
                    }
                    Text(
                        event.rawMessage,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextTertiary,
                        maxLines = 3,
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = PhantomViolet,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(28.dp),
                    )
                }
            } else {
                Text(
                    text = analysis ?: "No analysis available",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
