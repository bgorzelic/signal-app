# SIGNAL Device Setup Guide

This guide covers setting up a Pixel 10a as a dedicated wireless engineering tool running SIGNAL with on-device AI.

## What You're Setting Up

```
┌──────────────────────────────────────────────────────────────┐
│                        Pixel 10a                              │
│                                                               │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  SIGNAL App (Android)                                    │ │
│  │  - WiFi scanning + signal analysis                       │ │
│  │  - UDP syslog listener (port 1514)                      │ │
│  │  - Cisco WLC event parser                                │ │
│  │  - Client roaming timeline                               │ │
│  │  - AI event triage (via OpenClaw)                        │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                               │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  Termux (Linux environment on Android)                   │ │
│  │  └── OpenClaw Gateway (Node.js)                          │ │
│  │      └── Proxies to OpenRouter → Claude 3.5 Haiku        │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                               │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  Tailscale (VPN mesh)                                    │ │
│  │  - Stable IP for syslog ingestion                        │ │
│  │  - SSH access from dev machine                           │ │
│  └─────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────┘
```

## Device Requirements

| Requirement | Details |
|---|---|
| **Phone** | Google Pixel 10a (or any Android 10+ device) |
| **Android** | 16 (API 36) — minimum supported is Android 10 (API 29) |
| **Root** | Optional but recommended for advanced features |
| **Storage** | ~500MB free (SIGNAL app + Termux + OpenClaw + Node.js) |
| **Network** | WiFi for scanning, Tailscale for syslog ingestion |

## Step 1: Install SIGNAL

### Option A: From USB (development builds)

```bash
# On your Mac, with the phone connected via USB:
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
cd ~/dev/projects/signal-app
./gradlew installDebug
```

### Option B: From APK (sideload)

```bash
# Build the APK on your Mac:
./gradlew assembleDebug

# Transfer and install:
adb install app/build/outputs/apk/debug/app-debug.apk
```

### First Launch

1. Tap the SIGNAL icon
2. You'll see a 4-tab bottom navigation: Scanner, Syslog, Timeline, Settings
3. The Scanner tab will immediately request **Location Permission**

## Step 2: Grant Permissions

### Location Permission (required)

SIGNAL needs `ACCESS_FINE_LOCATION` to read WiFi scan results. **This is an Android platform requirement** — the OS blocks WiFi scan data without location permission, regardless of why you want it. SIGNAL doesn't use GPS or track your location.

If you denied the permission:
**Settings > Apps > SIGNAL > Permissions > Location > Allow all the time**

**Why "all the time"?** If you choose "Only while using the app," WiFi scanning stops when the screen is off. For a field tool that you carry in a pocket, "all the time" ensures continuous monitoring.

### Notification Permission (API 33+)

When you tap "Start Listening" on the Syslog tab, SIGNAL will request notification permission. This is needed to show the foreground service notification (the persistent "Listening on UDP port 1514" notification that keeps the syslog listener alive).

If denied, the syslog listener still works, but the notification is invisible.

## Step 3: Install Termux (for AI features)

OpenClaw provides on-device AI triage. It runs inside Termux, which provides a Linux environment on Android.

**SIGNAL works without Termux/OpenClaw** — WiFi scanning, syslog capture, event parsing, and timeline visualization all function. Only AI features (triage, log analysis) require OpenClaw.

### Install Termux

1. Download from **F-Droid** (not Google Play — the Play Store version is outdated and broken)
2. F-Droid URL: `https://f-droid.org/en/packages/com.termux/`
3. You may need to enable "Install from unknown sources" for F-Droid

### Install OpenClaw in Termux

Open Termux and run:

```bash
# Update package manager
pkg update && pkg upgrade

# Install Node.js (required by OpenClaw)
pkg install nodejs-lts

# Initialize OpenClaw (downloads the gateway)
npx openclaw@latest init

# Start the gateway
npx openclaw gateway start

# Verify it's running (should return JSON with version info):
curl http://127.0.0.1:18789/
```

**If `curl` returns JSON, OpenClaw is ready.** Go to SIGNAL's Settings tab — it should show "Connected".

### Configure OpenClaw Auto-Start

Install **Termux:Boot** from F-Droid to auto-start OpenClaw when the phone boots:

```bash
# Create the boot script directory
mkdir -p ~/.termux/boot

# Create the startup script
cat > ~/.termux/boot/start-openclaw.sh << 'EOF'
#!/data/data/com.termux/files/usr/bin/sh
# Wait for network
sleep 10
npx openclaw gateway start
EOF

chmod +x ~/.termux/boot/start-openclaw.sh
```

