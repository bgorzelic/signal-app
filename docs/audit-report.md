# SIGNAL Engineering Audit Report

**Date:** 2026-03-09 (updated)
**Auditor:** Claude (Principal Android Systems Engineer)
**Scope:** Full codebase review — 42 Kotlin source files, 7 test files, 4 doc files

## Executive Summary

The codebase is a well-architected MVP with solid fundamentals: clean MVVM separation, proper Hilt DI, Room with appropriate indices, strategy pattern for parser extensibility, and correct foreground service declaration. The previous session addressed several critical items (CancellationException handling, notification permission, port wiring, SharedFlow buffer sizing). This audit identifies remaining issues for field deployment hardening.

**Overall Assessment:** Ready for controlled field testing. A handful of concurrency and lifecycle issues should be fixed before extended deployment.

---

## Critical Issues

### C1: SyslogViewModel.allMessages is not thread-safe

**File:** `ui/syslog/SyslogViewModel.kt:46`
**Impact:** Potential ConcurrentModificationException or data corruption
**Status:** OPEN

```kotlin
private val allMessages = ArrayDeque<SyslogMessage>(MAX_MESSAGES + 1)
```

`allMessages` is a plain `java.util.ArrayDeque` (not synchronized). It's written to from the SharedFlow collector (which runs on a coroutine, potentially on `Dispatchers.Default` or `IO` via the service binding) and read from `clearMessages()` (called from UI thread) and `applyFilter()` (called from both).

The `addFirst()` → size check → `removeLast()` → `applyFilter()` sequence is not atomic. If `clearMessages()` is called from the UI while a message is being added, `ArrayDeque` can throw `ConcurrentModificationException` or silently corrupt its internal array.

