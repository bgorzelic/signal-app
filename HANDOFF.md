# SIGNAL — Engineering Handoff Document

## Project Overview

SIGNAL is a native Android wireless network diagnostics tool built for field engineers. It runs on a dedicated Pixel 10a with root access, Termux, and OpenClaw for on-device AI. The app captures syslog from wireless LAN controllers (WLCs), parses vendor-specific events, visualizes client roaming timelines, and provides AI-powered triage — all without cloud dependencies.

**Organization:** AI Aerial Solutions
**Package:** `dev.aiaerial.signal`
**Target Device:** Pixel 10a, Android 16, rooted
**Version:** 0.1.0-dev (MVP)

## Architecture Diagram

```
┌──────────────────────────────────────────────────────────────────────────┐
│                              SIGNAL App                                  │
│                                                                          │
│  ┌───────────────────────────── UI Layer ───────────────────────────────┐│
│  │                                                                      ││
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌───────┐ ││
│  │  │ Scanner  │  │  Syslog  │  │ Timeline │  │ Settings │  │Import │ ││
│  │  │  Screen  │  │  Screen  │  │  Screen  │  │  Screen  │  │Screen │ ││
│  │  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘  └───┬───┘ ││
│  │       │              │             │              │            │      ││
│  │  ┌────┴─────┐  ┌────┴─────┐  ┌────┴─────┐  ┌────┴─────┐ ┌───┴───┐  ││
│  │  │ Scanner  │  │ Syslog   │  │ Timeline │  │ Settings │ │Import │  ││
│  │  │ViewModel│  │ ViewModel│  │ ViewModel│  │ ViewModel│ │ViewMdl│  ││
│  │  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘ └───┬───┘  ││
│  └───────┼──────────────┼─────────────┼──────────────┼───────────┼──────┘│
│          │              │             │              │           │       │
│  ┌───────┼──────────────┼─────────────┼──────────────┼───────────┼──────┐│
│  │       │         Data │Layer        │              │           │      ││
│  │  ┌────┴─────┐  ┌─────┴──────┐     │        ┌─────┴─────┐    │      ││
│  │  │  WiFi    │  │  Syslog    │     │        │  Signal   │    │      ││
│  │  │ Scanner  │  │  Service   │     │        │  Prefs    │    │      ││
│  │  └──────────┘  │ (FGS)     │     │        └───────────┘    │      ││
│  │                │  ┌────────┐│     │                         │      ││
│  │                │  │Syslog  ││     │                         │      ││
│  │                │  │Receiver││     │                         │      ││
│  │                │  │UDP:1514││     │                         │      ││
│  │                │  └───┬────┘│     │                         │      ││
│  │                └──────┼─────┘     │                         │      ││
│  │                       │           │                         │      ││
│  │                 ┌─────┴───────────┴─────────────────────────┴────┐ ││
│  │                 │              Event Pipeline                     │ ││
│  │                 │   VendorDetector → CiscoWlcParser               │ ││
│  │                 │   Batch writer (20 events / 500ms flush)        │ ││
│  │                 └──────────────────┬──────────────────────────────┘ ││
│  │                                    │                                ││
│  │                 ┌──────────────────┴──────────────────────────────┐ ││
│  │                 │             Room Database (SQLite)               │ ││
│  │                 │   network_events table                          │ ││
│  │                 │   Indices: [sessionId], [sessionId,eventType],  │ ││
│  │                 │            [sessionId,clientMac]                 │ ││
│  │                 └──────────────────┬──────────────────────────────┘ ││
│  │                                    │                                ││
│  │                 ┌──────────────────┴──────────────────────────────┐ ││
│  │                 │        OpenClaw Client (OkHttp)                 │ ││
│  │                 │   POST http://127.0.0.1:18789/api/v1/chat      │ ││
│  │                 └────────────────────────────────────────────────┘ ││
│  └──────────────────────────────────────────────────────────────────────┘│
│                                                                          │
│  ┌──────────── Dependency Injection (Hilt) ──────────────┐              │
│  │  DatabaseModule: Room DB + DAO                         │              │
│  │  NetworkModule:  OkHttpClient (5s connect, 30s read)  │              │
│  │  WifiModule:     WifiManager                           │              │
│  └────────────────────────────────────────────────────────┘              │
└──────────────────────────────────────────────────────────────────────────┘
                               │
                    ┌──────────┴──────────┐
                    │     Pixel 10a       │
                    │  ┌────────────────┐ │
                    │  │    Termux      │ │
                    │  │  OpenClaw GW   │ │
                    │  │  → OpenRouter  │ │
                    │  │  → Claude 3.5  │ │
                    │  │     Haiku      │ │
                    │  └────────────────┘ │
                    │  ┌────────────────┐ │
                    │  │   Tailscale    │ │
                    │  │  (stable IP)   │ │
                    │  └────────────────┘ │
                    └─────────────────────┘
```

