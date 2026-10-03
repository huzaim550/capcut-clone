package com.reelforge.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = OnAccent,
    primaryContainer = AccentContainer,
    onPrimaryContainer = OnAccentContainer,
    background = Ink,
    onBackground = Mist,
    surface = Slate,
    onSurface = Mist,
    surfaceVariant = SlateRaised,
    onSurfaceVariant = MistMuted,
    error = Danger,
    onError = OnDanger,
)

private val LightColors = lightColorScheme(
    primary = LightAccent,
    onPrimary = Paper,
    primaryContainer = LightAccentContainer,
    onPrimaryContainer = Graphite,
    background = Paper,
    onBackground = Graphite,
    surface = Paper,
    onSurface = Graphite,
    surfaceVariant = PaperRaised,
    onSurfaceVariant = GraphiteMuted,
    error = Danger,
    onError = OnDanger,
)

private val ReelforgeTypography = Typography()

/**
 * The app's single theme entry point. Every screen wraps its content in this so
 * colour and type are decided in one place. Defaults to the system dark/light
 * setting rather than forcing dark, because the browser is usable in daylight.
 */
@Composable
fun ReelforgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = ReelforgeTypography,
        content = content,
    )
}
