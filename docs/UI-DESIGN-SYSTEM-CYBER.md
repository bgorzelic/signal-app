# SIGNAL Design System — Cinematic Tactical

> Codename: "Ghost Console"
> A premium dark interface for a mobile AI agent runtime.

## Design Principles

1. **Darkness with structure** — Deep black foundations with layered charcoal surfaces. Light comes from information, not decoration.
2. **Precision over noise** — Every element earns its place. No filler graphics, no decorative tech lines.
3. **Agentic atmosphere** — The UI feels like it's connected to a live intelligence system. Status is ambient, not shouted.
4. **Cinematic restraint** — Inspired by tactical UI aesthetics, but never cosplay. Premium product, not a theme pack.
5. **Speed is luxury** — Fast transitions, instant feedback. The most expensive thing is responsiveness.

## Color System

### Base Palette

```
Background Layers (darkest → lightest):
  surface-0:    #0A0A0C    "void"         — screen background, deepest layer
  surface-1:    #111114    "graphite"     — primary card/panel background
  surface-2:    #1A1A1F    "charcoal"     — elevated cards, active surfaces
  surface-3:    #222228    "slate"        — hover/pressed states, modals

Border & Divider:
  border-subtle:  #2A2A32  — card edges, section dividers
  border-focus:   #3A3A45  — focused inputs, active selection
  border-accent:  primary at 30% alpha — highlighted elements

Text:
  text-primary:   #E8E8EC   — primary content, titles
  text-secondary: #8888A0   — supporting text, metadata
  text-tertiary:  #555568   — disabled, placeholder
  text-inverse:   #0A0A0C   — text on bright backgrounds
```

### Accent Colors

```
Primary:    #00D4AA    "electric teal"   — primary actions, links, active states
            Used sparingly. This is the signature color.

Secondary:  #7B68EE    "phantom violet"  — AI/agent status, memory, intelligence
            Used for anything representing the AI system.

Success:    #2ECC71    "signal green"    — excellent signal, healthy, connected
Warning:    #F0A500    "ember"           — fair signal, caution, degraded
Error:      #E74C3C    "alert red"       — poor signal, disconnected, critical
Info:       #5BA3D9    "ice blue"        — informational, neutral highlights

System:     #33FF33 at 8% alpha  "ghost green"  — subtle system pulse, alive indicator
            NEVER used at full intensity. Always ghostly.
```

### Signal Quality Colors (Domain-Specific)

```
Excellent:  #2ECC71   RSSI >= -50 dBm
Good:       #00D4AA   RSSI >= -60 dBm
Fair:       #F0A500   RSSI >= -70 dBm
Poor:       #E74C3C   RSSI >= -80 dBm
Dead:       #555568   RSSI < -80 dBm
```

### Event Type Colors

```
Roam:         #00D4AA  (primary)     — client moved between APs
Association:  #7B68EE  (secondary)   — client joined network
Auth:         #5BA3D9  (info)        — authentication event
Disassoc:     #F0A500  (warning)     — client left AP
Deauth:       #E74C3C  (error)       — forced disconnect
Unknown:      #555568  (tertiary)    — unclassified
```

## Typography

### Font Stack

```
Primary:     Inter (or system sans-serif fallback)
Monospace:   JetBrains Mono (or system monospace fallback)
```

### Scale

```
Display:     28sp / bold / -0.5 tracking    — screen titles (rare)
Title:       20sp / semibold / 0 tracking   — section headers
Subtitle:    16sp / medium / 0.1 tracking   — card titles
Body:        14sp / regular / 0.2 tracking  — primary content
Caption:     12sp / regular / 0.3 tracking  — metadata, timestamps
Micro:       10sp / medium / 0.5 tracking   — badges, chips, labels
Mono:        12sp / regular / 0 tracking    — logs, MACs, hex, code
```

### Numeric Treatment

Numbers in status contexts (RSSI, port numbers, counts) use:
- Tabular figures (fixed-width digits)
- Slightly heavier weight than surrounding text
- Primary or accent color when representing live data

## Spacing Scale

