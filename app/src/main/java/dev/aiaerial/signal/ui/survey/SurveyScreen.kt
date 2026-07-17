package dev.aiaerial.signal.ui.survey

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aiaerial.signal.ui.theme.TextSecondary
import dev.aiaerial.signal.ui.theme.Void

@Composable fun SurveyScreen() {
    var started by remember { mutableStateOf(false) }
    val green = Color(0xFF65F23B)
    Column(Modifier.fillMaxSize().background(Void).padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (started) "CLIENT WALK ACTIVE" else "CLIENT WALK TEST", color = green, fontSize = 12.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
        Text(if (started) "Measure each spot" else "Validate the experience as you move.", color = Color.White, fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
        Text(if (started) "Move to the next location, then capture another point." else "Phone-only mode records client signal and active performance. It is not a calibrated spectrum survey.", color = TextSecondary, fontSize = 15.sp, lineHeight = 21.sp, modifier = Modifier.padding(top = 12.dp))
        Spacer(Modifier.height(24.dp))
        Button(onClick = { started = !started }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = green, contentColor = Color.Black)) {
            Text(if (started) "CAPTURE THIS SPOT" else "START WALK TEST", fontWeight = FontWeight.Bold)
        }
    }
}
