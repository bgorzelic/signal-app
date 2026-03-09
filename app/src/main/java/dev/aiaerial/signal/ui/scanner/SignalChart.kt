package dev.aiaerial.signal.ui.scanner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp

/**
 * Canvas-based RSSI line chart.
 * Y-axis range: -90 to -30 dBm.
 * Threshold lines at -50, -67, -80 dBm.
 */
@Composable
fun SignalChart(
    dataPoints: List<Pair<Long, Int>>,
    modifier: Modifier = Modifier,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val thresholdGood = Color(0xFF4CAF50)
    val thresholdFair = Color(0xFFFFC107)
    val thresholdPoor = Color(0xFFF44336)

    val thresholdPaints = remember {
        listOf(thresholdGood, thresholdFair, thresholdPoor).map { color ->
            android.graphics.Paint().apply {
                this.color = color.copy(alpha = 0.7f).toArgb()
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
    ) {
        val minDbm = -90f
        val maxDbm = -30f
        val range = maxDbm - minDbm // 60

        fun dbmToY(dbm: Int): Float =
            size.height - ((dbm - minDbm) / range) * size.height

        // Threshold lines
        val thresholds = listOf(
            -50 to thresholdGood,
            -67 to thresholdFair,
            -80 to thresholdPoor,
        )
        val labelTextSize = 10.dp.toPx()
        thresholds.forEachIndexed { index, (dbm, color) ->
            val y = dbmToY(dbm)
            drawLine(
                color = color.copy(alpha = 0.4f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
            )
            // Label
            drawContext.canvas.nativeCanvas.drawText(
                "${dbm}dBm",
                4.dp.toPx(),
                y - 2.dp.toPx(),
                thresholdPaints[index].apply { textSize = labelTextSize },
            )
        }

        // Data line
        if (dataPoints.size >= 2) {
            val path = Path()
            val step = size.width / (dataPoints.size - 1).coerceAtLeast(1)

            dataPoints.forEachIndexed { index, (_, rssi) ->
                val x = index * step
                val y = dbmToY(rssi)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 2.dp.toPx()),
            )
        }
    }
}
