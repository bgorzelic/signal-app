# SIGNAL Architecture

## Design Philosophy

SIGNAL is designed as a **single-device, offline-first** tool. Everything runs on the phone — syslog capture, event parsing, database storage, AI triage. The only external dependency is the WLC sending syslog to the phone's IP. AI features route through OpenClaw running locally in Termux, which proxies to OpenRouter — but the core diagnostics features work without any network at all.

**Key constraints that shaped the architecture:**
- Must run continuously on a phone in a backpack during wireless site surveys
- Must handle bursty syslog (hundreds of messages/second during roaming events)
- Must work offline (no cloud dependencies for core features)
- Must be maintainable by engineers who may not be Android specialists

## Layers

### UI Layer (Jetpack Compose + Material 3)

Each screen is a `@Composable` function paired with a `@HiltViewModel`. The ViewModel exposes `StateFlow`s, and the Composable collects them with `collectAsState()` or `collectAsStateWithLifecycle()`.

**Why StateFlow, not LiveData?** StateFlow is Kotlin-native, works with coroutines, and doesn't require Android framework dependencies in the data layer. LiveData is an older pattern that ties you to the Android lifecycle API. StateFlow also has a well-defined initial value (unlike LiveData which starts null).

**Why `collectAsStateWithLifecycle()` over `collectAsState()`?** The `WithLifecycle` variant stops collecting when the screen is not visible (e.g., app backgrounded). This saves CPU by not processing emissions that nobody will see. The Scanner screen uses this for WiFi results. The Syslog screen uses plain `collectAsState()` since it also collects in the service (which needs to keep running).

**Navigation:** Uses Jetpack Navigation Compose with type-safe `@Serializable` route objects (no string-based routes). Four bottom tabs (Scanner, Syslog, Timeline, Settings) plus a non-tab Import screen reached via navigation. The `popUpTo` / `saveState` / `restoreState` pattern prevents back-stack accumulation when switching tabs.

### ViewModel Layer

ViewModels manage UI state and coordinate between the UI and data layers. They:
- Launch coroutines in `viewModelScope` (automatically cancelled when the ViewModel is cleared)
- Expose `StateFlow` for reactive UI updates
- Never hold references to Activities, Fragments, or Views (prevents memory leaks)
- Survive configuration changes (screen rotation) — Android creates a new Activity but reuses the same ViewModel

**Key pattern — `flatMapLatest`:** In `TimelineViewModel`, when the user selects a different client MAC, `flatMapLatest` cancels the previous Room database query and starts a new one. This prevents stale data from a previous selection showing up briefly. Without `flatMapLatest`, switching clients would briefly show the old client's events while the new query loads.

**Key pattern — `stateIn`:** Converts a cold `Flow` (from Room) into a hot `StateFlow` with `SharingStarted.WhileSubscribed(5000)`. The 5000ms stop timeout means: if no one is collecting for 5 seconds, cancel the upstream Room query. This handles configuration changes — during a rotation, the UI disconnects and reconnects within ~200ms, well within the 5s window.

### Data Layer

Pure Kotlin (no Android dependencies except Room annotations). This layer contains:

- **Models:** `NetworkEvent`, `EventType`, `Vendor` — the core domain objects
- **Room Database:** SQLite via Room ORM. Provides reactive `Flow<List<NetworkEvent>>` queries that automatically update the UI when data changes. Uses TypeConverters for enum serialization with safe fallback defaults.
- **Parsers:** Strategy pattern via `VendorParser` interface. `VendorDetector` iterates parsers and delegates to whichever one claims the line.
- **EventPipeline:** The central coordinator. Takes raw syslog text, routes through parsers, batches results, and bulk-inserts into Room. Manages session IDs and coordinates the flush timer.
- **OpenClawClient:** HTTP client for AI features. Isolated from the rest of the data layer — removing AI would not affect parsing, storage, or display.

### Service Layer

`SyslogService` is an Android Foreground Service. It exists because:
1. Android aggressively kills background processes (within 1-5 minutes)
2. A UDP socket listener must stay alive while you walk around a building
3. Foreground services with notifications are the approved way to do long-running work on Android
4. The `specialUse` foreground service type is correct for network monitoring tools

The service owns the `SyslogReceiver` (UDP socket) and the `CoroutineScope` that drives the receive loop. It uses `START_STICKY` so Android restarts it if the system kills it for memory pressure.

