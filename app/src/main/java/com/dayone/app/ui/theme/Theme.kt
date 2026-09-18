package com.dayone.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.dayone.app.data.ThemeMode

const val DEFAULT_ACCENT_ARGB: Int = 0xFF00E5A0.toInt()

private fun schemeFor(accent: Color, dark: Boolean, amoled: Boolean) = if (dark) {
    darkColorScheme(
        primary = accent,
        onPrimary = if (accent.luminance() > 0.5f) Color.Black else Color.White,
        secondary = accent.copy(alpha = 0.85f),
        tertiary = accent,
        background = if (amoled) Color.Black else Color(0xFF0E0E14),
        surface = if (amoled) Color.Black else Color(0xFF15151C),
        surfaceVariant = if (amoled) Color(0xFF101014) else Color(0xFF23232D),
        surfaceContainer = if (amoled) Color(0xFF0A0A0C) else Color(0xFF1A1A22),
        surfaceContainerHigh = if (amoled) Color(0xFF121216) else Color(0xFF212129),
        outline = Color(0xFF585866)
    )
} else {
    lightColorScheme(
        primary = accent,
        onPrimary = if (accent.luminance() > 0.5f) Color.Black else Color.White,
        secondary = accent,
        tertiary = accent,
        background = Color(0xFFF7F8FA),
        surface = Color.White,
        surfaceVariant = Color(0xFFE9EAEF),
        surfaceContainer = Color(0xFFF1F2F6),
        surfaceContainerHigh = Color(0xFFE9EAF0),
        outline = Color(0xFF9A9AA6)
    )
}

private val DayOneTypography = Typography().let { base ->
    base.copy(
        headlineMedium = base.headlineMedium.merge(TextStyle(fontWeight = FontWeight.SemiBold)),
        titleLarge = base.titleLarge.merge(TextStyle(fontWeight = FontWeight.SemiBold)),
        titleMedium = base.titleMedium.merge(TextStyle(fontWeight = FontWeight.Medium)),
        labelLarge = base.labelLarge.merge(TextStyle(fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp))
    )
}

@Composable
fun DayOneTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    amoledDark: Boolean = false,
    accentArgb: Int = DEFAULT_ACCENT_ARGB,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        else -> schemeFor(Color(accentArgb), dark, amoledDark)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            // Status/navigation icons flip to dark on light themes so they stay readable.
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    MaterialTheme(colorScheme = colors, typography = DayOneTypography, content = content)
}
