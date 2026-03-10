# Test Data — Sample Syslog Files

These files contain realistic syslog messages for testing SIGNAL's parser pipeline.

## Files

| File | Description | Events |
|---|---|---|
| `cisco-9800-roaming.log` | Cisco 9800 IOS-XE format — client roaming across APs, deauth, auth failure | 8 messages, 2 clients |
| `cisco-aireos-classic.log` | Cisco AireOS (5520) format — associations, deauths | 6 messages, 2 clients |
| `mixed-noise.log` | Mixed syslog from WLC + switches + firewall — tests parser selectivity | 9 messages, only 3 are Cisco WiFi |

## Usage

### Testing the parser from CLI

Send these to a running SIGNAL instance:
```bash
while IFS= read -r line; do
  echo "$line" | nc -u <phone-ip> 1514
  sleep 0.2
done < testdata/cisco-9800-roaming.log
```

### Testing via Log Import

1. Open SIGNAL → Syslog → Import
2. Copy-paste the contents of any file into the text area
3. Tap "Parse Events"
4. Verify event count matches expected

### Expected Parse Results

**cisco-9800-roaming.log:** 8 events (3 ROAM, 1 DEAUTH, 1 ASSOC, 1 ROAM, 1 AUTH, 1 DISASSOC)
**cisco-aireos-classic.log:** 6 events (3 ASSOC, 1 DEAUTH, 1 ROAM, 1 DEAUTH)
**mixed-noise.log:** 3 events (only the Cisco WiFi lines — non-WiFi lines are ignored)