### Dependency Injection (Hilt)

Hilt manages the object graph. Three modules provide dependencies:

| Module | Provides | Scope | Why Singleton |
|---|---|---|---|
| `DatabaseModule` | `SignalDatabase`, `NetworkEventDao` | Singleton | One database connection for the entire app |
| `NetworkModule` | `OkHttpClient` | Singleton | OkHttp manages a connection pool — sharing one client is more efficient |
| `WifiModule` | `WifiManager` | Singleton | System service — there's only one per process |

**Why Hilt over manual DI?** It eliminates manual constructor wiring and ensures singletons are truly single across the app. Without it, you'd need to manually pass `OkHttpClient` from the Application through every ViewModel that needs it, or risk creating multiple instances (each with its own connection pool).

**Why Hilt over Koin?** Hilt is compile-time verified — dependency graph errors are caught during build, not at runtime. Koin is simpler but runtime-resolved.

## Concurrency Model

### Coroutine Scopes

| Scope | Owner | Lifetime | Used For |
|---|---|---|---|
| `viewModelScope` | Each ViewModel | Screen visible → ViewModel cleared | UI-related async work (collecting flows, launching operations) |
| `scope` in SyslogService | SyslogService | Service started → `onDestroy()` | UDP receive loop + pipeline processing |
| `Dispatchers.IO` | Various | Per-call | Blocking I/O (OkHttp, Room inserts) |

### Threading Rules

1. **Never block the main thread.** All I/O (network, database, file) runs on `Dispatchers.IO`. The main thread is reserved for UI rendering (Compose recomposition).
2. **Room queries return `Flow`.** Flow collection happens on the calling dispatcher; Room does the actual query on a background thread internally.
3. **OkHttp calls use `response.use{}`** to ensure connections are closed even on exceptions. Leaking response bodies exhausts OkHttp's connection pool.
4. **CancellationException must be rethrown.** In Kotlin coroutines, catching `Exception` accidentally catches `CancellationException`, breaking structured concurrency. Every catch block that catches `Exception` checks for and rethrows `CancellationException`.
5. **SupervisorJob in service scope.** A failure in the pipeline collector (parsing a malformed message) should not cancel the UDP receive loop. `SupervisorJob` provides this isolation.

### SharedFlow (Syslog Messages)

`SyslogReceiver` uses `MutableSharedFlow<SyslogMessage>(extraBufferCapacity = 1024)`. This means:
- **Hot flow:** Emits whether or not anyone is collecting
- **1024-message buffer:** If collectors can't keep up, messages are buffered
- **`tryEmit` behavior:** Returns false (drops the message) if the buffer is full. A `droppedCount` counter tracks drops.
- **No replay:** Late subscribers don't see old messages (they start fresh)

Two collectors exist simultaneously:
1. **SyslogService** feeds messages to `EventPipeline` for database storage
2. **SyslogViewModel** keeps messages in an in-memory list for the UI

This is why `SharedFlow` was chosen over `Channel` — multiple collectors from the same stream without fan-out logic.

## Database Schema

### network_events (v1)

| Column | Type | Notes |
|---|---|---|
| id | INTEGER | Primary key, auto-increment |
| timestamp | INTEGER | Unix milliseconds |
| eventType | TEXT | Stored as enum name via TypeConverter (fallback: UNKNOWN) |
| clientMac | TEXT | Nullable. MAC address `AA:BB:CC:DD:EE:FF` |
| apName | TEXT | Nullable. Access point name |
| bssid | TEXT | Nullable. AP radio MAC |
| channel | INTEGER | Nullable. WiFi channel number |
| rssi | INTEGER | Nullable. Signal strength in dBm |
| reasonCode | INTEGER | Nullable. IEEE 802.11 reason code |
| vendor | TEXT | Stored as enum name (fallback: GENERIC) |
| rawMessage | TEXT | Original syslog line — preserved for AI analysis and debugging |
| sessionId | TEXT | Groups events by capture session (UUID, generated at app start) |

**Indices:**
- `[sessionId]` — all queries filter by session
- `[sessionId, eventType]` — filter by event type within a session
- `[sessionId, clientMac]` — client journey queries (roaming timeline)

