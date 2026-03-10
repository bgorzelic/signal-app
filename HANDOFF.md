# SIGNAL — Engineering Handoff Document

## Project Overview

SIGNAL is a native Android wireless network diagnostics tool built for field engineers. It runs on a dedicated Pixel 10a with root access, Termux, and OpenClaw for on-device AI. The app captures syslog from wireless LAN controllers (WLCs), parses vendor-specific events, visualizes client roaming timelines, and provides AI-powered triage — all without cloud dependencies.

**Organization:** AI Aerial Solutions
**Package:** `dev.aiaerial.signal`
**Target Device:** Pixel 10a, Android 16, rooted
**Version:** 0.1.0-dev (MVP)

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        SIGNAL App                            │
│                                                              │
│  ┌─────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐ │
│  │ Scanner │  │  Syslog  │  │ Timeline │  │   Settings   │ │
│  │ Screen  │  │  Screen  │  │  Screen  │  │   Screen     │ │
│  └────┬────┘  └────┬─────┘  └────┬─────┘  └──────┬───────┘ │
│       │             │             │                │         │
│  ┌────┴────┐  ┌─────┴─────┐  ┌───┴────┐   ┌──────┴───────┐│
│  │Scanner  │  │ Syslog    │  │Timeline│   │ Settings     ││
│  │ViewModel│  │ ViewModel │  │ViewModel│  │ ViewModel    ││
│  └────┬────┘  └─────┬─────┘  └───┬────┘   └──────┬───────┘│
│       │             │             │                │         │
│  ┌────┴────┐  ┌─────┴─────┐      │         ┌──────┴───────┐│
│  │  WiFi   │  │  Syslog   │      │         │  Signal      ││
│  │ Scanner │  │  Service  │      │         │ Preferences  ││
│  └─────────┘  └─────┬─────┘      │         └──────────────┘│
│                     │             │                          │
│               ┌─────┴─────┐      │                          │
│               │  Syslog   │      │                          │
│               │ Receiver  │      │                          │
│               │ (UDP:1514)│      │                          │
│               └─────┬─────┘      │                          │
│                     │             │                          │
│               ┌─────┴─────────────┴────────────────┐        │
│               │         Event Pipeline             │        │
│               │  VendorDetector → CiscoWlcParser   │        │
│               └─────────────┬──────────────────────┘        │
│                             │                                │
│               ┌─────────────┴──────────────────────┐        │
│               │        Room Database               │        │
│               │   NetworkEvent (network_events)     │        │
│               └─────────────┬──────────────────────┘        │
│                             │                                │
│               ┌─────────────┴──────────────────────┐        │
│               │      OpenClaw Client (OkHttp)      │        │
│               │   localhost:18789 → Termux/OpenClaw │        │
│               └────────────────────────────────────┘        │
│                                                              │
└──────────────────────────────────────────────────────────────┘
                            │
                    ┌───────┴───────┐
                    │   Pixel 10a   │
                    │  ┌──────────┐ │
                    │  │  Termux  │ │
                    │  │ OpenClaw │ │
                    │  │ Gateway  │ │
                    │  └──────────┘ │
                    │  ┌──────────┐ │
                    │  │Tailscale │ │
                    │  └──────────┘ │
                    └───────────────┘
```

## Data Flow

### Syslog Ingestion Path
```
WLC (UDP syslog) → SyslogReceiver (Ktor UDP :1514)
                  → SyslogMessage.parse() (RFC 3164)
                  → SharedFlow emission
                  → SyslogService collector
                  → EventPipeline.processSyslogMessage()
                  → VendorDetector.parse()
                  → CiscoWlcParser (regex extraction)
                  → NetworkEvent created
                  → NetworkEventDao.insert()
                  → Room DB (network_events table)
```

### WiFi Scanning Path
```
WifiManager.startScan() → BroadcastReceiver
                        → callbackFlow emission
                        → ScannerViewModel (sorted by RSSI)
                        → ScannerScreen (cards + chart)
```

### AI Triage Path
```
User taps event → TriageBottomSheet
               → OpenClawClient.triageEvent()
               → OkHttp POST to localhost:18789/api/v1/chat
               → OpenClaw Gateway (Termux)
               → OpenRouter → Claude Haiku
               → Response displayed in bottom sheet
```

### Log Import Path
```
User pastes WLC debug output → LogImportViewModel.parseLog()
                              → EventPipeline.processLogBlock()
                              → VendorDetector parses each line
                              → Batch insert to Room DB
