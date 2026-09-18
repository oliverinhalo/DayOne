package com.dayone.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Accent = Color(0xFF00E5A0)
private val AccentDark = Color(0xFF00B583)

private val DarkColors = darkColorScheme(
    primary = Accent,
    secondary = AccentDark,
    background = Color(0xFF0E0E14),
    surface = Color(0xFF17171F)
)

private val LightColors = lightColorScheme(
    primary = AccentDark,
    secondary = Accent,
    background = Color(0xFFFAFAFA),
    surface = Color.White
)

@Composable
fun DayOneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
