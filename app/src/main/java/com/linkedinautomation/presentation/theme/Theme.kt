package com.linkedinautomation.presentation.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Primary = Color(0xFF0A66C2)
private val OnPrimary = Color.White
private val Secondary = Color(0xFF057642)
private val Background = Color(0xFFF3F2EF)
private val Surface = Color.White
private val Error = Color(0xFFB00020)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    secondary = Secondary,
    background = Background,
    surface = Surface,
    error = Error
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
