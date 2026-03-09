package dev.aiaerial.signal.ui.logimport

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LogImportScreen(viewModel: LogImportViewModel = hiltViewModel()) {
    val logText by viewModel.logText.collectAsState()
    val parsedEvents by viewModel.parsedEvents.collectAsState()
    val aiAnalysis by viewModel.aiAnalysis.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Import Logs", style = MaterialTheme.typography.titleMedium)
        Text(
            "Paste WLC debug output (e.g. 'debug client mac-address', show commands)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = logText,
            onValueChange = { viewModel.setLogText(it) },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            placeholder = { Text("Paste log output here...") },
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.parseLog() }) {
                Text("Parse Events")
            }
            OutlinedButton(onClick = { viewModel.analyzeWithAi() }, enabled = !isAnalyzing) {
                Text(if (isAnalyzing) "Analyzing..." else "AI Analysis")
            }
            TextButton(onClick = { viewModel.clear() }) {
                Text("Clear")
            }
        }

        // Parsed events summary
        if (parsedEvents.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "${parsedEvents.size} events parsed",
                style = MaterialTheme.typography.titleSmall,
            )
            parsedEvents.groupBy { it.eventType }.forEach { (type, events) ->
                Text(
                    "  ${type.name}: ${events.size}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        // AI analysis result
        aiAnalysis?.let { analysis ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("AI Analysis", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(analysis, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
