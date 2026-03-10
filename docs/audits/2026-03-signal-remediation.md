# SIGNAL App — Engineering Remediation Report

**Date:** 2026-03-10
**Scope:** Targeted audit and remediation of 6 open items from the initial MVP audit

## Issues Verified

### 1. SyslogViewModel Thread Safety (ArrayDeque)

- **Status:** ALREADY FIXED
- **Evidence:** `SyslogViewModel.kt` line 50 — `messagesMutex = Mutex()`. All ArrayDeque access is guarded:
  - `onServiceConnected` collector: lines 65-68 (`messagesMutex.withLock`)
  - `applyFilter()`: line 112 (`messagesMutex.withLock`)
  - `clearMessages()`: lines 122-124 (`messagesMutex.withLock`)
- **Risk:** None. The Mutex-based approach is correct for coroutine-based concurrency. `ArrayDeque` is accessed only through the Mutex, and UI state is published via `MutableStateFlow` which is thread-safe.
- **Action:** None required. Updated HANDOFF.md to reflect resolved status.

### 2. Room Migration Strategy

- **Status:** CONFIRMED OPEN → FIXED
- **Root cause:** `SignalDatabase` had `exportSchema = false` and `DatabaseModule` used `fallbackToDestructiveMigration()` with no migration scaffolding.
- **Risk:** Any schema change would silently wipe all user data. Unacceptable for field engineers who may have captured hours of survey data.
- **Fix applied:**
  - Enabled `exportSchema = true` in `@Database` annotation
  - Added KSP argument `room.schemaLocation` to `build.gradle.kts`
  - Created `SignalDatabase.ALL_MIGRATIONS` companion object as migration registry
  - Added `addMigrations()` call in `DatabaseModule`
  - Retained `fallbackToDestructiveMigration()` as safety net during v0.x development (documented for removal before v1.0)
  - Room now exports schema JSON to `app/schemas/` (version 1.json committed)
- **Validation:** Build succeeds, `app/schemas/dev.aiaerial.signal.data.local.SignalDatabase/1.json` generated.

### 3. EventPipeline.currentSessionId Race

- **Status:** BENIGN (no fix needed)
- **Analysis:**
  - `currentSessionId` is `@Volatile`, ensuring visibility across threads
  - `newSession()` calls `flush()` before reassigning the ID, which acquires `batchMutex` and drains all pending events from the previous session
  - The only race window: `vendorDetector.parse(msg.raw, currentSessionId)` reads the ID outside the mutex. If `newSession()` runs between this read and the subsequent `batchMutex.withLock`, the event is tagged with the old session ID.
  - This is semantically correct: the event was received during the old session and should be attributed to it.
  - String assignment on JVM is atomic, and `@Volatile` guarantees happens-before ordering.
- **Risk:** None. The behavior is correct under all interleavings.
- **Action:** Documented the safety reasoning. Added 8 unit tests for EventPipeline to validate session handling.

### 4. Deprecated WiFi APIs

- **Status:** CONFIRMED, DEFERRED
- **Evidence:** `WifiScanner.kt` uses two deprecated APIs:
  - `wifiManager.startScan()` (deprecated API 28+) — line 41
  - `wifiManager.connectionInfo` (deprecated API 31+) — line 48
- **Risk:** Low. Both APIs still function on Android 16 (target SDK 36). Deprecation means future removal, not current breakage.
- **Why deferred:**
  - Replacements (`registerScanResultsCallback`, `ConnectivityManager.NetworkCallback`) require fundamentally different callback patterns
  - The migration is ~2 hours of work with testing
  - No user impact on the target device (Pixel 10a, Android 16)
  - `@Suppress("DEPRECATION")` annotations are already in place
- **Action:** Documented for v0.3 roadmap.

### 5. R8/ProGuard Hardening

- **Status:** CONFIRMED OPEN → FIXED
- **Root cause:** `isMinifyEnabled = false` in release build type. Default proguard-rules.pro was empty.
- **Risk:** 63MB debug APK includes all of `material-icons-extended` and other unused code. Not a security risk but a size/performance issue.
- **Fix applied:**
  - Enabled `isMinifyEnabled = true` and `isShrinkResources = true` for release
  - Wrote comprehensive `proguard-rules.pro` with keep rules for:
    - Kotlin metadata and annotations
    - Hilt/Dagger entry points
    - Room entities, DAOs, and database classes
    - kotlinx.serialization classes and serializers
    - Ktor network library
    - OkHttp (suppressed platform warnings)
    - SIGNAL enum types (used in Room TypeConverters)
    - Compose navigation `@Serializable` route objects
  - Added stack trace preservation (`-keepattributes SourceFile,LineNumberTable`)
- **Validation:** Release build succeeds. APK size: 63MB → 4.9MB (92% reduction).

### 6. Data Retention Policy

- **Status:** CONFIRMED OPEN → FIXED
- **Root cause:** No cleanup logic existed. `network_events` table grew indefinitely. At 100 events/minute, ~43MB/day.
- **Risk:** Medium. Multi-day field deployments could exhaust phone storage, especially on 128GB devices running other apps.
- **Fix applied:**
  - Added DAO queries: `getSessionSummaries()`, `deleteOlderThan()`, `getTotalEventCount()`
  - Created `SessionSummary` data class for session listing
  - Created `DataRetentionManager` singleton with configurable cleanup
  - Added `retentionDays` preference (default: 30 days, 0 = disabled)
  - Wired cleanup to `SignalApplication.onCreate()` via background coroutine
  - Cleanup runs once per app start, non-blocking
- **Validation:** 4 unit tests for DataRetentionManager, all passing.

## Adjacent Fixes

### Deprecated Icon Warning
- **File:** `SignalNavHost.kt`
- **Issue:** `Icons.Outlined.Message` deprecated in favor of `Icons.AutoMirrored.Outlined.Message`
- **Fix:** Updated import and usage. Build warnings eliminated.

### Test Infrastructure
- **Added:** `testOptions.unitTests.isReturnDefaultValues = true` in `build.gradle.kts`
- **Reason:** Required for unit tests that touch `android.util.Log` indirectly (e.g., DataRetentionManager logging).

## Adjacent Issues Documented (Not Fixed)

| Issue | Risk | Reason Deferred |
|-------|------|-----------------|
| Deprecated WiFi APIs | Low | Still functional on target device; migration is non-trivial |
| SyslogService.onDestroy uses runBlocking | Low | Documented as intentional; flush() is fast (~1 DB insert) |
| ScannerViewModel rssiRingBuffer no sync | None | Single-writer pattern (sequential in one coroutine) |
| No Compose UI tests | Low | Requires instrumented test setup; not blocking |
| No Room DAO integration tests | Low | Requires in-memory DB with Android context |

## Test Summary

| Test Suite | Tests | Status |
|------------|-------|--------|
| CiscoWlcParserTest | 11 | Pass |
| WifiScanResultTest | 10 | Pass |
| EventPipelineTest (NEW) | 8 | Pass |
| SyslogMessageTest | 6 | Pass |
| SessionExporterTest | 4 | Pass |
| DataRetentionManagerTest (NEW) | 4 | Pass |
| NetworkEventTest | 2 | Pass |
| OpenClawClientTest | 2 | Pass |
| VendorDetectorTest | 2 | Pass |
| **Total** | **49** | **All pass** |
