# SIGNAL Device Setup Guide

This guide covers setting up a Pixel 10a as a dedicated wireless engineering tool.

## Device Requirements

- **Phone:** Google Pixel 10a (or any Android 14+ device)
- **Android:** 16 (API 36) — minimum supported is Android 10 (API 29)
- **Root:** Optional but recommended for advanced features
- **Storage:** ~500MB free (app + Termux + OpenClaw + models)

## Step 1: Install SIGNAL

### From USB (development)
```bash
# On your Mac, with the phone connected via USB:
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
cd ~/dev/projects/signal-app
./gradlew installDebug
```

### From APK (sideload)
```bash
# Build the APK on your Mac:
./gradlew assembleDebug

# The APK is at:
# app/build/outputs/apk/debug/app-debug.apk

# Transfer to phone and install:
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Step 2: Grant Permissions

On first launch, SIGNAL will request **Location Permission**. You must grant it — Android requires location access to return WiFi scan results. This is an OS-level requirement, not something SIGNAL can work around.

If you denied the permission, go to:
**Settings > Apps > SIGNAL > Permissions > Location > Allow all the time**

## Step 3: Install Termux (for AI features)

OpenClaw runs inside Termux, which provides a Linux environment on Android.

### Install Termux
1. Download from **F-Droid** (not Google Play — the Play version is outdated)
2. URL: `https://f-droid.org/en/packages/com.termux/`
3. Install the APK

### Install OpenClaw in Termux
```bash
# Open Termux and run:
pkg update && pkg upgrade
pkg install nodejs-lts

# Install OpenClaw
npx openclaw@latest init

# Start the gateway
npx openclaw gateway start

# Verify it's running (should return JSON):
curl http://127.0.0.1:18789/
```

### Configure OpenClaw auto-start

Install **Termux:Boot** from F-Droid to auto-start OpenClaw when the phone boots:

```bash
# Create the boot script directory
mkdir -p ~/.termux/boot

# Create the startup script
cat > ~/.termux/boot/start-openclaw.sh << 'EOF'
#!/data/data/com.termux/files/usr/bin/sh
npx openclaw gateway start
EOF

chmod +x ~/.termux/boot/start-openclaw.sh
```

### Battery optimization exceptions

Both Termux and Termux:Boot must be exempt from battery optimization, or Android will kill them:

1. **Settings > Apps > Termux > Battery > Unrestricted**
2. **Settings > Apps > Termux:Boot > Battery > Unrestricted**
3. If using Tailscale: **Settings > Apps > Tailscale > Battery > Unrestricted**

## Step 4: Install Tailscale (for network access)

Tailscale provides a stable IP address and secure connectivity for syslog ingestion.

1. Install from Google Play: **Tailscale**
2. Log in with your Tailscale account
3. Note the phone's Tailscale IP (e.g., `100.x.y.z`)

### Set Tailscale as always-on VPN (recommended)

This ensures the phone always has a reachable IP:

```bash
# Via ADB from your Mac:
adb shell settings put secure always_on_vpn_app com.tailscale.ipn
adb shell settings put secure always_on_vpn_lockdown 0
```

## Step 5: Configure Your WLC

Point your wireless controller's syslog output to the phone:

### Cisco 9800 / IOS-XE
```
logging host <phone-tailscale-ip> transport udp port 1514
logging trap debugging
```

### Cisco AireOS (older WLC)
```
config logging syslog host <phone-tailscale-ip> 1514
config logging syslog level debugging
```

### Alternative: Test without a WLC

From any machine on the same network (or via Tailscale):

```bash
# Send a fake Cisco syslog message:
echo '<134>Mar 9 12:00:00 wlc: *apfMsConnTask: apf_ms_connect.c:1234 00:11:22:33:44:55 Association received on AP AP-Floor3-East slot 1' | nc -u <phone-ip> 1514

# Send multiple messages:
for i in $(seq 1 10); do
  echo "<134>Mar 9 12:00:0${i} wlc: *apfMsConnTask: client AA:BB:CC:DD:EE:FF roamed to AP AP-East-${i}" | nc -u <phone-ip> 1514
  sleep 0.5
done
```

## Step 6: Verify Everything Works

### Test checklist

| Feature | How to Test | Expected Result |
|---|---|---|
| App launches | Tap SIGNAL icon | 4-tab bottom nav appears |
| WiFi scanning | Go to Scanner tab, grant location | Nearby networks listed with signal bars |
| Syslog receiver | Go to Syslog tab, tap "Start Listening" | "UDP :1514 active" shown |
| Syslog ingestion | Send test syslog from Mac | Messages appear in real time |
| Event parsing | Send Cisco-format syslog | "parsed" counter increments |
| Timeline | Go to Timeline tab after parsing events | Client MACs appear in dropdown |
| OpenClaw health | Go to Settings tab | "Connected" or "Disconnected" shown |
| AI triage | Tap a parsed event (if OpenClaw is running) | AI analysis appears in bottom sheet |
| Log import | Go to Syslog > Import, paste WLC output | Parsed event count shown |

## Network Topology

```
┌──────────────┐     syslog (UDP:1514)      ┌───────────────┐
│  Wireless    │ ──────────────────────────→ │  Pixel 10a    │
│  Controller  │     via Tailscale or LAN    │               │
│  (WLC)       │                             │  SIGNAL app   │
└──────────────┘                             │  Termux       │
                                             │  OpenClaw     │
┌──────────────┐     Tailscale mesh          │  Tailscale    │
│  Your Mac    │ ←─────────────────────────→ │               │
│  (dev/debug) │     SSH / ADB              └───────────────┘
└──────────────┘
```

## Troubleshooting

### "UDP :1514 active" but no messages appear

1. **Check network reachability:** From the sending machine, `nc -zvu <phone-ip> 1514`
2. **Check Tailscale:** Is the phone's Tailscale IP reachable? `ping <phone-tailscale-ip>`
3. **Check firewall:** Some Android ROMs or VPN configs block incoming UDP
4. **Try a different port:** If 1514 is blocked, modify `SyslogReceiver` to use another port

### OpenClaw keeps disconnecting

1. **Check Termux is not optimized:** Settings > Apps > Termux > Battery > Unrestricted
2. **Check Doze exemption:** `adb shell dumpsys deviceidle whitelist` should include `com.termux`
3. **OAT cache invalidation after OS updates:** Run `adb shell cmd package compile -m speed -f com.termux`

### Phone gets hot during extended use

The syslog listener and WiFi scanner running simultaneously can generate heat:
1. Reduce WiFi scan frequency (currently 2s — could be 10s for passive monitoring)
2. Dim the screen
3. Use a phone mount with airflow

### App crashes when rotating the phone

ViewModels survive rotation — this shouldn't happen. Check `adb logcat *:E` for the exception. Most likely cause: a Composable is holding state that should be in the ViewModel.
