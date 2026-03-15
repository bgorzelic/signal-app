package dev.aiaerial.signal.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// === Ghost Console Color System ===

// Background layers (darkest → lightest)
val Void = Color(0xFF0A0A0C)
val Graphite = Color(0xFF111114)
val Charcoal = Color(0xFF1A1A1F)
val Slate = Color(0xFF222228)

// Borders & dividers
val BorderSubtle = Color(0xFF2A2A32)
val BorderFocus = Color(0xFF3A3A45)

// Text
val TextPrimary = Color(0xFFE8E8EC)
val TextSecondary = Color(0xFF8888A0)
val TextTertiary = Color(0xFF555568)

// Accent: Electric Teal (signature)
val ElectricTeal = Color(0xFF00D4AA)
val ElectricTealDim = Color(0xFF009977)

// Accent: Phantom Violet (AI/agent)
val PhantomViolet = Color(0xFF7B68EE)
val PhantomVioletDim = Color(0xFF5A4EC0)

// Status
val SignalGreen = Color(0xFF2ECC71)
val Ember = Color(0xFFF0A500)
val AlertRed = Color(0xFFE74C3C)
val IceBlue = Color(0xFF5BA3D9)

// System pulse
val GhostGreen = Color(0xFF33FF33)

@Immutable
data class SignalColors(
    // Signal quality
    val signalExcellent: Color = SignalGreen,
    val signalGood: Color = ElectricTeal,
    val signalFair: Color = Ember,
    val signalPoor: Color = AlertRed,
    val signalDead: Color = TextTertiary,

    // Event types
    val eventRoam: Color = ElectricTeal,
    val eventAssoc: Color = PhantomViolet,
    val eventAuth: Color = IceBlue,
    val eventDisassoc: Color = Ember,
    val eventDeauth: Color = AlertRed,
    val eventUnknown: Color = TextTertiary,

    // Agent status
    val statusConnected: Color = SignalGreen,
    val statusChecking: Color = ElectricTeal,
    val statusDisconnected: Color = AlertRed,
    val statusDegraded: Color = Ember,
    val statusListening: Color = GhostGreen,
    val statusIdle: Color = TextTertiary,

    // Surfaces
    val surface0: Color = Void,
    val surface1: Color = Graphite,
    val surface2: Color = Charcoal,
    val surface3: Color = Slate,
    val borderSubtle: Color = BorderSubtle,
    val borderFocus: Color = BorderFocus,

    // Semantic
    val accent: Color = ElectricTeal,
    val accentDim: Color = ElectricTealDim,
    val agent: Color = PhantomViolet,
    val agentDim: Color = PhantomVioletDim,
)

val LocalSignalColors = staticCompositionLocalOf { SignalColors() }
