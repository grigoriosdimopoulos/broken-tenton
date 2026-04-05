package com.linkedinautomation.presentation.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Broken Tenton — dark, pink & red palette
private val PinkPrimary    = Color(0xFFE91E63)   // Material Pink 500
private val PinkLight      = Color(0xFFF06292)   // Pink 300
private val RedAccent      = Color(0xFFFF1744)   // Red A400
private val BlackBg        = Color(0xFF0D0D0D)
private val DarkSurface    = Color(0xFF1C1C1E)
private val DarkSurface2   = Color(0xFF2A2A2E)
private val OnDark         = Color(0xFFF5F5F5)
private val OnDarkVariant  = Color(0xFFB0B0B8)

private val DarkColorScheme = darkColorScheme(
    primary             = PinkPrimary,
    onPrimary           = Color.White,
    primaryContainer    = Color(0xFF880E4F),
    onPrimaryContainer  = Color(0xFFFCE4EC),
    secondary           = RedAccent,
    onSecondary         = Color.White,
    secondaryContainer  = Color(0xFF7F0000),
    onSecondaryContainer = Color(0xFFFFCDD2),
    tertiary            = PinkLight,
    onTertiary          = Color.Black,
    background          = BlackBg,
    onBackground        = OnDark,
    surface             = DarkSurface,
    onSurface           = OnDark,
    surfaceVariant      = DarkSurface2,
    onSurfaceVariant    = OnDarkVariant,
    outline             = Color(0xFF4A4A55),
    error               = RedAccent,
    onError             = Color.White
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