**Why these indices matter:** Without the composite index on `[sessionId, clientMac]`, the Timeline screen's `getClientJourney` query would do a full table scan for every client selection. With the index, it's an O(log n) lookup.

## Parser Architecture

The parser layer uses the **Strategy pattern**:

```
VendorDetector (router — iterates parsers, delegates to first match)
  ├── CiscoWlcParser    → canParse() checks for Cisco indicators
  │     Detects: apfMsConnTask, %DOT11, %DOT1X, %CLIENT_ORCH, wncd:, capwap
  │     Extracts: MAC, AP name, channel, RSSI, reason code
  │     Events: ROAM, ASSOC, DISASSOC, DEAUTH, AUTH
  │
  ├── [ArubaParser]     → future: AOS-CX, AOS-8 formats
  ├── [MerakiParser]    → future: Meraki MR syslog
  └── [RuckusParser]    → future: Ruckus SmartZone syslog
```

**How to add a new parser:**
1. Create a new class implementing `VendorParser`
2. Implement `canParse(line)` — returns true if the line matches your vendor's patterns
3. Implement `parse(line, sessionId)` — extract fields and return a `NetworkEvent`
4. Add an instance to `VendorDetector`'s parser list
5. Write tests using real syslog samples from the target vendor

**CiscoWlcParser details:**
- Detection order matters: DEAUTH is checked before DISASSOC before ASSOC (a line can contain multiple keywords, e.g., "Deauthenticated" contains "auth")
- All regex patterns are compiled once at class construction (not per-call)
- AP name extraction tries two formats: `AP name (name)` (9800) and `MAP name` (AireOS)
- MAC addresses are extracted case-insensitively

## Key Design Decisions

### 1. Ktor for UDP, OkHttp for HTTP
Ktor's raw socket API is clean for UDP: `aSocket(selectorManager).udp().bind()`. OkHttp is the standard Android HTTP client with proper connection pooling, timeouts, and retry logic. Using one library for both would be less natural.

### 2. SharedFlow over Channel for syslog
SharedFlow supports multiple collectors (service + UI) from the same stream. A Channel would require manual fan-out logic (splitting each message to two consumers). SharedFlow also provides backpressure via the buffer.

### 3. Foreground service with specialUse type
The syslog listener is a network monitoring tool, which doesn't fit standard foreground service types (`mediaPlayback`, `location`, `camera`, etc.). `specialUse` with a description string is the correct declaration for API 34+. Google Play reviewers read the `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` to understand the use case.

### 4. SharedPreferences over DataStore
DataStore is newer and recommended by Google, but SharedPreferences is simpler for three key-value pairs with synchronous reads. No migration complexity, no coroutine overhead for reading a port number. If the preference count grows significantly, migrate to DataStore.

### 5. No repository layer
For an MVP with a single data source (Room), the `EventPipeline` serves as the coordination layer — it handles batching, session management, and query routing. A formal Repository pattern would add abstraction without benefit. Add it if/when we introduce remote sync or multiple data sources.

### 6. Batch inserts via EventPipeline
Individual Room inserts create one SQLite transaction per event. Batch inserting 20 events in one `insertAll()` call reduces transaction overhead by ~20x. The 500ms flush timer ensures events are visible in the UI within half a second even during low-rate capture.

### 7. Manual CoroutineScope in SyslogService
The service uses `CoroutineScope(SupervisorJob() + Dispatchers.IO)` instead of `lifecycleScope`. This is intentional: `lifecycleScope` from `LifecycleService` is an additional dependency, and the manual approach gives explicit control over the dispatcher and supervisor strategy. The scope is cancelled in `onDestroy()`.

## Compose Recomposition Notes

For maintainers unfamiliar with Compose, here are the key recomposition behaviors in SIGNAL:

- **`collectAsState()`** triggers recomposition when the StateFlow emits a new value. If the value hasn't changed (structural equality), no recomposition occurs.
- **`items()` with `key`** in LazyColumn means Compose can skip unchanged items when the list changes. Every `items()` call uses a stable key (message ID, event ID, or BSSID).
- **`remember` blocks** survive recomposition. The `SimpleDateFormat` in `SyslogMessageRow` is created once per composition, not once per message.
- **Canvas composables** (SignalChart, timeline dot+line) do custom drawing — they don't create child composables, so they have minimal recomposition overhead.