## Data Flow

### Syslog Ingestion Path (real-time capture)
```
WLC sends UDP datagram
  → Network (Tailscale mesh or LAN)
  → Pixel 10a, port 1514
  → SyslogReceiver (Ktor UDP socket, Dispatchers.IO)
  → SyslogMessage.parse() — RFC 3164 extraction (priority, facility, severity, hostname)
  → MutableSharedFlow emission (1024-message buffer)
  → Two concurrent collectors:
      ├── SyslogService → EventPipeline.processSyslogMessage()
      │     → VendorDetector.parse() → CiscoWlcParser (regex extraction)
      │     → NetworkEvent created
      │     → Batch buffer (20 events or 500ms flush timer)
      │     → NetworkEventDao.insertAll() → Room DB
      │
      └── SyslogViewModel → allMessages ArrayDeque (5000 cap)
            → applyFilter() → UI StateFlow → Compose recomposition
```

### WiFi Scanning Path
```
ScannerViewModel.init()
  → collectScanResults(): registers BroadcastReceiver via callbackFlow
  │    → WifiManager.SCAN_RESULTS_AVAILABLE_ACTION
  │    → Maps ScanResult → WifiScanResult (adds channel, band, security)
  │    → Sorted by RSSI descending → StateFlow → UI
  │
  → pollConnectionInfo(): every 2 seconds
       → WifiManager.connectionInfo (BSSID, RSSI, link speed, frequency)
       → RSSI ring buffer (60 samples = 2 min window)
       → StateFlow → SignalChart Canvas composable
```

### AI Triage Path
```
User taps event → TriageBottomSheet composable
  → LaunchedEffect triggers OpenClawClient.triageEvent()
  → buildTriagePrompt() formats event fields + raw syslog
  → OkHttp POST to http://127.0.0.1:18789/api/v1/chat
  → JSON body: { messages: [system, user], model: "haiku", stream: false }
  → OpenClaw Gateway (Termux process)
  → OpenRouter API → anthropic/claude-3.5-haiku
  → Response JSON parsed → choices[0].message.content
  → Displayed in ModalBottomSheet
```

### Log Import Path (manual paste)
```
User pastes WLC debug output → LogImportScreen
  → LogImportViewModel.parseLog()
  → EventPipeline.processLogBlock(text)
  → text.lines().mapNotNull { VendorDetector.parse(line, sessionId) }
  → Batch insert to Room via dao.insertAll()
  → Optional: analyzeWithAi() → OpenClawClient.analyzeLogBlock()
```

## Module Structure

