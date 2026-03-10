# SIGNAL Engineering Audit Report

**Date:** 2026-03-09
**Auditor:** Claude (Principal Android Systems Engineer)
**Scope:** Full codebase review — 42 Kotlin source files, 7 test files

## Executive Summary

The codebase is a functional MVP with solid architecture fundamentals (MVVM, Hilt DI, Room, strategy pattern for parsers). Several issues need attention before field deployment: a coroutine scope leak in SyslogService, missing runtime permission for notifications, the syslog port configuration not being wired, and deprecated WiFi APIs. No critical security vulnerabilities found.

**Overall Assessment:** Deployable for testing. Needs hardening for continuous field use.

---

## Critical Issues

### C1: SyslogService creates its own CoroutineScope — should use lifecycleScope

**File:** `service/SyslogService.kt:31`
**Impact:** Potential coroutine leak

```kotlin
private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
```

The service creates a manual `CoroutineScope` and cancels it in `onDestroy()`. This works, but if Android force-kills the process (not the service), `onDestroy()` may not be called. The coroutines would leak until the process dies.

**Better approach:** Use `LifecycleService` from `androidx.lifecycle:lifecycle-service` which provides a `lifecycleScope` that's tied to the service's lifecycle. However, since this is a `@AndroidEntryPoint` Hilt service, it already integrates with lifecycle. The manual scope approach works for the MVP but should be revisited.

