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

**Why this is needed:** Gradle uses whatever JDK `JAVA_HOME` points to. If it finds your system Java (or none), the build will fail with cryptic errors about class versions or missing tools. Android Studio's bundled JDK (JBR — JetBrains Runtime) is tested against the Android Gradle Plugin version used by this project.

### 3. Verify the build

```bash
./gradlew assembleDebug
```

This downloads dependencies (first run takes a few minutes), compiles Kotlin, runs KSP annotation processors (Hilt, Room), and produces an APK at `app/build/outputs/apk/debug/app-debug.apk`.

**If the build fails:**
- Check `JAVA_HOME` is set correctly
- Try `./gradlew clean assembleDebug` to clear stale build artifacts
- Ensure Android Studio has downloaded SDK 36 (check SDK Manager)

## Understanding the Build System

### What is Gradle?

Gradle is the build system. It compiles your Kotlin code, processes resources, runs annotation processors, and packages everything into an APK. Configuration lives in:

| File | Purpose |
|---|---|
| `build.gradle.kts` (root) | Project-level plugins — declares which Gradle plugins exist |
| `app/build.gradle.kts` | App dependencies, SDK versions, build config |
| `gradle/libs.versions.toml` | Version catalog — all library versions in one place |
| `settings.gradle.kts` | Which modules to include in the build |
| `gradle.properties` | Build performance settings (memory, parallel execution) |

**Why `.kts`?** Kotlin Script — type-safe build files with IDE autocomplete. Older projects use `.gradle` (Groovy) which is dynamically typed.

### What is KSP?

**Kotlin Symbol Processing.** It reads annotations like `@Entity`, `@Dao`, `@HiltViewModel` at compile time and generates implementation code. You write:
```kotlin
@Dao
interface NetworkEventDao {
    @Query("SELECT * FROM network_events WHERE sessionId = :sessionId")
    fun getBySession(sessionId: String): Flow<List<NetworkEvent>>
}
```
KSP generates a class that implements this interface with actual SQLite queries. You never write the implementation manually.

### What is Hilt?

**A dependency injection framework.** Instead of manually creating objects and passing them through constructors, you annotate what you need and Hilt wires it up:

```kotlin
// You write this:
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: SignalPreferences,       // Hilt provides this
    private val openClawClient: OpenClawClient, // and this
) : ViewModel()

// Hilt figures out how to create SignalPreferences and OpenClawClient
// by looking at their @Inject constructors and @Module providers.
```

**The key annotations:**
- `@HiltAndroidApp` on `SignalApplication` — initializes Hilt
- `@AndroidEntryPoint` on `MainActivity` and `SyslogService` — enables injection
- `@HiltViewModel` on ViewModels — lets Hilt create them with dependencies
- `@Inject constructor` on classes — tells Hilt how to construct them
- `@Module` + `@Provides` in `di/` — provides objects Hilt can't construct directly (e.g., `Room.databaseBuilder()`)
- `@Singleton` — only create one instance, share it everywhere

### What is Room?

**An SQLite database wrapper.** You define:
- `@Entity` data classes — become database tables
- `@Dao` interfaces — queries, written as SQL strings with Kotlin method signatures
- `@Database` abstract class — ties entities and DAOs together

Room generates the SQL and connection handling at compile time (via KSP). Queries can return `Flow<List<T>>` which automatically re-emits results when the underlying data changes — this is how the Timeline screen updates in real time when new events are parsed.

### What is Jetpack Compose?

**The modern Android UI toolkit.** Instead of XML layouts, you write Kotlin functions that describe UI:

```kotlin
@Composable
fun Greeting(name: String) {
    Text("Hello, $name!")  // This IS the UI, not a reference to XML
}
```

Compose re-runs ("recomposes") these functions when their inputs change. `StateFlow` + `collectAsState()` bridges the ViewModel to the UI — when the flow emits a new value, the composable recomposes with the new data.