```
app/src/main/java/dev/aiaerial/signal/
├── SignalApplication.kt          # @HiltAndroidApp — Hilt requires this on the Application class
├── MainActivity.kt               # Single-activity host — sets up Compose + edge-to-edge display
│
├── data/                          # Data layer — pure Kotlin, no Android UI dependencies
│   ├── model/
│   │   ├── NetworkEvent.kt        # Room @Entity — the core data model for all wireless events
│   │   ├── EventType.kt           # Enum: ROAM, AUTH, DEAUTH, ASSOC, DISASSOC, RF_CHANGE, etc.
│   │   └── Vendor.kt              # Enum: CISCO, ARUBA, MERAKI, RUCKUS, JUNIPER, GENERIC, ANDROID
│   ├── local/
│   │   ├── SignalDatabase.kt      # Room database (v1, fallbackToDestructiveMigration for dev)
│   │   ├── NetworkEventDao.kt     # DAO — Flow-based reactive queries for events
│   │   └── Converters.kt          # Room TypeConverters — safe enum ↔ String with fallback defaults
│   ├── syslog/
│   │   ├── SyslogReceiver.kt      # Ktor UDP socket listener — binds 0.0.0.0:port, loops on receive()
│   │   └── SyslogMessage.kt       # RFC 3164 syslog parser + data class (priority, severity, hostname)
│   ├── parser/
│   │   ├── VendorParser.kt        # Strategy pattern interface: canParse(line) + parse(line, sessionId)
│   │   ├── CiscoWlcParser.kt      # Cisco 9800/AireOS regex parser — extracts MAC, AP, channel, RSSI
│   │   └── VendorDetector.kt      # Router — iterates parsers, delegates to first match
│   ├── EventPipeline.kt           # Central coordinator — syslog → parse → batch → Room insert
│   ├── openclaw/
│   │   ├── OpenClawClient.kt      # OkHttp REST client for AI triage and log analysis
│   │   └── OpenClawStatus.kt      # Enum: CONNECTED, DISCONNECTED, CHECKING
│   ├── prefs/
│   │   └── SignalPreferences.kt   # SharedPreferences wrapper (openClawUrl, syslogPort, setupComplete)
│   ├── export/
│   │   └── SessionExporter.kt     # CSV/JSON export with field escaping (CSV injection prevention)
│   └── wifi/
│       ├── WifiScanner.kt         # Wraps WifiManager — scan results as callbackFlow, connection info
│       ├── WifiScanResult.kt      # Data class + computed: channel number, band string, width in MHz
│       └── WifiConnectionInfo.kt  # Current connection: SSID, BSSID, RSSI, link speed, frequency, IP
│
├── service/
│   └── SyslogService.kt           # Foreground service — keeps UDP listener alive during field surveys
│
├── di/                             # Hilt dependency injection modules
│   ├── DatabaseModule.kt           # Provides: SignalDatabase (singleton), NetworkEventDao
│   ├── NetworkModule.kt            # Provides: OkHttpClient (5s connect, 30s read timeout)
│   └── WifiModule.kt              # Provides: WifiManager (from system service)
│
└── ui/                             # Compose UI layer — each feature has Screen + ViewModel
    ├── navigation/
    │   └── SignalNavHost.kt        # Bottom nav (4 tabs) + NavHost with type-safe @Serializable routes
    ├── scanner/
    │   ├── ScannerScreen.kt        # WiFi scan results list + permission handling
    │   ├── ScannerViewModel.kt     # Scan polling (2s), RSSI ring buffer (60 samples)
    │   ├── SignalChart.kt          # Canvas RSSI line chart (-90 to -30 dBm range)
    │   └── WifiNetworkCard.kt      # Per-network card — SSID, BSSID, channel, band, width, security
    ├── syslog/
    │   ├── SyslogScreen.kt         # Live syslog feed + filter + service start/stop
    │   └── SyslogViewModel.kt      # Service binding, message buffer (5000), debounced filter
    ├── timeline/
    │   ├── TimelineScreen.kt       # Client MAC selector dropdown + event list
    │   ├── TimelineViewModel.kt    # flatMapLatest on selected client for reactive queries
    │   └── RoamingTimelineCard.kt  # Visual timeline — Canvas dot+line, event card with details
    ├── settings/
    │   ├── SettingsScreen.kt       # OpenClaw config, syslog port, setup guide, about
    │   └── SettingsViewModel.kt    # Health check, URL/port persistence
    ├── logimport/
    │   ├── LogImportScreen.kt      # Paste area, parse button, AI analysis button
    │   └── LogImportViewModel.kt   # Parse + AI analysis with CancellationException handling
    ├── triage/
    │   └── TriageBottomSheet.kt    # AI triage modal — triggered from event tap
    └── theme/
        └── Theme.kt               # Material 3 dynamic color theme
```

## Runtime Services

### SyslogService (Foreground Service)

**What it does:** Runs a UDP socket listener on a configurable port (default 1514), receives syslog datagrams from WLCs, parses them through the event pipeline, and stores parsed events in Room.

**Why it's a foreground service:** Android aggressively kills background processes. A foreground service with a persistent notification keeps the UDP listener alive while you walk around a building doing wireless surveys. Without it, Android would kill the socket listener within minutes.

