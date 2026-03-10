# SIGNAL App — Validation Checklist

**Date:** 2026-03-10
**Purpose:** Manual and automated validation steps after the 2026-03 remediation pass

## Automated Validation

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

# 1. All unit tests pass
./gradlew testDebugUnitTest

# 2. Debug build succeeds
./gradlew assembleDebug

# 3. Release build succeeds (R8 enabled)
./gradlew assembleRelease

# 4. Verify R8 APK size reduction
ls -lh app/build/outputs/apk/release/app-release-unsigned.apk
# Expected: ~5MB (was 63MB before R8)

# 5. Verify Room schema export
ls app/schemas/dev.aiaerial.signal.data.local.SignalDatabase/
# Expected: 1.json
```

## On-Device Validation (Pixel 10a)

### Core Functionality
- [ ] App launches without crash
- [ ] Scanner tab: WiFi scan results appear (requires location permission)
- [ ] Scanner tab: RSSI chart updates when connected
- [ ] Syslog tab: Start/stop listening toggles foreground service
- [ ] Syslog tab: Notification appears when service is running
- [ ] Syslog tab: Messages appear when UDP datagrams are sent to phone
- [ ] Timeline tab: Events appear after syslog capture
- [ ] Settings tab: OpenClaw health check works (when Termux is running)
- [ ] Settings tab: Syslog port changes are respected on next service start

### Data Retention
- [ ] App starts without errors (retention cleanup runs silently)
- [ ] After 30+ days of data: old events are cleaned up on app start
- [ ] Setting retention to 0 disables auto-cleanup
- [ ] Check `adb logcat -s DataRetention` for cleanup log messages

### R8/ProGuard (Release Build)
- [ ] Install release APK: `adb install app/build/outputs/apk/release/app-release-unsigned.apk`
- [ ] All screens render correctly (no missing classes)
- [ ] Navigation works between all tabs
- [ ] Syslog service starts and receives messages
- [ ] AI triage bottom sheet works (OpenClaw connected)
- [ ] Log import parses and displays events
- [ ] Export functionality works
- [ ] Room database reads/writes succeed

### Room Migration (future schema changes)
- [ ] When adding a v2 schema change:
  1. Increment `version` in `@Database` annotation
  2. Add migration object in `SignalDatabase.ALL_MIGRATIONS`
  3. Verify `app/schemas/2.json` is generated
  4. Test migration with `MigrationTestHelper` (instrumented test)
  5. Install over existing v1 app and verify data is preserved

### Syslog Testing
```bash
# From a machine on the same network or Tailscale mesh:
echo "<134>Mar 10 12:00:00 WLC-9800 %CLIENT_ORCH_LOG-5-ADD_TO_RUN_STATE: MAC: aabb.ccdd.eeff AP: AP-Floor2 slot 1" | nc -u <phone-ip> 1514

# Verify the message appears in the Syslog tab
# Verify a ROAM/AUTH event appears in the Timeline tab
```

### Permission Edge Cases
- [ ] Deny location permission: Scanner tab shows explanation
- [ ] Deny notification permission: Syslog service still starts (notification may be silent)
- [ ] Revoke permissions mid-use: App handles gracefully

## Post-Validation Sign-Off

| Area | Tester | Date | Pass/Fail | Notes |
|------|--------|------|-----------|-------|
| Unit tests | | | | |
| Debug build | | | | |
| Release build | | | | |
| On-device core | | | | |
| Data retention | | | | |
| R8 release | | | | |
| Permissions | | | | |
