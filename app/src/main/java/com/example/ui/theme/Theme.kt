package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AccessibleDarkColorScheme = darkColorScheme(
    primary = NeonCyanGlow,
    onPrimary = Color.Black,
    secondary = CardPhoneGreen,
    onSecondary = Color.White,
    tertiary = CardWhatsAppBlue,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkCardSurface,
    onSurface = TextPrimary,
    error = CardSosRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AccessibleDarkColorScheme,
        typography = Typography,
        content = content
    )
}