**How it works:**
1. `SyslogViewModel.startListening()` calls `startForegroundService()` + `bindService()`
2. Service creates a notification channel (IMPORTANCE_LOW — no sound) and shows a persistent notification ("Listening on UDP port X")
3. `SyslogReceiver` opens a UDP socket via Ktor and loops on `socket.receive()`
4. Each datagram is parsed into a `SyslogMessage` and emitted via `MutableSharedFlow` (1024-message buffer)
5. A collector in the service feeds messages through `EventPipeline` to Room (batched: 20 events or 500ms flush)
6. The ViewModel binds to the service and collects the same `SharedFlow` for UI display

**Foreground service type:** `specialUse` (required on API 34+). This is the correct type for network monitoring tools that don't fit standard categories (mediaPlayback, location, etc.). The `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` property in the manifest explains the use case to Google Play reviewers.

**Lifecycle:** `START_STICKY` means Android will restart the service if it's killed. The `started` flag prevents duplicate coroutine launches on re-delivery of `onStartCommand`.

**Coroutine scope:** The service creates a manual `CoroutineScope(SupervisorJob() + Dispatchers.IO)`. The `SupervisorJob` means one child failing doesn't cancel siblings (important: the pipeline collector shouldn't kill the receive loop). Cancelled in `onDestroy()`.

## OpenClaw Integration Architecture

OpenClaw is an AI gateway that runs locally in Termux on the same phone. It proxies requests to Claude Haiku via OpenRouter, providing on-device AI without external cloud dependencies from the SIGNAL app's perspective.

### Architecture
```
SIGNAL App (Android process)
  └─ OkHttpClient (singleton, configured by Hilt)
     └─ POST http://127.0.0.1:18789/api/v1/chat
        └─ OpenClaw Gateway (Termux process, Node.js)
           └─ OpenRouter API (HTTPS, external)
              └─ anthropic/claude-3.5-haiku
```

### Endpoints Used
| Endpoint | Method | Purpose |
|---|---|---|
| `/` | GET | Health check — is OpenClaw running? |
| `/api/v1/chat` | POST | Chat completion (triage + log analysis) |

### Request Format
```json
{
  "messages": [
    { "role": "system", "content": "You are SIGNAL's wireless network analysis engine..." },
    { "role": "user", "content": "Explain this wireless network event..." }
  ],
  "model": "haiku",
  "stream": false
}
```

### Error Handling
- 5-second connect timeout, 30-second read timeout (configured in NetworkModule)
- Health check returns `DISCONNECTED` on `IOException`
- Chat errors return `"OpenClaw error: HTTP {code}"` or `"Error: {message}"`
- `CancellationException` is always rethrown (structured concurrency requirement)
- All `response.use { }` blocks ensure OkHttp connections are closed on all code paths

### Offline Behavior
The app works fully without OpenClaw — WiFi scanning, syslog capture, event parsing, timeline visualization, log import, and session export all function. AI features (triage bottom sheet, log analysis) show error messages when OpenClaw is unavailable. The Settings screen shows "Disconnected" and displays a setup guide.

## Networking Model

The app has two distinct network paths:

### 1. UDP Syslog (inbound from WLC)
- **Technology:** Ktor raw socket (`aSocket(selectorManager).udp().bind()`)
- **Bind address:** `0.0.0.0:port` (accepts from any interface)
- **Default port:** 1514 (configurable in Settings, stored in SharedPreferences)
- **Why 1514, not 514:** Standard syslog uses port 514, but binding below 1024 requires root on Linux/Android. Port 1514 is the conventional unprivileged alternative.
- **Network path:** WLC → Tailscale mesh (or direct LAN) → phone's IP → SyslogReceiver

### 2. HTTP to OpenClaw (localhost only)
- **Technology:** OkHttp (singleton client with connection pooling)
- **Target:** `http://127.0.0.1:18789` (never leaves the device)
- **This is IPC:** Inter-process communication between the SIGNAL Android app and the Termux OpenClaw Node.js process. No network traversal.

### Network Requirements
- Phone must be reachable from the WLC's network (Tailscale provides stable IP addressing)
- Termux must be running for AI features (OpenClaw gateway process)
- No internet required for core features (scanning, syslog capture, parsing)
- Internet required only for AI features (OpenClaw → OpenRouter → Claude)

## Permissions Model