**Key concept — recomposition:** When data changes, Compose doesn't rebuild the entire screen. It only re-runs the composable functions whose inputs changed. This is why we use `items(key = { it.id })` in LazyColumn — Compose can identify which items changed and only update those.

## Project Structure

```
signal-app/
├── app/
│   ├── build.gradle.kts          # App-level build config
│   ├── proguard-rules.pro        # R8 keep rules (empty — R8 disabled)
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml    # App declaration, permissions, services
│       │   ├── java/dev/aiaerial/signal/
│       │   │   ├── SignalApplication.kt   # Hilt entry point
│       │   │   ├── MainActivity.kt        # Single-activity Compose host
│       │   │   ├── data/                  # Data layer (see architecture.md)
│       │   │   ├── service/               # Foreground service
│       │   │   ├── di/                    # Hilt modules
│       │   │   └── ui/                    # Compose screens
│       │   └── res/                       # Android resources (icons, strings, themes)
│       └── test/                          # Unit tests (JVM, no Android needed)
├── docs/                                  # Engineering documentation
├── gradle/
│   ├── libs.versions.toml                 # Version catalog
│   └── wrapper/                           # Gradle wrapper (committed to git)
├── build.gradle.kts                       # Root build file
├── settings.gradle.kts                    # Module declarations
└── HANDOFF.md                             # Engineering handoff document
```

## Running Tests

```bash
# All unit tests
./gradlew testDebugUnitTest

# Specific test class
./gradlew testDebugUnitTest --tests "dev.aiaerial.signal.data.parser.CiscoWlcParserTest"

# With verbose output
./gradlew testDebugUnitTest --info

# Generate HTML test report
./gradlew testDebugUnitTest
# Report at: app/build/reports/tests/testDebugUnitTest/index.html
```

### What's Tested

| Test File | What It Tests | Key Assertions |
|---|---|---|
| `CiscoWlcParserTest` | Cisco syslog event detection and field extraction | Event types, MAC extraction, AP name formats |
| `SyslogMessageTest` | RFC 3164 parsing, priority/facility/severity | Priority math, edge cases, raw preservation |
| `VendorDetectorTest` | Parser routing and unknown-line handling | Correct vendor delegation |
| `NetworkEventTest` | Data model construction | All fields, nullable fields |
| `WifiScanResultTest` | Channel/band derivation from frequency | 2.4/5/6 GHz channels |
| `OpenClawClientTest` | Triage prompt building | Event details in prompt |
| `SessionExporterTest` | CSV/JSON export, field escaping | CSV injection prevention, empty lists |

### What's NOT Tested (and why)

| Component | Why Not | What Would Be Needed |
|---|---|---|
| Compose UI | Requires instrumented tests with `composeTestRule` | `androidTestImplementation("androidx.compose.ui:ui-test-junit4")` |
| Room DAO | Requires Android context | `@RunWith(AndroidJUnit4::class)` with in-memory database |
| SyslogReceiver | Network I/O | Real UDP socket or mocked Ktor |
| SyslogService | Android service lifecycle | Robolectric or instrumented tests |
| EventPipeline | Depends on Room DAO | Mock DAO or in-memory database |

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
# Should show something like: XXXXXXXX    device

# 5. Install and launch
./gradlew installDebug
adb shell am start -n dev.aiaerial.signal/.MainActivity
```

### Wireless ADB (over Tailscale)

If the Pixel is on your Tailscale network:

```bash
# On the phone: Settings → Developer Options → Wireless debugging → Enable
# Note the IP:port shown

# From your Mac:
adb pair <tailscale-ip>:<pairing-port>   # enter the pairing code
adb connect <tailscale-ip>:<debug-port>

# Then deploy as normal:
./gradlew installDebug
```

### Viewing Logs

```bash
# SIGNAL app logs only
adb logcat -s "SyslogService" "SignalApp"

# Crash logs (errors from any app)
adb logcat *:E

# Everything (very noisy — thousands of lines/second)
adb logcat

