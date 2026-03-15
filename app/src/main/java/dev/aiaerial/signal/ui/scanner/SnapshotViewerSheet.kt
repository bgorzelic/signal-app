package dev.aiaerial.signal.ui.scanner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aiaerial.signal.data.local.ScanSnapshot
import dev.aiaerial.signal.data.wifi.ScanSnapshotSerializer
import dev.aiaerial.signal.data.wifi.WifiScanResult
import dev.aiaerial.signal.ui.theme.AlertRed
import dev.aiaerial.signal.ui.theme.BorderFocus
import dev.aiaerial.signal.ui.theme.BorderSubtle
import dev.aiaerial.signal.ui.theme.Charcoal
import dev.aiaerial.signal.ui.theme.ElectricTeal
import dev.aiaerial.signal.ui.theme.Graphite
import dev.aiaerial.signal.ui.theme.SignalGreen
import dev.aiaerial.signal.ui.theme.SignalTheme
import dev.aiaerial.signal.ui.theme.Slate
import dev.aiaerial.signal.ui.theme.TextSecondary
import dev.aiaerial.signal.ui.theme.TextTertiary
import dev.aiaerial.signal.ui.theme.Void
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
private val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.US)

// ---- Comparison diff types ------------------------------------------------

private enum class DiffType { ADDED, REMOVED, CHANGED }

private data class NetworkDiff(
    val result: WifiScanResult,
    val type: DiffType,
    val rssiDelta: Int = 0, // only meaningful for CHANGED
)

private fun computeDiff(
    snapshotA: ScanSnapshot,
    snapshotB: ScanSnapshot,
): List<NetworkDiff> {
    val networksA = ScanSnapshotSerializer.deserialize(snapshotA.dataJson).associateBy { it.bssid }
    val networksB = ScanSnapshotSerializer.deserialize(snapshotB.dataJson).associateBy { it.bssid }

    val diffs = mutableListOf<NetworkDiff>()

    // Networks in A but not B — removed
    for ((bssid, result) in networksA) {
        if (bssid !in networksB) {
            diffs.add(NetworkDiff(result, DiffType.REMOVED))
        }
    }

    // Networks in B — either added or changed
    for ((bssid, resultB) in networksB) {
        val resultA = networksA[bssid]
        when {
            resultA == null -> diffs.add(NetworkDiff(resultB, DiffType.ADDED))
            resultA.rssi != resultB.rssi -> diffs.add(
                NetworkDiff(resultB, DiffType.CHANGED, rssiDelta = resultB.rssi - resultA.rssi)
            )
        }
    }

    // Sort: REMOVED first, then ADDED, then CHANGED by absolute delta descending
    return diffs.sortedWith(
        compareBy<NetworkDiff> { it.type.ordinal }
            .thenByDescending { kotlin.math.abs(it.rssiDelta) }
    )
}