| Permission | Type | Why | When Requested |
|---|---|---|---|
| `INTERNET` | Normal | OpenClaw HTTP calls | Auto-granted at install |
| `ACCESS_NETWORK_STATE` | Normal | Network connectivity checks | Auto-granted at install |
| `ACCESS_WIFI_STATE` | Normal | Read WiFi scan results | Auto-granted at install |
| `CHANGE_WIFI_STATE` | Normal | Trigger active WiFi scans | Auto-granted at install |
| `ACCESS_FINE_LOCATION` | Dangerous | **Required by Android** to return WiFi scan results | Runtime prompt on Scanner tab |
| `ACCESS_COARSE_LOCATION` | Dangerous | Fallback for location | Runtime (bundled with fine) |
| `FOREGROUND_SERVICE` | Normal | Keep syslog listener alive | Auto-granted at install |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Normal | Required on API 34+ for specialUse type | Auto-granted at install |
| `POST_NOTIFICATIONS` | Dangerous (API 33+) | Show foreground service notification | Runtime prompt before starting syslog |

**Important:** `ACCESS_FINE_LOCATION` is **not optional**. Android requires it to return WiFi scan results, even though SIGNAL doesn't use GPS. This is an Android platform requirement, not a SIGNAL design choice. If denied, the Scanner tab shows an explanation and a "Grant Permission" button.

## Foreground Services Explained

**Why does SIGNAL need a foreground service?**

Android has strict rules about what apps can do when they're not visible:
- **Regular background work:** Killed within minutes by Android's battery optimization
- **WorkManager:** Designed for deferrable tasks, not real-time monitoring
- **Foreground service:** The only way to keep a process alive indefinitely while the user is aware

SIGNAL's syslog listener must stay alive continuously while a wireless engineer walks around a building doing a site survey. The foreground service shows a persistent notification ("SIGNAL Syslog Receiver — Listening on UDP port 1514") so the user knows the listener is active, and Android knows not to kill it.

**Service type `specialUse`:** Android 14+ requires declaring what kind of work a foreground service does. Options like `mediaPlayback`, `location`, `camera` don't apply to network monitoring. `specialUse` with a subtype description is the correct declaration. The manifest property `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` describes the use case for Google Play review.

## Known Limitations

1. **Three-vendor parsing.** `CiscoWlcParser`, `ArubaParser`, and `MerakiParser` exist. Ruckus and Juniper Mist syslog formats are not yet handled. The `VendorParser` interface and `VendorDetector` router are ready for new parsers.

2. **WiFi scanning uses deprecated APIs.** `WifiManager.startScan()` and `WifiManager.connectionInfo` are deprecated. They still work on Android 16 but may be removed. Replacements: `registerScanResultsCallback()` and `ConnectivityManager.NetworkCallback`.

3. **Destructive migration fallback still active.** `fallbackToDestructiveMigration()` is retained during v0.x. Must be removed before v1.0 — all schema changes must use explicit migrations by then.

## Performance Considerations

### Memory
- **Syslog message buffer:** `SyslogViewModel` keeps up to 5000 `SyslogMessage` objects in memory. At ~200 bytes per message, this is ~1MB. Acceptable for field use.
- **RSSI history:** Capped at 60 data points (2 minutes at 2s interval). No persistence.
- **Room query results:** Flow-based queries hold the current result set in memory. For sessions with thousands of events, the Timeline screen could hold significant data. The `LIMIT` clause is not used — consider adding pagination.

### CPU
- **Regex patterns:** All compiled once at `CiscoWlcParser` class construction (not per-call). Each syslog line runs through ~6 regex checks.
- **WiFi poll interval:** 2 seconds. Each poll calls `wifiManager.connectionInfo` — a Binder IPC call to the system WiFi service. Acceptable but could be 5-10s for passive monitoring.
- **Batch inserts:** EventPipeline batches 20 events before inserting. This is efficient — individual inserts would create 20x more SQLite transactions.

### Network
- **SharedFlow buffer:** `SyslogReceiver` uses `extraBufferCapacity = 1024`. If the pipeline can't keep up with bursty syslog (>1024 messages queued), messages will be dropped (`tryEmit` returns false). A `droppedCount` counter tracks this.
- **UDP receive loop:** Runs on `Dispatchers.IO`. Each datagram is parsed synchronously before the next receive. Very high-rate syslog (>1000 msg/sec sustained) could cause backpressure.