### Battery Optimization Exemptions

**Critical:** Android will kill Termux and OpenClaw within minutes unless exempted from battery optimization.

For each app, go to:
**Settings > Apps > [App Name] > Battery > Unrestricted**

| App | Must Be Unrestricted | Why |
|---|---|---|
| Termux | Yes | OpenClaw gateway runs here |
| Termux:Boot | Yes | Starts OpenClaw on boot |
| Tailscale | Yes | Maintains VPN connection |
| SIGNAL | Recommended | Keeps foreground service priority higher |

Verify exemptions via ADB:
```bash
adb shell dumpsys deviceidle whitelist
# Should include com.termux, com.termux.boot, com.tailscale.ipn
```

## Step 4: Install Tailscale (for network access)

Tailscale provides a stable, routable IP address for syslog ingestion. Without it, the phone's IP changes when moving between networks, and WLC syslog won't reach it.

### Install and Configure

1. Install from Google Play: search "Tailscale"
2. Log in with your Tailscale account
3. Note the phone's Tailscale IP (e.g., `100.x.y.z`)

### Set Tailscale as Always-On VPN (recommended)

This ensures the phone always has a reachable IP, even after reboot:

```bash
# Via ADB from your Mac:
adb shell settings put secure always_on_vpn_app com.tailscale.ipn
adb shell settings put secure always_on_vpn_lockdown 0
```

**`lockdown 0`** means: use Tailscale when available, but allow traffic if Tailscale disconnects. Setting it to `1` would block all traffic without Tailscale — don't do that on a field device.

### Verify Connectivity

From your Mac (also on Tailscale):
```bash
# Ping the phone
ping <phone-tailscale-ip>

# Test SSH (if Termux SSH is set up)
ssh termux

# Test syslog port
echo "test" | nc -u <phone-tailscale-ip> 1514
```

## Step 5: Configure Your WLC

Point your wireless controller's syslog output to the phone's Tailscale IP on port 1514.

### Cisco 9800 / IOS-XE
```
configure terminal
logging host <phone-tailscale-ip> transport udp port 1514
logging trap debugging
end
write memory
```

### Cisco AireOS (older WLC)
```
config logging syslog host <phone-tailscale-ip> 1514
config logging syslog level debugging
save config
```

### Cisco Catalyst (IOS)
```
configure terminal
logging host <phone-tailscale-ip> transport udp port 1514
logging trap 7
end
```

### Testing Without a WLC

You can send fake syslog messages from any machine on the same Tailscale network:

```bash
# Single Cisco-format message:
echo '<134>Mar 9 12:00:00 wlc: *apfMsConnTask: apf_ms_connect.c:1234 00:11:22:33:44:55 Association received on AP AP-Floor3-East slot 1' | nc -u <phone-ip> 1514

# Simulate roaming (10 events):
for i in $(seq 1 10); do
  echo "<134>Mar 9 12:00:0${i} wlc: *apfMsConnTask: client AA:BB:CC:DD:EE:FF roamed to AP AP-East-${i}" | nc -u <phone-ip> 1514
  sleep 0.5
done

# Simulate deauth:
echo '<27>Mar 9 12:05:00 wlc: %DOT11-4-DEAUTH: Station AA:BB:CC:DD:EE:FF Deauthenticated reason 8' | nc -u <phone-ip> 1514
```

## Step 6: Verify Everything Works

### Test Checklist

| # | Feature | How to Test | Expected Result |
|---|---|---|---|
| 1 | App launches | Tap SIGNAL icon | 4-tab bottom nav appears |
| 2 | WiFi scanning | Scanner tab, grant location permission | Nearby networks listed with signal strength |
| 3 | Connection info | Connect to WiFi, go to Scanner | "Connected to [SSID]" card with RSSI |
| 4 | RSSI chart | Stay on Scanner for 10 seconds | Line chart appears showing RSSI over time |
| 5 | Syslog receiver | Syslog tab → "Start Listening" | "UDP :1514 active", notification appears |
| 6 | Syslog ingestion | Send test syslog from Mac | Messages appear in real time |
| 7 | Event parsing | Send Cisco-format syslog | "parsed" counter increments |
| 8 | Timeline | Timeline tab (after parsing events) | Client MACs appear in dropdown |
| 9 | Roaming cards | Select a client MAC | Colored timeline cards with event details |
| 10 | OpenClaw health | Settings tab | "Connected" (green) or "Disconnected" (red) |
| 11 | AI triage | Tap a parsed event | AI analysis appears in bottom sheet |
| 12 | Log import | Syslog → Import, paste WLC output | Parsed event count shown |
| 13 | AI log analysis | Import → "AI Analysis" button | Analysis text appears in card |

