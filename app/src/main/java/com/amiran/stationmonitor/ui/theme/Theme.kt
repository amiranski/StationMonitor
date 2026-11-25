package com.amiran.stationmonitor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = BrandNavy,
    onPrimary = Color.White,
    primaryContainer = NeutralTint,
    onPrimaryContainer = BrandNavy,
    secondary = BrandRed,
    onSecondary = Color.White,
    background = NeutralGround,
    onBackground = BrandNavy,
    surface = Color.White,
    onSurface = BrandNavy,
    surfaceVariant = NeutralTint,
    onSurfaceVariant = NeutralMuted,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F8FA),
    surfaceContainer = NeutralGround,
    surfaceContainerHigh = NeutralTint,
    surfaceContainerHighest = Color(0xFFE1E4EB),
    outline = NeutralOutline,
    outlineVariant = NeutralDivider,
    error = Color(0xFFB42318),
    onError = Color.White,
    errorContainer = Color(0xFFFCE6E3),
    onErrorContainer = Color(0xFF8C1D13),
    inverseSurface = Color(0xFF2F3033),
    inverseOnSurface = Color(0xFFF2F0F4),
    inversePrimary = Color(0xFFFFB4A8),
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color(0xFF12203A),
    primaryContainer = DarkTint,
    onPrimaryContainer = DarkOnSurface,
    secondary = Color(0xFFFF8A7A),
    onSecondary = Color(0xFF3D1A17),
    background = DarkGround,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkTint,
    onSurfaceVariant = DarkMuted,
    surfaceContainerLowest = Color(0xFF0B111E),
    surfaceContainerLow = Color(0xFF151D2F),
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkTint,
    surfaceContainerHighest = Color(0xFF2A3550),
    outline = Color(0xFF6E7890),
    outlineVariant = DarkDivider,
    error = Color(0xFFFFB4A8),
    onError = Color(0xFF5F1410),
    errorContainer = Color(0xFF3D1A17),
    onErrorContainer = Color(0xFFFFDAD4),
    inverseSurface = Color(0xFFE6E9F0),
    inverseOnSurface = Color(0xFF1A2337),
    inversePrimary = BrandRed,
)

private val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }

/**
 * The app theme. Dynamic color (Android 12+ wallpaper colors) is intentionally not used:
 * the app follows the company's brand colors instead.
 */
@Composable
fun StationMonitorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalStatusColors provides if (darkTheme) DarkStatusColors else LightStatusColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            content = content,
        )
    }
}

/** Lets screens write `MaterialTheme.statusColors.error` next to `MaterialTheme.colorScheme`. */
val MaterialTheme.statusColors: StatusColors
    @Composable
    @ReadOnlyComposable
    get() = LocalStatusColors.current