### Battery
- **Primary drain:** WiFi scanning poll (every 2s) and the foreground service keeping the process alive
- **Mitigation:** Consider reducing poll interval to 5-10s when not actively monitoring, or pausing when screen is off
- **OpenClaw calls:** Only on user tap (not periodic), so minimal battery impact

### Storage
- **Room DB growth:** ~300 bytes per `NetworkEvent` row (with indices). At 100 events/minute, that's ~1.8MB/hour, ~43MB/day. Acceptable for multi-day use, but a retention policy is recommended for week-long deployments.

## Testing

### Unit Tests (12 test files, 79 tests)
| Test File | Coverage |
|---|---|
| `ArubaParserTest` | 12 tests: association, roaming, deauth, disassoc, auth, MAC formats |
| `CiscoWlcParserTest` | 11 tests: event type detection, field extraction, edge cases |
| `MerakiParserTest` | 11 tests: association, reassociation, auth, deauth, splash, AP name |
| `WifiScanResultTest` | 10 tests: channel derivation (2.4/5/6 GHz), band strings |
| `EventPipelineTest` | 8 tests: session management, batching, log block parsing |
| `SyslogMessageTest` | 6 tests: RFC 3164 parsing, severity levels, edge cases |
| `EmaSmootherTest` | 5 tests: variance reduction, convergence, alpha sensitivity |
| `SessionExporterTest` | 4 tests: CSV/JSON export, escaping, empty lists |
| `DataRetentionManagerTest` | 4 tests: cleanup logic, retention period, disabled mode |
| `VendorDetectorTest` | 4 tests: Cisco, Aruba, Meraki routing, unknown vendor |
| `NetworkEventTest` | 2 tests: full construction, nullable fields |
| `OpenClawClientTest` | 2 tests: triage prompt content |

### Running Tests
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

# All unit tests
./gradlew testDebugUnitTest

# Specific test class
./gradlew testDebugUnitTest --tests "dev.aiaerial.signal.data.parser.CiscoWlcParserTest"

# With console output
./gradlew testDebugUnitTest --info
```

### What's NOT Tested (and why)
- **Compose UI:** Requires instrumented tests with `composeTestRule`. Not set up yet.
- **Room DAO:** Requires an in-memory database with Android context (`@RunWith(AndroidJUnit4::class)`).
- **SyslogReceiver:** Would need a real or mocked UDP socket.
- **SyslogService:** Android service lifecycle requires Robolectric or instrumented tests.
- **EventPipeline:** Depends on Room DAO (needs mocking or in-memory DB).

## Developer Setup

See `docs/development.md` for full setup instructions. Quick start:

```bash
# 1. Clone
git clone https://github.com/bgorzelic/signal-app.git
cd signal-app

# 2. Set JAVA_HOME (Android Studio's bundled JDK)
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

# 3. Build
./gradlew assembleDebug

# 4. Run tests
./gradlew testDebugUnitTest

# 5. Deploy to connected device
./gradlew installDebug
```

## Device Setup (Termux/OpenClaw)

See `docs/device-setup.md` for full instructions. Summary:

1. Install SIGNAL APK on Pixel 10a
2. Grant location permission (required for WiFi scanning)
3. Install Termux from F-Droid
4. Install OpenClaw in Termux: `pkg install nodejs-lts && npx openclaw@latest init`
5. Start OpenClaw gateway: `npx openclaw gateway start`
6. Install Tailscale, configure always-on VPN
7. Point WLC syslog to `<phone-tailscale-ip>:1514`
8. Exempt Termux, Termux:Boot, and Tailscale from battery optimization

## Troubleshooting

### Build Issues
| Problem | Solution |
|---|---|
| "Could not determine java version" | Set `JAVA_HOME` to Android Studio's JDK |
| KSP annotation processing fails | `./gradlew clean assembleDebug` |
| "No connected devices" | `adb devices`, check USB cable, re-authorize USB debugging |

### Runtime Issues
| Problem | Check |
|---|---|
| No WiFi scan results | Location permission granted? Check Settings > Apps > SIGNAL > Permissions |
| Syslog not appearing | 1) Is foreground service running (check notification)? 2) Can you ping the phone from the WLC network? 3) Test with `echo "test" \| nc -u <phone-ip> 1514` |
| OpenClaw "Disconnected" | 1) SSH into phone: `ssh termux` 2) Check process: `ps aux \| grep openclaw` 3) Start: `npx openclaw gateway start` 4) Verify: `curl http://127.0.0.1:18789/` |
| App crashes on launch | `adb logcat *:E` — likely Room schema mismatch (clear data) or missing Hilt annotation |
| Phone overheating | Reduce WiFi scan frequency, dim screen, ensure good airflow |