## Network Topology

```
┌──────────────────┐     syslog (UDP:1514)      ┌───────────────────┐
│    Wireless       │ ──────────────────────────→ │    Pixel 10a      │
│    Controller     │     via Tailscale or LAN    │                   │
│    (WLC)          │                             │  SIGNAL app       │
└──────────────────┘                             │  Termux/OpenClaw  │
                                                  │  Tailscale        │
┌──────────────────┐     Tailscale mesh          │                   │
│    Your Mac       │ ←─────────────────────────→ │                   │
│    (dev/debug)    │     SSH / ADB / deploy      └───────────────────┘
└──────────────────┘

                         ┌─────────────────┐
                         │   OpenRouter    │
                         │   (internet)    │     Only for AI features
OpenClaw ───────────────→│   Claude 3.5    │     (not required for
(on Pixel, in Termux)    │   Haiku         │      core functionality)
                         └─────────────────┘
```

## Troubleshooting

### Syslog Not Appearing

| Step | Check | Fix |
|---|---|---|
| 1 | Is the service running? | Look for SIGNAL notification in status bar |
| 2 | Is the phone reachable? | `ping <phone-ip>` from sending machine |
| 3 | Is the port correct? | `echo "test" \| nc -u <phone-ip> 1514` |
| 4 | Is Tailscale connected? | Check Tailscale app on phone |
| 5 | Is there a firewall? | Some Android ROMs block incoming UDP |
| 6 | Try a different port | Change in Settings, restart listener |

### OpenClaw Keeps Disconnecting

| Step | Check | Fix |
|---|---|---|
| 1 | Battery optimization | Settings > Apps > Termux > Battery > Unrestricted |
| 2 | Doze exemption | `adb shell dumpsys deviceidle whitelist` should include `com.termux` |
| 3 | Process running | `ssh termux` → `ps aux \| grep openclaw` |
| 4 | OAT cache (after OS update) | `adb shell cmd package compile -m speed -f com.termux` |
| 5 | Restart manually | In Termux: `npx openclaw gateway start` |

### Phone Gets Hot During Extended Use

The syslog listener and WiFi scanner running simultaneously generate heat, especially during active site surveys.

| Mitigation | How |
|---|---|
| Reduce WiFi scan frequency | Currently 2s polling — can be 10s for passive monitoring |
| Dim the screen | Or use Android's "always-on display" at low brightness |
| Phone mount with airflow | Don't keep it in a pocket — use a belt clip or lanyard |
| Pause WiFi scanning | If you only need syslog, stay on the Syslog tab |

### App Crashes

```bash
# Get the crash log immediately after the crash:
adb logcat -d *:E | tail -50

# Common causes:
# - Room schema mismatch: clear app data (Settings > Apps > SIGNAL > Storage > Clear Data)
# - Missing Hilt annotation: check adb logcat for "Hilt" errors
# - OOM on large datasets: check for SIGNAL in `adb shell dumpsys meminfo`
```

## Advanced: Root Features

If your Pixel 10a is rooted, these additional capabilities are possible (not yet implemented):

| Feature | Root Requirement | Potential |
|---|---|---|
| Bind port 514 (standard syslog) | Yes (ports < 1024) | No WLC reconfiguration needed |
| Raw 802.11 frame capture | Yes (monitor mode) | Packet-level debugging |
| TX power adjustment | Yes | Range testing |
| CPU frequency pinning | Yes | Consistent performance during surveys |
| Kernel network tuning | Yes | Larger UDP receive buffers |

## Quick Reference Card

Print this and tape it to the back of the phone:

```
SIGNAL Quick Reference
─────────────────────
Syslog Port:   1514
Tailscale IP:  _____________ (fill in)
OpenClaw:      http://127.0.0.1:18789

Start syslog:  Syslog tab → Start Listening
Start OpenClaw: Termux → npx openclaw gateway start
Test syslog:   echo '<134>...' | nc -u <ip> 1514

WLC Config (9800):
  logging host <ip> transport udp port 1514
  logging trap debugging
```
