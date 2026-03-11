# SIGNAL — WiFi Network Intelligence for Android

Android app that transforms a Pixel 10a into a wireless diagnostic sensor. Captures syslog events from enterprise WiFi controllers (Cisco WLC, Aruba, Meraki), parses them into structured network events, and displays real-time roaming timelines with AI-powered triage via OpenClaw.

## Quick Start

### Prerequisites

- Android Studio (Ladybug or later)
- JDK 11+
- Android device or emulator (API 29+, targeting API 36)

### Build

```bash
# Clone and open in Android Studio
git clone <repo-url>
cd signal-app

# Build debug APK
./gradlew assembleDebug

# Run tests
./gradlew test

# Install on connected device
./gradlew installDebug
```

Or from the workspace root:

```bash
just build-signal     # Build debug APK
just install-signal   # Build + install on device via ADB
just run-signal       # Launch on device
```

## Architecture

```
WiFi Controller (Cisco/Aruba/Meraki)
  └─ syslog UDP:1514 ──→ SyslogReceiver
                              └─ VendorDetector → Parser (Cisco/Aruba/Meraki)
                                    └─ EventPipeline → Room DB
                                          ├─ Timeline UI (roaming events, AP maps)
                                          ├─ Scanner UI (live WiFi scan, channel utilization)
                                          └─ OpenClawClient → HTTP POST → OpenClaw Gateway
                                                                └─ AI triage response
```

## Project Structure

```
signal-app/
├── app/
│   └── src/main/java/dev/aiaerial/signal/
│       ├── data/
│       │   ├── local/          # Room database, DAOs, converters
│       │   ├── model/          # NetworkEvent, ApAssociation, EventType
│       │   ├── parser/         # Vendor-specific syslog parsers
│       │   ├── syslog/         # UDP syslog receiver
│       │   ├── wifi/           # WiFi scanner, channel utilization
│       │   ├── openclaw/       # OpenClaw gateway client
│       │   ├── export/         # Session CSV export
│       │   ├── prefs/          # SharedPreferences wrapper
│       │   ├── retention/      # Data retention manager
│       │   └── EventPipeline.kt
│       ├── di/                 # Hilt dependency injection modules
│       ├── service/            # SyslogService (foreground service)
│       ├── ui/
│       │   ├── scanner/        # WiFi scanner screen + charts
│       │   ├── timeline/       # Roaming timeline + AP map cards
│       │   ├── syslog/         # Raw syslog viewer
│       │   ├── triage/         # AI triage bottom sheet
│       │   ├── logimport/      # Log file import
│       │   ├── settings/       # App settings
│       │   ├── navigation/     # Compose NavHost
│       │   └── theme/          # Material 3 theme
│       ├── MainActivity.kt
│       └── SignalApplication.kt
├── docs/                       # Architecture, development, audit docs
├── testdata/                   # Sample syslog data for testing
├── build.gradle.kts            # Root build config
└── settings.gradle.kts         # Project settings
```

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| DI | Hilt |
| Database | Room (SQLite) |
| Networking | Ktor (UDP syslog), OkHttp (HTTP) |
| Serialization | kotlinx.serialization |
| Build | Gradle (Kotlin DSL) |
| Min SDK | 29 (Android 10) |
| Target SDK | 36 |

## Key Commands

```bash
./gradlew assembleDebug        # Build debug APK
./gradlew assembleRelease      # Build release APK (requires signing config)
./gradlew test                 # Run unit tests
./gradlew connectedAndroidTest # Run instrumented tests on device
./gradlew lint                 # Run Android lint
```

## Supported WiFi Controllers

- **Cisco WLC** — Association, roaming, and deauth syslog events
- **Aruba** — Client association and roaming events
- **Meraki** — Event log parsing

## Related Projects

- [OpenClaw](https://openclaw.ai) — AI gateway that processes triage requests from SIGNAL
- [openclaw-pixel10a-guide](https://github.com/bgorzelic/openclaw-android-edge) — Deployment guide for the Pixel 10a edge node

## Status

Active development. Version 0.3.0.
