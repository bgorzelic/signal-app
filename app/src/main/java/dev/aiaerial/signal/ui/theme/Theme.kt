package dev.aiaerial.signal.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Ghost Console — cinematic tactical dark theme
private val GhostColorScheme = darkColorScheme(
    // Primary: Electric Teal
    primary = ElectricTeal,
    onPrimary = Void,
    primaryContainer = ElectricTealDim,
    onPrimaryContainer = TextPrimary,

    // Secondary: Phantom Violet (agent/AI)
    secondary = PhantomViolet,
    onSecondary = Void,
    secondaryContainer = PhantomVioletDim,
    onSecondaryContainer = TextPrimary,

    // Tertiary: Ember (warning accent)
    tertiary = Ember,
    onTertiary = Void,
    tertiaryContainer = Ember.copy(alpha = 0.2f),
    onTertiaryContainer = Ember,

    // Error: Alert Red
    error = AlertRed,
    onError = TextPrimary,
    errorContainer = AlertRed.copy(alpha = 0.15f),
    onErrorContainer = AlertRed,

    // Backgrounds & surfaces
    background = Void,
    onBackground = TextPrimary,
    surface = Graphite,
    onSurface = TextPrimary,
    surfaceVariant = Charcoal,
    onSurfaceVariant = TextSecondary,
    surfaceTint = ElectricTeal,

    // Borders
    outline = BorderSubtle,
    outlineVariant = BorderFocus,

    // Navigation
    inverseSurface = TextPrimary,
    inverseOnSurface = Void,
    inversePrimary = ElectricTealDim,

    // Scrim
    scrim = Void,

    // Surface containers (Material 3 tonal elevation)
    surfaceBright = Slate,
    surfaceDim = Void,
    surfaceContainer = Graphite,
    surfaceContainerHigh = Charcoal,
    surfaceContainerHighest = Slate,
    surfaceContainerLow = Void,
    surfaceContainerLowest = Void,
)

@Composable
fun SignalTheme(
    darkTheme: Boolean = true, // Always dark — this is a tactical console
    content: @Composable () -> Unit,
) {
    val colorScheme = GhostColorScheme

    // Set system bar colors to match the void
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    CompositionLocalProvider(
        LocalSignalColors provides SignalColors(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SignalTypography,
            content = content,
        )
    }
}

// Extension to access domain colors from any composable
object SignalTheme {
    val colors: SignalColors
        @Composable
        get() = LocalSignalColors.current
}