## Engineering Audit Summary

See `docs/audit-report.md` for the full technical audit. Key findings:

### Resolved in MVP
- CancellationException handling in SyslogService pipeline collector
- POST_NOTIFICATIONS runtime permission request
- Syslog port wired from preferences to receiver
- SharedFlow buffer increased to 1024
- SyslogReceiver idempotency (checks `job?.isActive`)
- Database fallbackToDestructiveMigration added
- Version corrected to 0.1.0

### Resolved in Remediation Pass (2026-03-10)
- SyslogViewModel thread safety — was already fixed (Mutex in place)
- Room migration strategy — schema export enabled, migration scaffolding added
- EventPipeline.currentSessionId race — verified benign (`@Volatile` + flush-before-switch)
- R8/ProGuard — enabled for release builds, APK 63MB → 4.9MB
- Data retention policy — 30-day default auto-cleanup on app start
- Deprecated Icon warning — `Icons.Outlined.Message` → `Icons.AutoMirrored`
- Test infrastructure — `unitTests.isReturnDefaultValues = true`
- New tests: EventPipelineTest (8 tests), DataRetentionManagerTest (4 tests)

### Open Items
| Priority | Issue | Effort |
|---|---|---|
| Low | Migrate deprecated WiFi APIs | 2 hours |
| Low | Remove `fallbackToDestructiveMigration()` before v1.0 | 15 min |
| Low | Add Compose UI tests | 2 hours |
| Low | Add Room DAO integration tests | 1 hour |

## Future Roadmap

### Near-term (v0.2) — COMPLETE
1. ~~Share/Export button for session data (CSV/JSON via Android Share sheet)~~
2. ~~Session picker for historical session browsing~~
3. ~~Aruba parser (AOS-8 Mobility Controller syslog)~~

### Medium-term (v0.3) — IN PROGRESS
4. ~~Meraki parser (Meraki MR syslog format)~~
5. ~~RSSI trend smoothing (EMA, alpha=0.3)~~
6. Channel utilization estimation (from scan results)
7. AP association mapping (which clients are on which APs)
8. Migrate to non-deprecated WiFi APIs

### Long-term (v1.0)
12. Ruckus parser
13. Juniper Mist parser
14. Signal history persistence (RSSI over time, persisted to Room)
15. Roaming event correlation (link disassoc → reassoc events)
16. On-device ML for anomaly detection (TFLite)
17. Export to Ekahau / iBwave formats
18. Root-only features: raw 802.11 frame capture, txpower control

## Last Session

**Date:** 2026-03-10

### Previous Work
Full engineering audit and documentation improvement pass. Analyzed all 42 source files and 7 test files. Rewrote HANDOFF.md with comprehensive architecture documentation.

### Current Session
1. Targeted remediation pass for 6 open audit items (Room migration, R8, data retention, tests). See `docs/audits/2026-03-signal-remediation.md`.
2. v0.2 complete: session picker, CSV/JSON export via Share sheet.
3. v0.3 in progress: Aruba parser, Meraki parser, EMA-smoothed RSSI chart.

## Blockers

None — MVP is feature-complete. Remediation pass complete.

## Build Commands

```bash
# Set JAVA_HOME (required — Android Studio's bundled JDK)
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest

# Deploy to connected device
./gradlew installDebug

# Run specific test class
./gradlew testDebugUnitTest --tests "dev.aiaerial.signal.data.parser.CiscoWlcParserTest"

# Full clean build
./gradlew clean assembleDebug

# View APK size
ls -lh app/build/outputs/apk/debug/app-debug.apk
```

## Git Repository

**Remote:** https://github.com/bgorzelic/signal-app
**Branch:** main