**Fix options:**
1. **Mutex** — wrap all operations in `batchMutex.withLock { }` (consistent with EventPipeline's approach)
2. **Single-writer pattern** — route all modifications through a `Channel<SyslogAction>` processed by a single coroutine
3. **CopyOnWriteArrayList** — simplest but O(n) on every write

**Recommended:** Option 1 (Mutex). Consistent with existing patterns and handles this correctly.

### C2: SyslogService.onDestroy() uses runBlocking

**File:** `service/SyslogService.kt:97`
**Impact:** ANR risk — blocks the main thread during flush

```kotlin
override fun onDestroy() {
    receiver?.stop()
    kotlinx.coroutines.runBlocking { eventPipeline.flush() }
    scope.cancel()
    super.onDestroy()
}
```

`onDestroy()` runs on the main thread. `runBlocking` blocks it until `eventPipeline.flush()` completes, which does a Room `insertAll()` (disk I/O). If the database is busy or the batch is large, this can trigger an ANR (Application Not Responding — 5-second timeout).

**Fix:** Launch the flush in the service's scope before cancelling it:
```kotlin
override fun onDestroy() {
    receiver?.stop()
    scope.launch { eventPipeline.flush() }
    // Give the flush a moment, then cancel
    scope.cancel()
    super.onDestroy()
}
```
Or use `runBlocking(Dispatchers.IO)` to at least move the blocking off the main thread's default dispatcher.

### C3: SyslogService manual CoroutineScope not tied to service lifecycle

**File:** `service/SyslogService.kt:32`
**Impact:** Low risk for foreground services, medium risk if service type changes

```kotlin
private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
```

If Android force-kills the process (as opposed to stopping the service), `onDestroy()` is not guaranteed to be called, and the scope won't be cancelled. For a foreground service this is unlikely (they get higher OOM priority), but it's worth noting.

**Recommendation:** Acceptable for now. Consider migrating to `LifecycleService` with `lifecycleScope` in a future refactor.

---

## Important Issues

### I1: WiFi scanning uses deprecated APIs

**File:** `data/wifi/WifiScanner.kt:41-42, 47-48`
**Impact:** APIs may be removed in future Android versions
**Status:** OPEN

`WifiManager.startScan()` is deprecated since API 28 (still functional through API 36).
`WifiManager.connectionInfo` is deprecated since API 31.

Replacements:
- Scan results: `WifiManager.registerScanResultsCallback()` (API 30+)
- Connection info: `ConnectivityManager.registerNetworkCallback()` with `NetworkRequest.Builder().addTransportType(TRANSPORT_WIFI)`

**Risk:** Low — deprecated doesn't mean removed. These APIs work on Android 16. But they could be removed in Android 17+.

**Recommendation:** Keep for now. Plan migration for v0.3 when targeting the next Android version.

### I2: No data retention policy

**Impact:** Storage grows indefinitely during long deployments

Room database grows at ~300 bytes per event. At 100 events/minute:
- 1 hour: ~1.8 MB
- 1 day: ~43 MB
- 1 week: ~300 MB

The `SyslogViewModel` also keeps 5000 messages in memory (~1MB).

**Recommendation:** Add a background job that purges sessions older than N days, or add a manual "Delete Session" button on a future session picker screen.

### I3: EventPipeline.flushJob race condition

**File:** `data/EventPipeline.kt:47-48`
**Impact:** Potential double-flush or missed timer

```kotlin
if (flushJob == null && scope != null) {
    flushJob = scope.launch {
        delay(FLUSH_INTERVAL_MS)
        flush()
    }
}
```

The `flushJob` null check and assignment are inside `batchMutex.withLock`, but `flush()` itself acquires the same mutex. When the delay fires and `flush()` is called, it will try to acquire `batchMutex` — this works because `Mutex` is not reentrant, but `flush()` uses `withLock` which will properly wait.

However, there's a subtle issue: if `flush()` is called externally (e.g., from `onDestroy`) while the timer is also firing, both will try to acquire the mutex. The mutex correctly serializes them, but the second flush will find an empty list and no-op. This is fine behavior.

**Status:** Actually safe as written. The mutex provides correct serialization. No fix needed.

### I4: TriageBottomSheet catches Exception without CancellationException check

**File:** `ui/triage/TriageBottomSheet.kt:25-29`
**Impact:** Swallows CancellationException if the composable leaves composition during the API call

```kotlin
analysis = try {
    openClawClient.triageEvent(event)
} catch (e: Exception) {
    "Error: ${e.message}\n\nIs OpenClaw running on localhost:18789?"
}
```

This runs inside `LaunchedEffect`, which uses `rememberCoroutineScope` internally. If the bottom sheet is dismissed while the API call is in flight, the coroutine is cancelled. The catch block will catch `CancellationException` and display it as an error instead of letting it propagate.

**Fix:** Add the CancellationException check:
```kotlin
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    "Error: ${e.message}\n\nIs OpenClaw running on localhost:18789?"
}
```

### I5: SyslogScreen uses collectAsState instead of collectAsStateWithLifecycle

**File:** `ui/syslog/SyslogScreen.kt:32-34`
**Impact:** Minor — continues collecting flows when app is backgrounded

```kotlin
val messages by viewModel.messages.collectAsState()
val isRunning by viewModel.isRunning.collectAsState()
```

`collectAsState()` continues collecting even when the app is in the background. For the syslog screen, this means the UI state keeps updating even though nobody can see it. The ScannerScreen correctly uses `collectAsStateWithLifecycle()`.

**Risk:** Low — the syslog service needs to keep running regardless. But the UI state updates waste CPU cycles when backgrounded.

### I6: SimpleDateFormat created per-composition in SyslogMessageRow

**File:** `ui/syslog/SyslogScreen.kt:111`

```kotlin
val timeFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }
```

This is correctly wrapped in `remember` so it survives recomposition. However, `SimpleDateFormat` is not thread-safe. If Compose recomposes this composable from multiple threads (unlikely but possible in future Compose versions), it could produce garbled output.

**Risk:** Very low with current Compose. Just worth noting for future-proofing.

---

## Minor Issues

### M1: Material Icons Extended pulls in entire icon set

**File:** `app/build.gradle.kts:58`

```kotlin
implementation(libs.androidx.compose.material.icons.extended)
```

The app uses only 5 icons (Wifi, Message, Timeline, Settings, ArrowBack). The extended library includes thousands, adding ~5MB to the APK. R8 would strip unused icons, but R8 is disabled.

**Fix:** Enable R8 (requires keep rules for Hilt, Room, kotlinx.serialization, Ktor), or switch to individual icon downloads.

### M2: tools:targetApi="31" in manifest

**File:** `AndroidManifest.xml:30`

This is cosmetic — it suppresses lint warnings about APIs above level 31. Should match `targetSdk = 36` for accuracy.

### M3: No proguard-rules.pro content

**File:** `app/proguard-rules.pro`

When R8 is eventually enabled, keep rules will be needed for:
- Hilt (`@HiltAndroidApp`, `@AndroidEntryPoint`)
- Room (`@Entity`, `@Dao`)
- kotlinx.serialization (`@Serializable`)
- Ktor network classes
- OkHttp (already provides its own consumer rules)

### M4: OpenClawClient.chat() returns error strings instead of throwing

**File:** `data/openclaw/OpenClawClient.kt:73-74`

```kotlin
if (!response.isSuccessful) {
    return "OpenClaw error: HTTP ${response.code}"
}
```

HTTP errors return a string rather than throwing an exception. This means callers can't distinguish between a successful analysis that says "Error..." and an actual HTTP error. For the current UI (just displaying text), this works, but it makes the API harder to use correctly.

**Recommendation:** Consider a sealed class result type (`Result<String>` or custom `AnalysisResult`) for v0.2.

### M5: WifiScanResult.channelWidth stores the raw Android enum value

**File:** `data/wifi/WifiScanResult.kt:8`

The `channelWidth` field stores Android's `ScanResult.CHANNEL_WIDTH_*` enum as an integer (0-5). This is fragile if Android adds new width values. The `channelWidthMhz` getter handles this with an "else -> ?" fallback, which is fine.

### M6: ScannerScreen requests permissions on every composition

**File:** `ui/scanner/ScannerScreen.kt:58-60`

```kotlin
LaunchedEffect(Unit) {
    permissionLauncher.launch(permissions)
}
```

`LaunchedEffect(Unit)` runs once per composition entry. Since the Scanner is the start destination, this runs every time the app starts. After the first grant, subsequent launches show a brief permission dialog flash (Android skips already-granted permissions quickly, but it's noticeable).

**Fix:** Check `ContextCompat.checkSelfPermission()` first and only launch if not granted.

---

## Performance Analysis

### Good Practices Found

| Practice | Location | Why It Matters |
|---|---|---|
| Pre-compiled regex patterns | `CiscoWlcParser` (class-level) | Each syslog line goes through ~6 regex checks. Compiling per-call would be ~100x slower. |
| Batch inserts | `EventPipeline` (20 events / 500ms) | Reduces SQLite transaction overhead by ~20x |
| Composite Room indices | `NetworkEvent` entity | Covers all DAO query patterns — no full table scans |
| SharedFlow buffer (1024) | `SyslogReceiver` | Handles burst traffic without blocking the receive loop |
| RSSI ring buffer (60 points) | `ScannerViewModel` | Fixed memory, no unbounded growth |
| `items(key = {...})` in LazyColumn | All list screens | Compose skips recomposing unchanged items |
| `WhileSubscribed(5000)` | Timeline/Syslog ViewModels | Cancels upstream queries when UI is not visible, with grace period for rotation |

### Potential Bottlenecks

| Concern | Risk Level | Trigger Condition | Mitigation |
|---|---|---|---|
| 5000-message in-memory list | Low | Extended capture session | Already capped — monitor memory via `adb shell dumpsys meminfo` |
| Room insert throughput | Low | >100 events/second sustained | Batch size of 20 handles this. Consider increasing to 50 for extreme loads. |
| WiFi poll (2s interval) | Low | Always running | Switch to 5-10s when screen is off |
| SharedFlow drops | Low | >1024 msg/sec burst | `droppedCount` counter tracks drops. Increase buffer if needed. |
| Room query for Timeline | Medium | Session with >10,000 events | No pagination — consider LIMIT/OFFSET for large datasets |

### Battery Impact Assessment

| Component | Drain Source | Impact | Optimization |
|---|---|---|---|
| Foreground service | Process kept alive | Medium | Required — can't avoid |
| WiFi polling (2s) | Binder IPC to WiFi service | Medium | Reduce to 5-10s for passive mode |
| UDP receive loop | Socket in blocking receive | Low | Blocks on I/O, minimal CPU |
| Room writes | Disk I/O | Low | Batched, already efficient |
| OpenClaw calls | HTTP + AI inference | Low | Only on user action, not periodic |
| Screen rendering | GPU + display | High | Dim screen, use dark mode |

---

## Security Assessment

### Strengths

| Practice | Assessment |
|---|---|
| No secrets in source code | Good — no API keys, tokens, or credentials |
| OpenClaw is localhost-only | Good — AI traffic never leaves the device (from SIGNAL's perspective) |
| No external network calls from SIGNAL | Good — all AI goes through local OpenClaw proxy |
| CSV export has field escaping | Good — prevents CSV injection attacks |
| Service not exported | Good — `android:exported="false"` prevents other apps from binding |
| CancellationException handled correctly | Good — structured concurrency maintained |
| OkHttp responses properly closed | Good — `response.use { }` prevents connection leaks |

### Acceptable Risks (Field Tool Context)

| Risk | Assessment | Mitigation |
|---|---|---|
| UDP port 1514 accepts from any source | Inherent to syslog (RFC 3164 has no auth) | Acceptable for a field tool on a private network |
| SharedPreferences stores OpenClaw URL | Always localhost — not sensitive | OK |
| No TLS on syslog ingestion | Standard syslog is plaintext UDP | Use Tailscale (encrypted tunnel) for transport security |
| No input validation on syslog messages | Malformed messages produce null parse results | Parser safely returns null for unrecognized input |

---

## Concurrency Correctness Review

### Correct Patterns

1. **EventPipeline.batchMutex** — Properly serializes batch operations and flush timer
2. **@Volatile on EventPipeline.currentSessionId** — Single-writer (newSession), multi-reader pattern
3. **@Volatile on SyslogReceiver.droppedCount** — Single-writer (receive loop), multi-reader
4. **@Volatile on OpenClawClient.baseUrl** — Single-writer (setBaseUrl), multi-reader
5. **SupervisorJob in SyslogService** — Prevents pipeline failure from killing receive loop
6. **viewModelScope** — Automatically cancelled on ViewModel clear, preventing leaks

### Issues

1. **SyslogViewModel.allMessages** — Not thread-safe (see C1 above)
2. **SyslogService.onDestroy runBlocking** — Blocks main thread (see C2 above)

---

## Recommendations Summary

### Already Fixed (Previous Session)
| Issue | Status |
|---|---|
| CancellationException in SyslogService pipeline collector | FIXED |
| POST_NOTIFICATIONS runtime permission request | FIXED |
| Syslog port wired from preferences to receiver | FIXED |
| SharedFlow buffer increased to 1024 with tryEmit | FIXED |
| SyslogReceiver idempotency (checks job?.isActive) | FIXED |
| Database fallbackToDestructiveMigration added | FIXED |
| Version corrected to 0.1.0 | FIXED |

### Open Items

| Priority | Issue | Effort | Risk if Ignored |
|---|---|---|---|
| **High** | C1: SyslogViewModel thread safety | 30 min | ConcurrentModificationException crash during use |
| **High** | C2: runBlocking in onDestroy | 15 min | ANR when stopping syslog service |
| **Medium** | I4: TriageBottomSheet CancellationException | 5 min | Error message shown when dismissing during load |
| **Medium** | I2: Data retention policy | 1 hour | Storage exhaustion on week-long deployments |
| **Medium** | M6: Permission check before launch | 15 min | Brief dialog flash on every app start |
| **Low** | I1: Migrate deprecated WiFi APIs | 2 hours | Future Android version may remove them |
| **Low** | I5: collectAsStateWithLifecycle in SyslogScreen | 5 min | Wasted CPU when backgrounded |
| **Low** | M1: Enable R8 or remove icons-extended | 1 hour | 5MB larger APK than necessary |
| **Low** | M4: Sealed class for OpenClaw results | 30 min | Better API design for future callers |

---

## Recommended Fix Order

1. **C1 + C2** — Thread safety and ANR fix (45 min). These are the only issues that can cause crashes.
2. **I4** — CancellationException in triage (5 min). One-line fix.
3. **M6** — Permission check (15 min). Improves first-launch experience.
4. **I2** — Data retention (1 hour). Important for real field deployments.
5. Everything else is low priority and can be addressed in v0.2/v0.3.

---

## Future Architecture Considerations

### For v0.2
- **Session management:** Add a session picker screen. Room already stores sessionId — need a UI to browse and compare sessions.
- **Export integration:** Wire `SessionExporter` to Android's Share sheet. Add a FAB or menu item on Timeline/Syslog screens.
- **Aruba parser:** Copy the CiscoWlcParser pattern. ArubaOS-CX uses different keywords but similar syslog structure.

### For v0.3
- **Repository pattern:** If adding cloud sync or shared storage, introduce a Repository layer between ViewModels and data sources.
- **WorkManager for background export:** Schedule periodic CSV exports to Google Drive or a shared location.
- **DataStore migration:** If preferences grow beyond 5-6 keys, migrate from SharedPreferences to Preferences DataStore.

### For v1.0
- **Room schema migrations:** Before distributing to other users, set `exportSchema = true` and write proper `Migration` objects for every version increment.
- **Instrumented tests:** Add Compose UI tests, Room DAO tests with in-memory databases, and integration tests for the full syslog → Room pipeline.
- **Crash reporting:** Integrate Firebase Crashlytics or Sentry for field error reporting.