```
2dp   — micro gap (icon-to-text, badge padding)
4dp   — tight gap (inline elements)
8dp   — compact gap (related items)
12dp  — standard gap (card internal padding)
16dp  — section gap (between cards)
24dp  — major gap (screen sections)
32dp  — hero gap (between major content blocks)
```

## Component Language

### Cards

```
Standard Card:
  background:    surface-1
  border:        1dp solid border-subtle
  radius:        12dp
  padding:       12dp
  elevation:     0dp (flat — depth via color, not shadow)

Elevated Card (active/selected):
  background:    surface-2
  border:        1dp solid border-focus
  glow:          0dp 0dp 8dp primary at 10% alpha (optional)

Status Card (live data):
  background:    surface-1
  border-left:   3dp solid [status-color]
  radius:        12dp (with square left edge)
  padding:       12dp
```

### Buttons

```
Primary:
  background:    primary (#00D4AA)
  text:          text-inverse (#0A0A0C)
  radius:        8dp
  height:        44dp
  font:          14sp / semibold / uppercase / 1.0 tracking

Secondary:
  background:    transparent
  border:        1dp solid border-focus
  text:          text-primary
  radius:        8dp

Ghost:
  background:    transparent
  text:          text-secondary
  no border
```

### Status Badges

```
Small pill shape:
  height:    20dp
  radius:    10dp (full round)
  padding:   4dp horizontal, 2dp vertical
  font:      micro (10sp)
  background: status-color at 15% alpha
  text:      status-color at full
  border:    none

Example: [● CONNECTED] in green, [● OFFLINE] in red
```

### Navigation Bar

```
Background:    surface-0
Border-top:    1dp solid border-subtle
Item active:   primary color icon + label
Item inactive: text-tertiary icon + label
Indicator:     primary at 12% alpha (pill shape behind icon)
Height:        64dp
```

### Input Fields

```
Background:    surface-1
Border:        1dp solid border-subtle
Focus border:  1dp solid primary
Radius:        8dp
Text:          text-primary
Placeholder:   text-tertiary
Cursor:        primary
Height:        48dp
```

## Motion Principles

1. **Duration:** 150-250ms for micro-interactions, 300ms for screen transitions
2. **Easing:** `FastOutSlowIn` for enters, `FastOutLinearIn` for exits
3. **Stagger:** List items appear with 30ms stagger delay
4. **Pulse:** Status indicators use `infiniteTransition` with 2s period, 30-60% alpha range
5. **Charts:** Data points animate in with spring physics (stiffness=200, damping=15)
6. **No decorative motion.** Every animation communicates state change.

## Status & Agent States

```
Connected:     Solid primary dot + "Connected" badge
Checking:      Pulsing primary dot + "Checking..." badge
Disconnected:  Solid error dot + "Offline" badge
Degraded:      Pulsing warning dot + "Degraded" badge
Listening:     Pulsing ghost-green ring + "Live" badge
Idle:          Dim tertiary dot + "Idle" badge
```

## Empty States

```
Layout:
  Center-aligned vertically
  Icon:    48dp, text-tertiary, outlined style
  Title:   16sp / medium / text-secondary
  Body:    14sp / regular / text-tertiary
  Action:  Primary button (if applicable)
  Spacing: 16dp between elements

Tone: Concise and helpful, not cute. Guide the user to the next action.

Example:
  [WiFi icon, outlined, dim]
  "No networks scanned"
  "Tap Scan to discover nearby WiFi networks."
  [Scan Networks]
```

## Loading States

```
Skeleton:
  Rounded rectangles at surface-2 color
  Shimmer: subtle left-to-right gradient sweep (surface-2 → surface-3 → surface-2)
  Duration: 1.5s per sweep
  No spinner unless blocking (use skeletons for progressive loading)

Blocking:
  CircularProgressIndicator in primary color
  Centered in content area
  Optional: pulsing text below ("Analyzing..." etc.)
```

## Error States

```
Inline:
  Left-border card (3dp error-red border)
  Error icon + message
  Retry button if applicable

Full-screen:
  Center-aligned
  Error icon (48dp, error color)
  Title + description
  Retry or Back action
```