# Filter to SIGNAL's package
adb logcat --pid=$(adb shell pidof dev.aiaerial.signal)

# Save to file for analysis
adb logcat -d > device_logs.txt
```

## Common Development Tasks

### Adding a new vendor parser

1. Create `app/src/main/java/dev/aiaerial/signal/data/parser/ArubaParser.kt`
2. Implement the `VendorParser` interface:
   ```kotlin
   class ArubaParser : VendorParser {
       override fun canParse(line: String): Boolean {
           // Check for Aruba-specific keywords
       }
       override fun parse(line: String, sessionId: String): NetworkEvent? {
           // Extract fields using regex
       }
   }
   ```
3. Add it to `VendorDetector`'s parser list:
   ```kotlin
   private val parsers: List<VendorParser> = listOf(
       CiscoWlcParser(),
       ArubaParser(),  // ← add here
   )
   ```
4. Write tests in `app/src/test/.../parser/ArubaParserTest.kt` using real syslog samples

### Adding a new screen

1. Create the ViewModel: `ui/newscreen/NewViewModel.kt`
   - Extend `ViewModel()`, annotate with `@HiltViewModel`, inject dependencies
2. Create the Composable: `ui/newscreen/NewScreen.kt`
   - Use `hiltViewModel()` to get the ViewModel, `collectAsState()` for data
3. Add a route in `SignalNavHost.kt`:
   ```kotlin
   @Serializable object NewRoute
   ```
4. Add to the `NavHost`:
   ```kotlin
   composable<NewRoute> { NewScreen() }
   ```
5. If it's a tab, add to `topLevelRoutes`:
   ```kotlin
   TopLevelRoute("New", NewRoute, Icons.Outlined.SomeIcon),
   ```

### Adding a new Room entity

1. Define the `@Entity` data class in `data/model/`
2. Create a `@Dao` interface in `data/local/`
3. Add it to `SignalDatabase`'s entities array:
   ```kotlin
   @Database(entities = [NetworkEvent::class, NewEntity::class], version = 2)
   ```
4. **Increment the database version** to 2
5. During development: `fallbackToDestructiveMigration()` wipes and recreates the DB
6. For production: write a `Migration(1, 2)` object with ALTER TABLE SQL

### Modifying the OpenClaw client

The client is at `data/openclaw/OpenClawClient.kt`. Key rules:
- Always use `response.use { }` to close the response body (prevents connection leaks)
- Always rethrow `CancellationException` in catch blocks (structured concurrency)
- The `baseUrl` is `@Volatile` because multiple threads may read it
- Chat requests use OkHttp's synchronous `execute()` inside `withContext(Dispatchers.IO)`

## Troubleshooting

### Build Issues

| Symptom | Cause | Fix |
|---|---|---|
| "Could not determine java version" | `JAVA_HOME` not set or wrong | `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` |
| KSP errors about generated code | Stale build artifacts | `./gradlew clean assembleDebug` |
| "No connected devices" | USB not recognized | Check cable, re-authorize USB debugging, try different USB port |
| Dependency resolution failure | Network issue or version conflict | Check internet, look at error for specific library |

### Runtime Issues

| Symptom | Likely Cause | Debug Steps |
|---|---|---|
| App crashes on launch | Hilt or Room misconfiguration | `adb logcat *:E` — look for `RuntimeException` |
| "Location permission required" loop | Permission denied permanently | Settings > Apps > SIGNAL > Permissions > Location > Allow |
| Syslog messages not appearing | Network reachability | 1) Check notification bar for service 2) `ping <phone-ip>` 3) `nc -u <phone-ip> 1514` |
| OpenClaw shows "Disconnected" | Termux/OpenClaw not running | SSH to phone, check `ps`, start `npx openclaw gateway start` |
| Room crash after schema change | Version not incremented | Clear app data or increment DB version |
| Blank RSSI chart | No WiFi connection | Connect to a WiFi network first |