**Risk:** Low for a foreground service (they're rarely force-killed). Medium for a background service.

### C2: SyslogReceiver.start() is not idempotent with scope reuse

**File:** `data/syslog/SyslogReceiver.kt:17-18`
**Impact:** After stop() + start(), the receiver silently does nothing

```kotlin
suspend fun start(scope: CoroutineScope) {
    if (job != null) return // prevent double-start
```

After `stop()` sets `job = null`, calling `start()` again works. But the `scope` parameter passed in `SyslogService.onStartCommand()` is the same `scope` that launched the previous job. If that scope was cancelled (e.g., during `onDestroy()`), the new `scope.launch` returns a cancelled job immediately. The receiver appears started but receives nothing.

**Fix:** Check `job?.isActive` instead of `job != null`, and verify the scope is still active.

### C3: syslogPort preference is not wired to SyslogReceiver

**File:** `data/prefs/SignalPreferences.kt:16`, `data/syslog/SyslogReceiver.kt:10`
**Impact:** Settings screen lets user change the port, but the receiver ignores it

The port is hardcoded at construction time:
```kotlin
private val receiver = SyslogReceiver(port = 1514) // in SyslogService
```

`SignalPreferences.syslogPort` is never read by the receiver.

**Fix:** Inject `SignalPreferences` into `SyslogService` and pass `prefs.syslogPort` to the receiver.

---

## Important Issues

### I1: No POST_NOTIFICATIONS runtime permission request (API 33+)

**File:** `service/SyslogService.kt:62`
**Impact:** Foreground service notification silently hidden on API 33+

On Android 13+, apps must request `POST_NOTIFICATIONS` at runtime. Without the grant, `startForeground()` still works but the notification is invisible. This means the user sees no indication the service is running.

**Fix:** Request `POST_NOTIFICATIONS` in `SyslogScreen` before calling `startListening()`. Check the permission first with `ContextCompat.checkSelfPermission()`.

### I2: CancellationException not handled in SyslogService pipeline collector

**File:** `service/SyslogService.kt:51-56`
**Impact:** CancellationException silently caught and logged as error

```kotlin
} catch (e: Exception) {
    android.util.Log.e("SyslogService", "Failed to process message", e)
}
```

This catch block will catch `CancellationException`, log it as an error, and swallow it. This breaks structured concurrency — the coroutine won't actually cancel.

**Fix:**
```kotlin
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    android.util.Log.e("SyslogService", "Failed to process message", e)
}
```

### I3: SyslogViewModel holds ApplicationContext — potential for stale service binding

**File:** `ui/syslog/SyslogViewModel.kt:21`
**Impact:** Service binding outlives the Activity

The ViewModel holds `@ApplicationContext` and calls `context.bindService()`. This means the binding persists across configuration changes (good) but also persists if the ViewModel is held after the Activity is destroyed (e.g., in a retained scope). The `onCleared()` override handles unbinding, but if the ViewModel is leaked, the binding leaks.

**Risk:** Low — Hilt-managed ViewModels are properly scoped. But worth noting.

### I4: allMessages in SyslogViewModel uses synchronizedList but operations aren't atomic

**File:** `ui/syslog/SyslogViewModel.kt:39`
**Impact:** Potential race between add and toList

```kotlin
private val allMessages = java.util.Collections.synchronizedList(mutableListOf<SyslogMessage>())
```

Individual operations (add, removeLast, clear) are synchronized, but the sequence `add(0, msg)` → `removeLast()` → `applyFilter()` in the collector is NOT atomic. If `clearMessages()` is called from the UI thread while a message is being processed on IO, the list could be in an inconsistent state.

**Fix:** Use a `Mutex` or move all list operations to a single coroutine (single-writer pattern).

### I5: WiFi scanning uses deprecated APIs

**File:** `data/wifi/WifiScanner.kt:41-42, 47-48`
**Impact:** APIs may be removed in future Android versions

`WifiManager.startScan()` and `WifiManager.connectionInfo` are deprecated. The replacement is `WifiManager.registerScanResultsCallback()` and `ConnectivityManager.NetworkCallback`.

**Risk:** Low for now — deprecated APIs still work on Android 16. But they could be removed in a future version.

### I6: No database migration strategy

**File:** `di/DatabaseModule.kt:21-25`
**Impact:** Adding columns crashes the app on existing installs

```kotlin
Room.databaseBuilder(context, SignalDatabase::class.java, "signal_database").build()
```

No `fallbackToDestructiveMigration()` and no `Migration` objects. If you change the schema and increment the version, Room throws `IllegalStateException` on existing installs.

**Fix for development:** Add `.fallbackToDestructiveMigration()`.
**Fix for production:** Write proper `Migration` objects.

### I7: SharedFlow may drop messages under burst load

**File:** `data/syslog/SyslogReceiver.kt:12`
**Impact:** Syslog messages lost during high-volume bursts

```kotlin
private val _messages = MutableSharedFlow<SyslogMessage>(extraBufferCapacity = 256)
```

`MutableSharedFlow` with `extraBufferCapacity = 256` and no `replay` means that if the buffer fills (256 pending messages), `emit()` will suspend, blocking the UDP receive loop. This prevents message loss but creates backpressure on the socket.

Actually, the code uses `_messages.emit(message)` which is a suspending call. During a burst of >256 messages where the pipeline is slow, the UDP receive loop will pause, and the OS UDP buffer may overflow, dropping datagrams at the OS level.

**Fix:** Use `tryEmit()` and accept that some messages may be dropped during extreme bursts, or increase the buffer. For a field tool, `tryEmit()` with a larger buffer (1024) and a drop counter is more appropriate than blocking.

### I8: EventPipeline.currentSessionId is not thread-safe for compound operations

**File:** `data/EventPipeline.kt:17-25`
**Impact:** Race between newSession() and processSyslogMessage()

`currentSessionId` is `@Volatile` (individual reads/writes are visible), but `newSession()` writes and `processSyslogMessage()` reads without synchronization. A message being processed during `newSession()` could be assigned to either the old or new session.

**Risk:** Low — `newSession()` is unlikely to be called during active syslog capture. But it's worth a `Mutex` or `AtomicReference` if session management becomes more complex.

---

## Minor Issues

### M1: ExampleInstrumentedTest.kt and ExampleUnitTest.kt are scaffolding leftovers

**Files:** `androidTest/.../ExampleInstrumentedTest.kt`, `test/.../ExampleUnitTest.kt`
**Fix:** Delete them.

### M2: versionName "1.0" is misleading

**File:** `app/build.gradle.kts:19`
**Fix:** Change to `"0.1.0"` to match the actual development state.

### M3: R8 TODO but no proguard-rules.pro content

**File:** `app/build.gradle.kts:26-27`
**Fix:** When enabling R8, you'll need keep rules for Hilt, Room, kotlinx.serialization, and Ktor. Create a proper `proguard-rules.pro`.

### M4: No .gitignore for build/ directory

**File:** `.gitignore`
**Impact:** Build artifacts might be committed accidentally.
**Fix:** Verify `.gitignore` includes `build/`, `.gradle/`, `local.properties`.

### M5: Material Icons Extended pulls in entire icon set

**File:** `app/build.gradle.kts:58`
**Impact:** Adds ~5MB to the APK

```kotlin
implementation(libs.androidx.compose.material.icons.extended)
```

The app uses only 5 icons (Wifi, Message, Timeline, Settings, ArrowBack). The extended library includes thousands. R8 would strip unused icons, but R8 is disabled.

**Fix:** Either enable R8, or switch to specific icon imports from the core set.

### M6: tools:targetApi="31" in manifest is outdated

**File:** `AndroidManifest.xml:30`
**Impact:** Cosmetic — doesn't affect behavior. Should be 36 to match targetSdk.

---

## Performance Observations

### Good
- Room indices cover all query patterns
- Regex patterns in CiscoWlcParser are compiled once (class-level properties)
- SharedFlow buffer size (256) is reasonable for normal syslog rates
- WiFi connection poll (2s) is acceptable for active monitoring
- RSSI history capped at 60 points prevents unbounded growth
- Paint objects in SignalChart are now properly remembered

### Watch
- 5000-message in-memory list in SyslogViewModel (~1MB). Monitor during long sessions.
- WiFi `triggerScan()` calls `startScan()` which has a throttling limit (4 scans per 2 minutes for normal apps). The polling loop in ScannerViewModel triggers `connectionInfo()` every 2s but doesn't trigger scans — scans only happen on user button tap. This is correct.

### Risk
- Extended syslog capture (hours) with high message rates could stress Room inserts. Consider batching inserts (accumulate 10-50 events, call `insertAll`) instead of inserting one at a time.

---

## Security Assessment

### Good
- No secrets in source code
- OpenClaw communication is localhost-only (127.0.0.1)
- No external network calls (all AI goes through local OpenClaw)
- CSV export has field escaping (injection prevention)
- Service is not exported (`android:exported="false"`)

### Watch
- UDP port 1514 accepts datagrams from any source — no authentication. An attacker on the same network could inject fake syslog messages. This is inherent to syslog protocol (RFC 3164 has no authentication). For a field tool, this is acceptable.
- `SharedPreferences` stores the OpenClaw URL in plaintext. Not sensitive since it's always localhost.

---

## Recommendations Summary

| Priority | Issue | Effort |
|---|---|---|
| High | I2: CancellationException in SyslogService | 5 min |
| High | I1: POST_NOTIFICATIONS runtime permission | 30 min |
| High | C3: Wire syslog port to preferences | 15 min |
| Medium | I6: Database migration strategy | 15 min |
| Medium | C2: SyslogReceiver idempotency | 15 min |
| Medium | M2: Fix version to 0.1.0 | 2 min |
| Medium | M1: Delete scaffold test files | 2 min |
| Low | I5: Migrate deprecated WiFi APIs | 2 hours |
| Low | I7: SharedFlow burst handling | 30 min |
| Low | M5: Remove extended icons or enable R8 | 30 min |
