package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ShDarkColorScheme = darkColorScheme(
    primary = BrandCyan,
    onPrimary = Color(0xFF001F26),
    primaryContainer = BrandBlueDark,
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = BrandBlue,
    onSecondary = Color.White,
    secondaryContainer = DarkSurfaceElevated,
    onSecondaryContainer = TextPrimaryDark,
    tertiary = StatusRegisteredGreen,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = TextPrimaryDark,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = DarkOutline,
    error = StatusFailedRed,
    onError = Color.White
)

@Composable
fun ShCallingTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ShDarkColorScheme,
        typography = ShCallingTypography,
        content = content
    )
}