```

## Module Structure

```
app/src/main/java/dev/aiaerial/signal/
├── SignalApplication.kt          # @HiltAndroidApp entry point
├── MainActivity.kt               # Single-activity Compose host
│
├── data/                          # Data layer (no Android UI deps)
│   ├── model/
│   │   ├── NetworkEvent.kt        # Room @Entity — core data model
│   │   ├── EventType.kt           # Enum: ROAM, AUTH, DEAUTH, ASSOC, etc.
│   │   └── Vendor.kt              # Enum: CISCO, ARUBA, MERAKI, etc.
│   ├── local/
│   │   ├── SignalDatabase.kt      # Room database (v1)
│   │   ├── NetworkEventDao.kt     # DAO with Flow-based reactive queries
│   │   └── Converters.kt          # Room TypeConverters for enums
│   ├── syslog/
│   │   ├── SyslogReceiver.kt      # Ktor UDP socket listener
│   │   └── SyslogMessage.kt       # RFC 3164 parser + data class
│   ├── parser/
│   │   ├── VendorParser.kt        # Interface (strategy pattern)
│   │   ├── CiscoWlcParser.kt      # Cisco 9800/AireOS regex parser
│   │   └── VendorDetector.kt      # Routes to correct parser
│   ├── openclaw/
│   │   ├── OpenClawClient.kt      # OkHttp REST client for AI
│   │   └── OpenClawStatus.kt      # Enum: CONNECTED, DISCONNECTED, CHECKING
│   ├── prefs/
│   │   └── SignalPreferences.kt   # SharedPreferences wrapper
│   └── export/
│       └── SessionExporter.kt     # CSV/JSON export utility
│
├── service/
│   └── SyslogService.kt           # Foreground service for UDP listener
│
├── di/                             # Hilt dependency injection modules
│   ├── DatabaseModule.kt           # Room database + DAO
│   ├── NetworkModule.kt            # OkHttpClient
│   └── WifiModule.kt              # WifiManager
│
└── ui/                             # Compose UI layer
    ├── navigation/
    │   └── SignalNavHost.kt        # Bottom nav + type-safe routes
    ├── scanner/
    │   ├── ScannerScreen.kt        # WiFi scan results list
    │   ├── ScannerViewModel.kt     # Scan polling + RSSI history
    │   ├── SignalChart.kt          # Canvas RSSI chart
    │   └── WifiNetworkCard.kt      # Per-network card
    ├── syslog/
    │   ├── SyslogScreen.kt         # Live syslog feed
    │   └── SyslogViewModel.kt      # Service binding + filtering
    ├── timeline/
    │   ├── TimelineScreen.kt       # Client MAC selector + event list
    │   ├── TimelineViewModel.kt    # flatMapLatest on selected client
    │   └── RoamingTimelineCard.kt  # Canvas timeline visualization
    ├── settings/
    │   ├── SettingsScreen.kt       # OpenClaw config + setup wizard
    │   └── SettingsViewModel.kt    # Health check + preferences
    ├── logimport/
    │   ├── LogImportScreen.kt      # Paste area + parse/analyze
    │   └── LogImportViewModel.kt   # Parse + AI analysis
    ├── triage/
    │   └── TriageBottomSheet.kt    # AI triage modal
    └── theme/
        └── Theme.kt               # Material 3 theme
```

## Runtime Services

### SyslogService (Foreground Service)

**What it does:** Runs a UDP socket listener on port 1514, receives syslog datagrams from WLCs, parses them, and stores parsed events in Room.

**Why it's a foreground service:** Android kills background processes aggressively. A foreground service with a persistent notification keeps the UDP listener alive while you walk around a building doing wireless surveys.

**How it works:**
1. `SyslogViewModel.startListening()` calls `startForegroundService()` + `bindService()`
2. Service creates a notification channel and shows a persistent notification
3. `SyslogReceiver` opens a UDP socket via Ktor and loops on `socket.receive()`
4. Each datagram is parsed into a `SyslogMessage` and emitted via `SharedFlow`
5. A collector in the service feeds messages through `EventPipeline` to Room
6. The ViewModel binds to the service and collects the same `SharedFlow` for UI display

**Foreground service type:** `specialUse` (required on API 34+). This is the correct type for network monitoring tools that don't fit standard categories.

**Lifecycle:** `START_STICKY` means Android will restart the service if it's killed. The `started` flag prevents duplicate coroutine launches on re-delivery.

## OpenClaw Integration

OpenClaw is an AI gateway that runs locally in Termux on the same phone. It proxies requests to Claude Haiku via OpenRouter.

**Architecture:**
```
SIGNAL App
  └→ OkHttpClient
     └→ POST http://127.0.0.1:18789/api/v1/chat
        └→ OpenClaw Gateway (Termux process)
           └→ OpenRouter API
              └→ anthropic/claude-3.5-haiku
