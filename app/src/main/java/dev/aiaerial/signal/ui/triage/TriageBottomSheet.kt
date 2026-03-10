package dev.aiaerial.signal.ui.triage

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.aiaerial.signal.data.model.NetworkEvent
import dev.aiaerial.signal.data.openclaw.OpenClawClient
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

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text("AI Triage", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("${event.eventType.name} — ${event.clientMac ?: "unknown client"}")
                    event.apName?.let { Text("AP: $it") }
                    Text(
                        event.rawMessage,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 2
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Text(
                    text = analysis ?: "No analysis available",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
