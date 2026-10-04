package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val RacingColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = PitchBlack,
    primaryContainer = RacingCard,
    onPrimaryContainer = NeonCyan,
    secondary = RacingRed,
    onSecondary = MetalWhite,
    secondaryContainer = RacingCardBorder,
    onSecondaryContainer = RacingRedGlow,
    tertiary = NitroOrange,
    onTertiary = PitchBlack,
    background = RacingDarkBg,
    onBackground = MetalWhite,
    surface = RacingSurface,
    onSurface = MetalWhite,
    surfaceVariant = RacingCard,
    onSurfaceVariant = MetalGray,
    outline = RacingCardBorder
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RacingColorScheme,
        typography = Typography,
        content = content
    )
}