// ---- Main sheet composable ------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnapshotViewerSheet(
    snapshots: List<ScanSnapshot>,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // UI state
    var expandedSnapshotId by remember { mutableStateOf<Long?>(null) }
    var compareMode by remember { mutableStateOf(false) }
    var selectionA by remember { mutableStateOf<ScanSnapshot?>(null) }
    var selectionB by remember { mutableStateOf<ScanSnapshot?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Graphite,
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 36.dp, height = 3.dp)
                    .background(BorderFocus, CircleShape),
            )
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "SCAN HISTORY",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = TextTertiary,
                )
                if (snapshots.size >= 2) {
                    TextButton(
                        onClick = {
                            compareMode = !compareMode
                            selectionA = null
                            selectionB = null
                        },
                    ) {
                        Text(
                            text = if (compareMode) "CANCEL" else "COMPARE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = if (compareMode) AlertRed else ElectricTeal,
                        )
                    }
                }
            }

            HorizontalDivider(
                color = BorderSubtle,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (snapshots.isEmpty()) {
                EmptyHistoryPlaceholder()
            } else if (compareMode) {
                // Show diff results if both selected, otherwise show selection list
                val a = selectionA
                val b = selectionB
                if (a != null && b != null) {
                    ComparisonView(
                        snapshotA = a,
                        snapshotB = b,
                        onClearSelection = {
                            selectionA = null
                            selectionB = null
                        },
                    )
                } else {
                    CompareSelectionInstructions(
                        selectionA = selectionA,
                        selectionB = selectionB,
                    )
                    LazyColumn {
                        items(items = snapshots, key = { it.id }) { snapshot ->
                            val isSelected = snapshot == selectionA || snapshot == selectionB
                            SnapshotRowSelectable(
                                snapshot = snapshot,
                                isSelected = isSelected,
                                onSelect = {
                                    when {
                                        isSelected -> {
                                            if (snapshot == selectionA) selectionA = null
                                            else selectionB = null
                                        }
                                        selectionA == null -> selectionA = snapshot
                                        selectionB == null -> selectionB = snapshot
                                        // Both filled — replace the older selection
                                        else -> selectionA = snapshot
                                    }
                                },
                            )
                            HorizontalDivider(
                                color = BorderSubtle,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                        item { Spacer(modifier = Modifier.height(32.dp)) }
                    }
                }
            } else {
                // Normal list mode
                LazyColumn {
                    items(items = snapshots, key = { it.id }) { snapshot ->
                        val isExpanded = expandedSnapshotId == snapshot.id
                        SnapshotRow(
                            snapshot = snapshot,
                            isExpanded = isExpanded,
                            onToggleExpand = {
                                expandedSnapshotId = if (isExpanded) null else snapshot.id
                            },
                            onDelete = { onDelete(snapshot.id) },
                        )
                        HorizontalDivider(
                            color = BorderSubtle,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }
    }
}

// ---- Empty state -----------------------------------------------------------

@Composable
private fun EmptyHistoryPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "No snapshots saved.\nTap SAVE SNAPSHOT to capture the current scan.",
            fontSize = 12.sp,
            color = TextTertiary,
            lineHeight = 18.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

// ---- Normal snapshot row ---------------------------------------------------

@Composable
private fun SnapshotRow(
    snapshot: ScanSnapshot,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = SignalTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = snapshot.label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = timeFormat.format(Date(snapshot.timestamp)),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TextSecondary,
                    )
                    Text(
                        text = "${snapshot.networkCount} nets",
                        fontSize = 11.sp,
                        color = TextTertiary,
                    )
                    snapshot.ssid?.let { ssid ->
                        val rssiStr = snapshot.rssi?.let { " ${it} dBm" } ?: ""
                        val rssiColor = snapshot.rssi?.let { rssi ->
                            when {
                                rssi >= -50 -> colors.signalExcellent
                                rssi >= -60 -> colors.signalGood
                                rssi >= -70 -> colors.signalFair
                                else -> colors.signalPoor
                            }
                        } ?: TextTertiary
                        Text(
                            text = "$ssid$rssiStr",
                            fontSize = 11.sp,
                            color = rssiColor,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Expand indicator
                Text(
                    text = if (isExpanded) "▲" else "▼",
                    fontSize = 10.sp,
                    color = TextTertiary,
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete snapshot",
                        tint = TextTertiary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        // Expanded detail card
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            SnapshotDetailExpanded(snapshot = snapshot)
        }
    }
}

// ---- Expanded detail (inline card) ----------------------------------------

@Composable
private fun SnapshotDetailExpanded(snapshot: ScanSnapshot) {
    val colors = SignalTheme.colors
    val networks = remember(snapshot.id) {
        ScanSnapshotSerializer.deserialize(snapshot.dataJson)
            .sortedByDescending { it.rssi }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .background(Charcoal, RoundedCornerShape(8.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = dateFormat.format(Date(snapshot.timestamp)),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = TextTertiary,
            )
            snapshot.bssid?.let { bssid ->
                Text(
                    text = bssid,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = TextTertiary,
                )
            }
        }

        HorizontalDivider(color = BorderSubtle)

        networks.forEach { net ->
            val signalColor = when {
                net.rssi >= -50 -> colors.signalExcellent
                net.rssi >= -60 -> colors.signalGood
                net.rssi >= -70 -> colors.signalFair
                else -> colors.signalPoor
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = net.ssid.ifEmpty { "(Hidden)" },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = net.bssid,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = TextTertiary,
                    modifier = Modifier.weight(1.2f),
                )
                Text(
                    text = "${net.rssi} dBm",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = signalColor,
                )
            }
        }
    }
}

// ---- Compare mode: selection row ------------------------------------------

@Composable
private fun CompareSelectionInstructions(
    selectionA: ScanSnapshot?,
    selectionB: ScanSnapshot?,
) {
    val selectedCount = listOfNotNull(selectionA, selectionB).size
    val message = when (selectedCount) {
        0 -> "Select snapshot A"
        1 -> "Select snapshot B"
        else -> "Ready to compare"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SelectionBadge(label = "A", snapshot = selectionA)
        Text(text = "vs", fontSize = 10.sp, color = TextTertiary)
        SelectionBadge(label = "B", snapshot = selectionB)
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = message,
            fontSize = 10.sp,
            color = TextTertiary,
            letterSpacing = 0.5.sp,
        )
    }
    HorizontalDivider(
        color = BorderSubtle,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

@Composable
private fun SelectionBadge(label: String, snapshot: ScanSnapshot?) {
    val filled = snapshot != null
    Box(
        modifier = Modifier
            .background(
                if (filled) ElectricTeal.copy(alpha = 0.15f) else Void,
                RoundedCornerShape(4.dp),
            )
            .border(
                1.dp,
                if (filled) ElectricTeal else BorderSubtle,
                RoundedCornerShape(4.dp),
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (filled) "$label: ${timeFormat.format(Date(snapshot!!.timestamp))}" else label,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (filled) ElectricTeal else TextTertiary,
        )
    }
}

@Composable
private fun SnapshotRowSelectable(
    snapshot: ScanSnapshot,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .background(if (isSelected) ElectricTeal.copy(alpha = 0.06f) else Void.copy(alpha = 0f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = snapshot.label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) ElectricTeal else MaterialTheme.colorScheme.onSurface,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = timeFormat.format(Date(snapshot.timestamp)),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = TextSecondary,
                )
                Text(
                    text = "${snapshot.networkCount} nets",
                    fontSize = 11.sp,
                    color = TextTertiary,
                )
            }
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .background(ElectricTeal, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "✓", fontSize = 10.sp, color = Void, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ---- Comparison result view -----------------------------------------------

@Composable
private fun ComparisonView(
    snapshotA: ScanSnapshot,
    snapshotB: ScanSnapshot,
    onClearSelection: () -> Unit,
) {
    val diffs = remember(snapshotA.id, snapshotB.id) {
        computeDiff(snapshotA, snapshotB)
    }

    val added = diffs.count { it.type == DiffType.ADDED }
    val removed = diffs.count { it.type == DiffType.REMOVED }
    val changed = diffs.count { it.type == DiffType.CHANGED }

    LazyColumn {
        // Summary header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(Charcoal, RoundedCornerShape(8.dp))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                    .padding(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "DIFF SUMMARY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = TextTertiary,
                    )
                    TextButton(onClick = onClearSelection) {
                        Text(
                            text = "CHANGE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = ElectricTeal,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "A:",
                        fontSize = 10.sp,
                        color = TextTertiary,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        text = snapshotA.label,
                        fontSize = 10.sp,
                        color = TextSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = timeFormat.format(Date(snapshotA.timestamp)),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = TextTertiary,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "B:",
                        fontSize = 10.sp,
                        color = TextTertiary,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        text = snapshotB.label,
                        fontSize = 10.sp,
                        color = TextSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = timeFormat.format(Date(snapshotB.timestamp)),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = TextTertiary,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DiffCountBadge(count = added, label = "ADDED", color = SignalGreen)
                    DiffCountBadge(count = removed, label = "REMOVED", color = AlertRed)
                    DiffCountBadge(count = changed, label = "CHANGED", color = ElectricTeal)
                }
            }
        }

        // Diff rows
        if (diffs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No differences detected.\nBoth snapshots are identical.",
                        fontSize = 12.sp,
                        color = TextTertiary,
                        lineHeight = 18.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        } else {
            items(items = diffs, key = { "${it.result.bssid}-${it.type}" }) { diff ->
                DiffRow(diff = diff)
                HorizontalDivider(
                    color = BorderSubtle,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun DiffCountBadge(count: Int, label: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$count",
            fontFamily = FontFamily.Monospace,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Text(
            text = label,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = color.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun DiffRow(diff: NetworkDiff) {
    val (accentColor, typeLabel) = when (diff.type) {
        DiffType.ADDED -> SignalGreen to "+"
        DiffType.REMOVED -> AlertRed to "-"
        DiffType.CHANGED -> ElectricTeal to "~"
    }
    val colors = SignalTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Type indicator pill
        Box(
            modifier = Modifier
                .size(width = 22.dp, height = 22.dp)
                .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = typeLabel,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
            )
        }

        // SSID + BSSID
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = diff.result.ssid.ifEmpty { "(Hidden)" },
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (diff.type == DiffType.REMOVED)
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                else
                    MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = diff.result.bssid,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = TextTertiary,
            )
        }

        // RSSI / delta column
        Column(horizontalAlignment = Alignment.End) {
            when (diff.type) {
                DiffType.ADDED, DiffType.REMOVED -> {
                    val signalColor = when {
                        diff.result.rssi >= -50 -> colors.signalExcellent
                        diff.result.rssi >= -60 -> colors.signalGood
                        diff.result.rssi >= -70 -> colors.signalFair
                        else -> colors.signalPoor
                    }
                    Text(
                        text = "${diff.result.rssi} dBm",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (diff.type == DiffType.REMOVED)
                            signalColor.copy(alpha = 0.4f)
                        else
                            signalColor,
                    )
                }
                DiffType.CHANGED -> {
                    val deltaSign = if (diff.rssiDelta >= 0) "+" else ""
                    val deltaColor = when {
                        diff.rssiDelta > 0 -> SignalGreen
                        diff.rssiDelta < 0 -> AlertRed
                        else -> TextTertiary
                    }
                    Text(
                        text = "${diff.result.rssi} dBm",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TextSecondary,
                    )
                    Text(
                        text = "$deltaSign${diff.rssiDelta} dBm",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = deltaColor,
                    )
                }
            }
            // Band label
            Text(
                text = "${diff.result.band} Ch${diff.result.channel}",
                fontSize = 10.sp,
                color = TextTertiary,
            )
        }
    }
}
