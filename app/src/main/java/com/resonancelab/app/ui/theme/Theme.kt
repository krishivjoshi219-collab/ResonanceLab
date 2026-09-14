package com.resonancelab.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF06202B),
    primaryContainer = Color(0xFF0C2E3E),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = Color(0xFF94A3B8),
    onSecondary = Color(0xFF0B0E14),
    secondaryContainer = CyberSurfaceVariant,
    onSecondaryContainer = TextPrimary,
    tertiary = NeonAmber,
    onTertiary = Color(0xFF2A1E05),
    background = CyberVoidBlack,
    onBackground = TextPrimary,
    surface = CyberDeepSlate,
    onSurface = TextPrimary,
    surfaceVariant = CyberSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLow = CyberDeepSlate,
    surfaceContainer = Color(0xFF151C29),
    surfaceContainerHigh = CyberSurfaceVariant,
    outline = CyberCardBorder,
    outlineVariant = Color(0xFF1A2336),
    error = PlasmaPink,
    onError = Color(0xFF2A0B0B)
)

private val AppShapes = androidx.compose.material3.Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

@Composable
fun ResonanceLabTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = CyberVoidBlack.toArgb()
            window.navigationBarColor = CyberVoidBlack.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
