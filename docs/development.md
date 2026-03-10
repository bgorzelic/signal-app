# SIGNAL Development Guide

## Prerequisites

- macOS with Apple Silicon (development machine)
- Android Studio (provides Gradle, SDK, and JDK)
- Pixel 10a or Android emulator (API 29+)
- USB cable for device deployment

## Environment Setup

### 1. Clone the repository

```bash
git clone https://github.com/bgorzelic/signal-app.git
cd signal-app
```

### 2. Set JAVA_HOME

Android Studio bundles its own JDK. Gradle requires this environment variable:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

Add this to your `.zprofile` or `.zshrc` to make it permanent.

**Why this is needed:** Gradle uses whatever JDK `JAVA_HOME` points to. If it finds your system Java (or none), the build will fail with cryptic errors about class versions or missing tools.

### 3. Verify the build

```bash
./gradlew assembleDebug
```

This downloads dependencies (first run takes a few minutes), compiles Kotlin, runs KSP annotation processors (Hilt, Room), and produces an APK at `app/build/outputs/apk/debug/app-debug.apk`.

## Project Structure Explained

### What is Gradle?

Gradle is the build system. It compiles your Kotlin code, processes resources, runs annotation processors, and packages everything into an APK. Configuration lives in:

- `build.gradle.kts` (root) — project-level plugins
- `app/build.gradle.kts` — app dependencies, SDK versions, build config
- `gradle/libs.versions.toml` — version catalog (all library versions in one place)
- `settings.gradle.kts` — which modules to include

### What is KSP?

Kotlin Symbol Processing. It reads annotations like `@Entity`, `@Dao`, `@HiltViewModel` at compile time and generates implementation code. You never write the Room database queries or Hilt injection factories manually — KSP does it.

### What is Hilt?

A dependency injection framework. Instead of manually creating objects and passing them through constructors, you annotate what you need and Hilt wires it up:

```kotlin
// You write this:
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: SignalPreferences,    // Hilt provides this
    private val openClawClient: OpenClawClient, // and this
) : ViewModel()

// Hilt figures out how to create SignalPreferences and OpenClawClient
// by looking at their @Inject constructors and @Module providers.
```

### What is Room?

An SQLite database wrapper. You define:
- `@Entity` data classes (database tables)
- `@Dao` interfaces (queries, written as SQL strings)
- `@Database` abstract class (ties them together)

Room generates the SQL and connection handling. Queries can return `Flow<List<T>>` which automatically re-emits results when the underlying data changes.

### What is Jetpack Compose?

The modern Android UI toolkit. Instead of XML layouts, you write Kotlin functions that describe UI:

```kotlin
@Composable
fun Greeting(name: String) {
    Text("Hello, $name!")  // This IS the UI, not a reference to XML
}
```

Compose re-runs (recomposes) these functions when their inputs change. `StateFlow` + `collectAsState()` bridges the ViewModel to the UI — when the flow emits a new value, the composable recomposes.

## Running Tests

```bash
# All unit tests
./gradlew testDebugUnitTest

# Specific test class
./gradlew testDebugUnitTest --tests "dev.aiaerial.signal.data.parser.CiscoWlcParserTest"
```

### What's tested

| Test File | What It Tests |
|---|---|
| `NetworkEventTest` | Data model construction with all/nullable fields |
| `SyslogMessageTest` | RFC 3164 parsing, priority/facility/severity extraction |
| `CiscoWlcParserTest` | Cisco syslog event detection and field extraction |
| `VendorDetectorTest` | Parser routing and unknown-line handling |
| `WifiScanResultTest` | Channel number and band derivation from frequency |
| `OpenClawClientTest` | Triage prompt building |
| `SessionExporterTest` | CSV/JSON export, field escaping, edge cases |

### What's NOT tested (and why)

- **Compose UI:** Requires instrumented tests with `composeTestRule`. Not set up yet.
- **Room DAO:** Requires an Android context. Would need `@RunWith(AndroidJUnit4::class)` with an in-memory database.
- **SyslogReceiver:** Network I/O. Would need to spin up a real UDP socket or mock Ktor.
- **SyslogService:** Android service lifecycle. Requires Robolectric or instrumented tests.

## Deploying to Device

### USB deployment

```bash
# 1. Enable Developer Options on the phone
#    Settings → About phone → tap "Build number" 7 times

# 2. Enable USB Debugging
#    Settings → Developer Options → USB Debugging → ON

# 3. Connect via USB, authorize on the phone when prompted

# 4. Verify device is visible
adb devices

# 5. Install and launch
./gradlew installDebug
adb shell am start -n dev.aiaerial.signal/.MainActivity
```

### Viewing logs from the device

```bash
# All SIGNAL-related logs
adb logcat -s "SyslogService" "SignalApp"

# Everything (very noisy)
adb logcat

# Filter to errors only
adb logcat *:E
```

## Common Tasks

### Adding a new vendor parser

1. Create `app/src/main/java/dev/aiaerial/signal/data/parser/ArubaParser.kt`
2. Implement `VendorParser` interface (`canParse` + `parse`)
3. Add it to `VendorDetector`'s parser list
4. Write tests in `app/src/test/.../parser/ArubaParserTest.kt`

### Adding a new screen

1. Create the ViewModel in `ui/newscreen/NewViewModel.kt`
2. Create the Composable in `ui/newscreen/NewScreen.kt`
3. Add a `@Serializable object NewRoute` in `SignalNavHost.kt`
4. Add a `composable<NewRoute> { NewScreen() }` inside the `NavHost`
5. If it's a tab, add it to `topLevelRoutes`

### Adding a new Room entity

1. Define the `@Entity` data class in `data/model/`
2. Create a `@Dao` interface in `data/local/`
3. Add it to `SignalDatabase`'s `entities` array
4. **Increment the database version** and add a migration, OR use `.fallbackToDestructiveMigration()` during development

### Modifying the OpenClaw client

The client is at `data/openclaw/OpenClawClient.kt`. It uses OkHttp's synchronous `execute()` inside `withContext(Dispatchers.IO)`. Key rules:
- Always use `response.use { }` to close the response body
- Always rethrow `CancellationException` in catch blocks
- The `baseUrl` is `@Volatile` because multiple threads may read it

## Troubleshooting

### Build fails with "Could not determine java version"
Set `JAVA_HOME` to Android Studio's bundled JDK. See Environment Setup above.

### "No connected devices" on installDebug
Run `adb devices`. If empty: check USB cable, re-authorize USB debugging on phone.

### App crashes immediately on launch
Check `adb logcat` for the exception. Common causes:
- Missing Hilt setup (forgot `@AndroidEntryPoint` or `@HiltAndroidApp`)
- Room schema mismatch (added a column without incrementing version)

### Syslog not showing up
1. Is the foreground service running? (Check notification bar)
2. Is the phone reachable? `ping <phone-ip>` from the WLC's network
3. Is port 1514 correct? Test with `echo "test" | nc -u <phone-ip> 1514`
4. Is Tailscale connected? (if routing through Tailscale)

### OpenClaw shows "Disconnected"
1. SSH into the phone: `ssh termux`
2. Check if OpenClaw is running: `ps aux | grep openclaw`
3. Start it: `npx openclaw gateway start`
4. Verify: `curl http://127.0.0.1:18789/`