```

**Endpoints used:**
- `GET /` — health check (is OpenClaw running?)
- `POST /api/v1/chat` — chat completion (triage + log analysis)

**Error handling:**
- 5-second connect timeout, 30-second read timeout
- Health check returns `DISCONNECTED` on `IOException`
- Chat errors return `"OpenClaw error: HTTP {code}"` or `"Error: {message}"`
- `CancellationException` is always rethrown (structured concurrency)

**Offline behavior:** The app works without OpenClaw — scanning, syslog capture, parsing, and timeline all function. AI features (triage, log analysis) show error messages when OpenClaw is unavailable.

## Networking Model

The app has two distinct network paths:

1. **UDP Syslog (inbound):** Ktor raw socket listening on port 1514. WLCs on the same network send syslog datagrams to the phone's IP. This requires the phone to be reachable — Tailscale provides stable IP addressing.

2. **HTTP to OpenClaw (localhost):** OkHttp talking to `127.0.0.1:18789`. This never leaves the device — it's inter-process communication between the SIGNAL app and the Termux OpenClaw process.

**Port 1514 (not 514):** Standard syslog uses port 514, but binding below 1024 requires root on Linux/Android. Port 1514 is the conventional unprivileged alternative. Configure your WLC to send syslog to `<phone-ip>:1514`.

## Permissions Model

| Permission | Why | When Requested |
|---|---|---|
| `INTERNET` | OpenClaw HTTP calls | Auto-granted (normal) |
| `ACCESS_NETWORK_STATE` | Network connectivity checks | Auto-granted (normal) |
| `ACCESS_WIFI_STATE` | Read WiFi scan results | Auto-granted (normal) |
| `CHANGE_WIFI_STATE` | Trigger WiFi scans | Auto-granted (normal) |
| `ACCESS_FINE_LOCATION` | Required by Android to return WiFi scan results | Runtime prompt on Scanner tab |
| `ACCESS_COARSE_LOCATION` | Fallback for location | Runtime (bundled with fine) |
| `FOREGROUND_SERVICE` | Keep syslog listener alive | Auto-granted (normal) |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Required on API 34+ for specialUse type | Auto-granted (normal) |
| `POST_NOTIFICATIONS` | Show foreground service notification | Should be runtime on API 33+ (TODO) |

**Important:** `ACCESS_FINE_LOCATION` is not optional. Android requires it to return WiFi scan results, even though SIGNAL doesn't use GPS. This is an Android platform requirement, not a SIGNAL design choice.

## Known Limitations

1. **Cisco-only parsing.** Only `CiscoWlcParser` exists. Aruba, Meraki, Ruckus, and Juniper syslog formats are not yet handled.
2. **No export UI.** `SessionExporter` exists with `toCsv()`/`toJson()` but there's no Share button in the UI yet.
3. **No POST_NOTIFICATIONS runtime request.** On API 33+, the notification permission should be requested at runtime before starting the foreground service.
4. **No database migration strategy.** Room is at version 1 with `exportSchema = false`. Schema changes will crash on existing installs.
5. **SyslogReceiver port is hardcoded to 1514.** `SignalPreferences.syslogPort` exists but isn't wired to `SyslogReceiver`.
6. **No R8/ProGuard.** Minification is disabled. APK is larger than necessary.
7. **No session persistence across app restarts.** `EventPipeline.currentSessionId` is a UUID generated at app start. Previous session data exists in Room but there's no session picker UI.
8. **WiFi scanning uses deprecated APIs.** `WifiManager.startScan()` and `WifiManager.connectionInfo` are deprecated. They still work but may be removed.

## Performance Considerations

- **Syslog message buffer:** `SyslogViewModel` keeps up to 5000 messages in memory (`synchronizedList`). At ~200 bytes per message, this is ~1MB. Acceptable for field use.
- **Room indices:** `NetworkEvent` has indices on `[sessionId]`, `[sessionId, eventType]`, and `[sessionId, clientMac]`. These cover all query patterns used by the DAO.
- **SharedFlow buffer:** `SyslogReceiver` uses `extraBufferCapacity = 256`. If the pipeline can't keep up with bursty syslog (>256 messages queued), messages will be dropped (tryEmit behavior).
- **WiFi poll interval:** 2 seconds. Each poll calls `wifiManager.connectionInfo` which is a Binder IPC call.
- **RSSI history:** Capped at 60 data points (2 minutes at 2s interval). No persistence.

## Last Session

**Date:** 2026-03-09

Built the complete MVP (12 tasks) using Subagent-Driven Development. Every task went through two-stage code review (spec compliance + code quality). Quality fixes applied for CSV injection, CancellationException handling, OkHttp response leaks, Paint color rendering, channelWidth display, permission handling, health check race conditions, and draft URL state.

## Next Steps

1. Install on Pixel 10a and test with live WLC syslog
2. Test OpenClaw connectivity (Termux gateway must be running)
3. Add POST_NOTIFICATIONS runtime permission request
4. Wire `SignalPreferences.syslogPort` to `SyslogReceiver`
5. Add Share/Export button to UI
6. Add Aruba/Meraki vendor parsers
7. Add session picker for historical data
8. Enable R8 with proper keep rules
9. Add Room migration strategy

## Blockers

None — MVP is feature-complete and pushed to GitHub.

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
```

## Git Repository

**Remote:** https://github.com/bgorzelic/signal-app
**Branch:** main
