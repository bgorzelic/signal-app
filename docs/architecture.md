# SIGNAL Architecture

## Design Philosophy

SIGNAL is designed as a **single-device, offline-first** tool. Everything runs on the phone — syslog capture, event parsing, database storage, AI triage. The only external dependency is the WLC sending syslog to the phone's IP.

## Layers

### UI Layer (Jetpack Compose + Material 3)

Each screen is a `@Composable` function paired with a `@HiltViewModel`. The ViewModel exposes `StateFlow`s, and the Composable collects them with `collectAsState()` or `collectAsStateWithLifecycle()`.

**Why StateFlow, not LiveData?** StateFlow is Kotlin-native, works with coroutines, and doesn't require Android framework dependencies in the data layer. LiveData is an older pattern that ties you to the Android lifecycle API.

**Navigation:** Uses Jetpack Navigation Compose with type-safe `@Serializable` route objects. Four bottom tabs (Scanner, Syslog, Timeline, Settings) plus a non-tab Import screen reached via navigation.

### ViewModel Layer

ViewModels manage UI state and coordinate between the UI and data layers. They:
- Launch coroutines in `viewModelScope` (automatically cancelled when the screen is destroyed)
- Expose `StateFlow` for reactive UI updates
- Never hold references to Activities, Fragments, or Views

**Key pattern — `flatMapLatest`:** In `TimelineViewModel`, when the user selects a different client MAC, `flatMapLatest` cancels the previous database query and starts a new one. This prevents stale data from a previous selection showing up briefly.

### Data Layer

Pure Kotlin (no Android dependencies except Room annotations). This layer contains:

- **Models:** `NetworkEvent`, `EventType`, `Vendor` — the core domain objects
- **Room Database:** SQLite via Room ORM. Provides reactive `Flow<List<NetworkEvent>>` queries that automatically update the UI when data changes.
- **Parsers:** Strategy pattern via `VendorParser` interface. `VendorDetector` iterates parsers and delegates to whichever one claims the line.
- **EventPipeline:** The central coordinator. Takes raw syslog text, routes through parsers, and inserts results into Room.
- **OpenClawClient:** HTTP client for AI features. Isolated from the rest of the data layer.

### Service Layer

`SyslogService` is an Android Foreground Service. It exists because:
1. Android aggressively kills background processes
2. A UDP socket listener must stay alive while you walk around a building
3. Foreground services with notifications are the approved way to do long-running work

The service owns the `SyslogReceiver` (UDP socket) and the `CoroutineScope` that drives the receive loop.

### Dependency Injection (Hilt)

Hilt manages the object graph. Three modules provide dependencies:

| Module | Provides | Scope |
|---|---|---|
| `DatabaseModule` | `SignalDatabase`, `NetworkEventDao` | Singleton |
| `NetworkModule` | `OkHttpClient` | Singleton |
| `WifiModule` | `WifiManager` | Singleton |

**Why Hilt?** It eliminates manual constructor wiring and ensures singletons are truly single across the app. Without it, you'd need to manually pass `OkHttpClient` from the Application through every ViewModel that needs it.

## Concurrency Model

### Coroutine Scopes

| Scope | Owner | Lifetime | Used For |
|---|---|---|---|
| `viewModelScope` | Each ViewModel | Screen visible → destroyed | UI-related async work |
| `scope` in SyslogService | SyslogService | Service started → destroyed | UDP receive loop + pipeline |
| `Dispatchers.IO` | Various | Per-call | Blocking I/O (OkHttp, Room) |

### Threading Rules

1. **Never block the main thread.** All I/O (network, database, file) runs on `Dispatchers.IO`.
2. **Room queries return `Flow`.** Flow collection happens on the calling dispatcher; Room does the actual query on a background thread.
3. **OkHttp calls use `response.use{}`** to ensure connections are closed even on exceptions.
4. **CancellationException must be rethrown.** In Kotlin coroutines, catching `Exception` accidentally catches `CancellationException`, breaking structured concurrency. Every catch block that catches `Exception` must check for and rethrow `CancellationException`.

### SharedFlow (Syslog Messages)

`SyslogReceiver` uses `MutableSharedFlow<SyslogMessage>(extraBufferCapacity = 256)`. This means:
- **Hot flow:** Emits whether or not anyone is collecting
- **256-message buffer:** If collectors can't keep up, newer messages still get buffered
- **No replay:** Late subscribers don't see old messages

Two collectors exist:
1. `SyslogService` feeds messages to `EventPipeline` for database storage
2. `SyslogViewModel` keeps messages in a list for the UI

## Database Schema

### network_events (v1)

| Column | Type | Notes |
|---|---|---|
| id | INTEGER | Primary key, auto-increment |
| timestamp | INTEGER | Unix milliseconds |
| eventType | TEXT | Stored as enum name via TypeConverter |
| clientMac | TEXT | Nullable. MAC address `AA:BB:CC:DD:EE:FF` |
| apName | TEXT | Nullable. Access point name |
| bssid | TEXT | Nullable. AP radio MAC |
| channel | INTEGER | Nullable. WiFi channel number |
| rssi | INTEGER | Nullable. Signal strength in dBm |
| reasonCode | INTEGER | Nullable. IEEE 802.11 reason code |
| vendor | TEXT | Stored as enum name. Default `GENERIC` |
| rawMessage | TEXT | Original syslog line |
| sessionId | TEXT | Groups events by capture session |

**Indices:** `[sessionId]`, `[sessionId, eventType]`, `[sessionId, clientMac]`

## Parser Architecture

The parser layer uses the **Strategy pattern**:

```
VendorDetector (router)
  ├── CiscoWlcParser    → canParse() checks for Cisco indicators
  ├── [ArubaParser]     → future
  ├── [MerakiParser]    → future
  └── [RuckusParser]    → future
```

**How it works:**
1. `VendorDetector.parse(line, sessionId)` iterates through registered parsers
2. Each parser's `canParse(line)` checks for vendor-specific keywords
3. The first parser that matches extracts fields via regex
4. Returns a `NetworkEvent` or `null`

**CiscoWlcParser specifics:**
- Detects Cisco via keywords: `apfMsConnTask`, `%DOT11`, `%DOT1X`, `%CLIENT_ORCH`, `wncd:`, `capwap`
- Extracts: MAC address, AP name, channel, RSSI, reason code
- Event type detection order matters: DEAUTH before DISASSOC before ASSOC (a line can match multiple patterns)
- All regex patterns are compiled once at class construction time (not per-call)

## Key Design Decisions

1. **Ktor for UDP, OkHttp for HTTP.** Ktor's raw socket API is clean for UDP. OkHttp is the standard Android HTTP client with proper connection pooling and timeouts.

2. **SharedFlow over Channel for syslog.** SharedFlow supports multiple collectors (service + UI) from the same stream. A Channel would require fan-out logic.

3. **Foreground service with specialUse type.** The syslog listener is a network monitoring tool, which doesn't fit standard foreground service types (mediaPlayback, location, etc.). `specialUse` with a description string is the correct declaration for API 34+.

4. **SharedPreferences over DataStore.** DataStore is newer and recommended, but SharedPreferences is simpler for three key-value pairs with synchronous reads. No migration complexity.

5. **No repository layer.** For an MVP with a single data source (Room), the EventPipeline serves as the coordination layer. A formal Repository pattern would be appropriate if we add remote sync or multiple data sources.
