# SIGNAL Icon Direction — Cyber Premium

## Brand Context

SIGNAL is a network intelligence sensor — it listens, scans, triages, and surfaces insights from WiFi infrastructure. The icon should communicate: **signal intelligence, precision, quiet power**.

## Four Icon Concepts

### Concept A — "Signal Prism"
A stylized signal wave passing through a geometric prism/lens shape, suggesting analysis and transformation of raw signal into intelligence.

**Structure:**
- Three curved signal arcs (classic WiFi symbol geometry)
- Passing through or emerging from a sharp diamond/chevron shape
- The prism shape adds the "intelligence" layer — raw signal goes in, insight comes out

**Feel:** Technical, analytical, clean
**Radius:** Sharp outer edges, smooth inner curves
**Colors:** Electric teal on void black
**Monochrome:** Works as single-weight outline

### Concept B — "Ghost Pulse" (RECOMMENDED)
An abstract pulse/waveform inside a rounded square, suggesting a living signal monitor. The waveform has a subtle "heartbeat" quality — the app is alive, listening.

**Structure:**
- Rounded square container (adaptive icon safe zone)
- Inside: a stylized waveform — not a medical heartbeat, but a network pulse
- 3-4 clean peaks of varying height, suggesting signal analysis
- One peak highlighted or slightly taller (the "signal" moment)
- Optional: tiny dot at the peak (the detection point)

**Feel:** Alive, monitoring, precise, stealthy
**Radius:** 24dp outer radius (Android adaptive), 2dp stroke
**Colors:** Primary stroke on surface-0 background, optional ghost-green glow on peak
**Monochrome:** Single stroke weight, clean silhouette

**Why recommended:**
- Instantly communicates "signal monitoring" at any size
- The waveform is unique (not a generic WiFi icon)
- Works as adaptive, monochrome, and notification icon
- Has subtle energy without being noisy
- Scales from 16dp notification to 108dp launcher

### Concept C — "Stealth Aperture"
A hexagonal or circular aperture shape with segmented openings, suggesting a sensor or scanning lens.

**Structure:**
- Circle divided into 4-6 segments with thin gaps
- Segments suggest an iris/aperture opening
- Center dot or small circle (the sensor eye)

**Feel:** Surveillance, precision optics, intelligence gathering
**Radius:** Circular, with sharp segment edges
**Colors:** Phantom violet center, teal segments
**Monochrome:** Outline segments with center dot

### Concept D — "Minimal S"
A geometric, angular letterform "S" that doubles as a signal path or circuit trace.

**Structure:**
- The letter S rendered as clean right-angle or 45-degree paths
- Evokes a circuit trace or signal routing diagram
- Minimal, bold, typographic

**Feel:** Brand-forward, memorable, typographic
**Radius:** Sharp 90-degree corners or 45-degree diagonals
**Colors:** Teal on black
**Monochrome:** Single weight, crisp outline

## Recommendation: Concept B — "Ghost Pulse"

It best captures SIGNAL's identity as a **live network intelligence sensor**. The waveform communicates the app's core function instantly, and its "alive" quality supports the cinematic tactical aesthetic.

## In-App Icon Style

### Line Weight & Corner Treatment

```
Stroke:        1.5dp (standard), 2dp (emphasized)
Corner radius: 2dp for sharp technical feel (not rounded/friendly)
Cap:           Round caps on strokes
Join:          Round joins
Fill:          Outline only for navigation/toolbar; filled for status badges
```

### Active / Inactive Behavior

```
Active (selected tab):
  Color:    primary (#00D4AA)
  Weight:   2dp
  Fill:     Optional light fill at 8% primary alpha

Inactive:
  Color:    text-tertiary (#555568)
  Weight:   1.5dp
  Fill:     None

Disabled:
  Color:    text-tertiary at 50% alpha
  Weight:   1.5dp
```

### Icon Set Direction

Use a consistent set of **outlined, geometric** icons. Recommended source: **Material Symbols (Sharp)** variant or **Lucide** icons, customized to match the 1.5dp stroke weight and sharp corner radius.

Replace current icons:

| Current | Replace With | Reason |
|---------|-------------|--------|
| `Icons.Outlined.Wifi` | Custom signal/radar icon | More unique, matches brand |
| `Icons.AutoMirrored.Outlined.Message` | Terminal/console icon | Syslog is logs, not messages |
| `Icons.Outlined.Timeline` | Network graph icon | Better represents roaming timeline |
| `Icons.Outlined.Settings` | Sliders/tune icon | Sharper feel than gear |

## Android Launcher Icon Specification

### Adaptive Icon Structure

```
Foreground layer: 108dp × 108dp
  Safe zone:      72dp × 72dp (centered)
  Icon content:   Within safe zone
  Format:         Vector drawable (SVG → XML)

Background layer: 108dp × 108dp
  Color:          surface-0 (#0A0A0C) solid fill
  Alternative:    Very subtle radial gradient from #111114 center to #0A0A0C edge
```

### Monochrome Icon

```
Size:     48dp × 48dp (notification) or adaptive
Format:   Single-color vector, no fills
Color:    System-provided (Android handles tinting)
Weight:   2dp stroke (heavier than in-app for visibility at small sizes)
```

### Implementation (Android)

```xml
<!-- res/mipmap-anydpi-v26/ic_launcher.xml -->
<adaptive-icon>
    <background android:drawable="@drawable/ic_launcher_background"/>
    <foreground android:drawable="@drawable/ic_launcher_foreground"/>
    <monochrome android:drawable="@drawable/ic_launcher_monochrome"/>
</adaptive-icon>
```

## Color Application on Icons

Icons should NEVER use more than 2 colors:
1. **Primary stroke** (the icon shape)
2. **Accent highlight** (optional, for one detail — a dot, a peak, a pulse)

In dark mode, icons are always light-on-dark. Never outline icons on light backgrounds in this design system.
