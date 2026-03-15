package dev.aiaerial.signal.ui.logimport

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.aiaerial.signal.ui.theme.BorderSubtle
import dev.aiaerial.signal.ui.theme.Charcoal
import dev.aiaerial.signal.ui.theme.ElectricTeal
import dev.aiaerial.signal.ui.theme.Graphite
import dev.aiaerial.signal.ui.theme.PhantomViolet
import dev.aiaerial.signal.ui.theme.TextPrimary
import dev.aiaerial.signal.ui.theme.TextSecondary
import dev.aiaerial.signal.ui.theme.TextTertiary
import dev.aiaerial.signal.ui.theme.Void

@Composable
fun LogImportScreen(
    viewModel: LogImportViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
) {
    val logText by viewModel.logText.collectAsState()
    val parsedEvents by viewModel.parsedEvents.collectAsState()
    val aiAnalysis by viewModel.aiAnalysis.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Void)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextSecondary,
                )
            }
            Text(
                "IMPORT LOGS",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            )
        }
        Text(
            "Paste WLC debug output (e.g. 'debug client mac-address', show commands)",
            fontSize = 11.sp,
            color = TextTertiary,
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = logText,
            onValueChange = { viewModel.setLogText(it) },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            placeholder = {
                Text(
                    "Paste log output here...",
                    color = TextTertiary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                )
            },
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = BorderSubtle,
                focusedBorderColor = ElectricTeal,
                unfocusedContainerColor = Graphite,
                focusedContainerColor = Graphite,
                cursorColor = ElectricTeal,
            ),
            textStyle = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                color = TextPrimary,
                fontSize = 11.sp,
            ),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { viewModel.parseLog() },
                enabled = logText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricTeal,
                    contentColor = Void,
                    disabledContainerColor = ElectricTeal.copy(alpha = 0.2f),
                ),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    "PARSE",
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontSize = 12.sp,
                )
            }
            OutlinedButton(
                onClick = { viewModel.analyzeWithAi() },
                enabled = !isAnalyzing && logText.isNotBlank(),
                shape = RoundedCornerShape(8.dp),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = PhantomViolet,
                ),
            ) {
                Text(
                    if (isAnalyzing) "ANALYZING..." else "AI ANALYSIS",
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontSize = 12.sp,
                )
            }
            TextButton(onClick = { viewModel.clear() }) {
                Text(
                    "CLEAR",
                    color = TextTertiary,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                )
            }
        }

        // Parsed events summary
        if (parsedEvents.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "${parsedEvents.size} EVENTS PARSED",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = ElectricTeal,
            )
            Spacer(modifier = Modifier.height(4.dp))
            parsedEvents.groupBy { it.eventType }.forEach { (type, events) ->
                Text(
                    "${type.name}: ${events.size}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary,
                )
            }
        }

        // AI analysis result
        aiAnalysis?.let { analysis ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Graphite),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "AI ANALYSIS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = PhantomViolet,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        analysis,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 20.sp,
                    )
                }
            }
        }
    }
}
