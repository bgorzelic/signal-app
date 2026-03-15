package dev.aiaerial.signal.ui.scanner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import dev.aiaerial.signal.ui.theme.BorderSubtle
import dev.aiaerial.signal.ui.theme.ElectricTeal
import dev.aiaerial.signal.ui.theme.Graphite
import dev.aiaerial.signal.ui.theme.SignalTheme

/**
 * Canvas-based RSSI line chart with Ghost Console styling.
 * Y-axis range: -90 to -30 dBm.
 * Threshold lines at -50, -67, -80 dBm.
 */
@Composable
fun SignalChart(
    dataPoints: List<Pair<Long, Int>>,
    smoothedPoints: List<Pair<Long, Int>> = emptyList(),
    modifier: Modifier = Modifier,
) {
    val colors = SignalTheme.colors
    val lineColor = ElectricTeal

    val thresholdPaints = remember(colors) {
        listOf(colors.signalExcellent, colors.signalFair, colors.signalPoor).map { color ->
            android.graphics.Paint().apply {
                this.color = color.copy(alpha = 0.7f).toArgb()
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Graphite)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .padding(8.dp),
    ) {
        val minDbm = -90f
        val maxDbm = -30f
        val range = maxDbm - minDbm

        fun dbmToY(dbm: Int): Float =
            size.height - ((dbm - minDbm) / range) * size.height

        // Threshold lines
        val thresholds = listOf(
            -50 to colors.signalExcellent,
            -67 to colors.signalFair,
            -80 to colors.signalPoor,
        )
        val labelTextSize = 10.dp.toPx()
        thresholds.forEachIndexed { index, (dbm, color) ->
            val y = dbmToY(dbm)
            drawLine(
                color = color.copy(alpha = 0.25f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
            )
            drawContext.canvas.nativeCanvas.drawText(
                "${dbm}dBm",
                4.dp.toPx(),
                y - 2.dp.toPx(),
                thresholdPaints[index].apply { textSize = labelTextSize },
            )
        }

        // Raw data line (faded when smoothed line is also shown)
        if (dataPoints.size >= 2) {
            val rawAlpha = if (smoothedPoints.size >= 2) 0.2f else 0.8f
            val path = Path()
            val step = size.width / (dataPoints.size - 1).coerceAtLeast(1)

            dataPoints.forEachIndexed { index, (_, rssi) ->
                val x = index * step
                val y = dbmToY(rssi)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            drawPath(
                path = path,
                color = lineColor.copy(alpha = rawAlpha),
                style = Stroke(width = 1.dp.toPx()),
            )
        }

        // Smoothed trend line (solid, accent-colored, prominent)
        if (smoothedPoints.size >= 2) {
            val path = Path()
            val step = size.width / (smoothedPoints.size - 1).coerceAtLeast(1)

            smoothedPoints.forEachIndexed { index, (_, rssi) ->
                val x = index * step
                val y = dbmToY(rssi)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 2.5f.dp.toPx()),
            )
        }
    }
}
