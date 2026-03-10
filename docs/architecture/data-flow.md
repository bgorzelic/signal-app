# SIGNAL — Data Flow Architecture

## Overview

SIGNAL has four primary data flows, each with different latency and reliability requirements.

## 1. Real-Time Syslog Ingestion

```
WLC (UDP datagram)
  → Network (Tailscale / LAN)
  → SyslogReceiver (Ktor UDP socket, Dispatchers.IO)
  → SyslogMessage.parse() — RFC 3164 extraction
  → MutableSharedFlow (buffer: 1024, DROP_OLDEST on overflow)
  → Two concurrent collectors:
      ├── SyslogService → EventPipeline.processSyslogMessage()
      │     → VendorDetector.parse() → CiscoWlcParser (compiled regex)
      │     → NetworkEvent created with current sessionId
      │     → Batch buffer (20 events or 500ms flush timer)
      │     → NetworkEventDao.insertAll() → Room SQLite
      │
      └── SyslogViewModel (via service binding)
            → ArrayDeque (5000 cap, Mutex-guarded)
            → applyFilter() — debounced 300ms
            → StateFlow<List<SyslogMessage>> → Compose UI
```

### Backpressure Model
- SharedFlow uses `tryEmit()` — non-blocking, drops on full buffer
- `droppedCount` counter tracks lost messages
- Practical limit: ~1000 msg/sec sustained before drops occur

### Concurrency Model
- SyslogReceiver runs on `Dispatchers.IO` (supervisor job)
- EventPipeline uses `Mutex` for batch buffer access
- SyslogViewModel uses separate `Mutex` for UI message buffer
- Session ID (`@Volatile`) is read outside mutex — intentional; the slight race is semantically correct (event tagged with session active at parse time)

## 2. WiFi Scanning

```
ScannerViewModel.init()
  ├── collectScanResults()
  │     → WifiScanner.scanResults() — callbackFlow wrapping BroadcastReceiver
  │     → WifiManager.SCAN_RESULTS_AVAILABLE_ACTION
  │     → ScanResult → WifiScanResult (adds channel, band, security)
  │     → Sorted by RSSI desc → StateFlow → Compose UI
  │
  └── pollConnectionInfo() — every 2 seconds
        → WifiManager.connectionInfo (deprecated but functional)
        → RSSI ring buffer (60 samples, ~2 min window)
        → StateFlow → SignalChart Canvas composable
```

### Notes
- `triggerScan()` is throttled by Android (~4 scans per 2 minutes for apps)
- `connectionInfo` uses deprecated API — still works on Android 16
- Ring buffer is single-writer (sequential in one coroutine), no sync needed

## 3. Log Import (Manual)

```
User pastes text → LogImportScreen
  → LogImportViewModel.parseLog()
  → EventPipeline.processLogBlock(text)
  → text.lines().mapNotNull { VendorDetector.parse(line, sessionId) }
  → dao.insertAll(events) — single batch, no buffering
  → Optional: analyzeWithAi() → OpenClawClient.analyzeLogBlock()
```

### Memory Consideration
- Entire paste is held in memory as String
- Parsed events list is held in memory until inserted
- For very large pastes (>10K lines), this could cause GC pressure
- Practical limit: ~50K lines before noticeable lag

## 4. AI Triage (On-Demand)

```
User taps event → TriageBottomSheet
  → OpenClawClient.triageEvent(event)
  → buildTriagePrompt() — formats event fields + raw syslog
  → OkHttp POST to http://127.0.0.1:18789/api/v1/chat (IPC to Termux)
  → OpenClaw Gateway → OpenRouter → Claude 3.5 Haiku
  → Response parsed: choices[0].message.content
  → Displayed in ModalBottomSheet
```

### Error Handling
- 5s connect timeout, 30s read timeout
- IOException → "Disconnected" status
- HTTP errors → "OpenClaw error: HTTP {code}"
- CancellationException always rethrown (structured concurrency)

## Data Lifecycle

```
Event created (parse)
  → Batch buffer (EventPipeline, in-memory)
  → Room SQLite (persistent)
  → Queried by Timeline/Session views (Flow-based reactive)
  → Cleaned up by DataRetentionManager (default: 30 days)
```

### Retention Policy
- `DataRetentionManager.cleanup()` runs on every app start
- Deletes events with `timestamp < (now - retentionDays * 24h)`
- Configurable via `SignalPreferences.retentionDays` (default 30, 0 = disabled)
- At 100 events/min: ~43MB/day, ~1.3GB/month without cleanup

## Room Database Schema

```sql
CREATE TABLE network_events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    timestamp INTEGER NOT NULL,
    eventType TEXT NOT NULL,
    clientMac TEXT,
    apName TEXT,
    bssid TEXT,
    channel INTEGER,
    rssi INTEGER,
    reasonCode INTEGER,
    vendor TEXT NOT NULL DEFAULT 'GENERIC',
    rawMessage TEXT NOT NULL,
    sessionId TEXT NOT NULL
);

CREATE INDEX idx_sessionId ON network_events(sessionId);
CREATE INDEX idx_sessionId_eventType ON network_events(sessionId, eventType);
CREATE INDEX idx_sessionId_clientMac ON network_events(sessionId, clientMac);
```

### Migration Strategy
- Schema version tracked in `@Database(version = N)`
- Schema JSON exported to `app/schemas/` for version comparison
- Migrations registered in `SignalDatabase.ALL_MIGRATIONS`
- `fallbackToDestructiveMigration()` retained as safety net during v0.x
- Must be removed before v1.0 — all migrations must be explicit
