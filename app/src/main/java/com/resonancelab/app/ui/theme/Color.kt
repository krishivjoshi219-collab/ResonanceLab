package com.resonancelab.app.ui.theme

import androidx.compose.ui.graphics.Color

// Sci-Fi Cybernetic Dark / Neon Palette
val CyberVoidBlack = Color(0xFF070B12)
val CyberDeepSlate = Color(0xFF0D1524)
val CyberSurfaceVariant = Color(0xFF142036)
val CyberCardBorder = Color(0xFF1E2F4D)
val CyberCardGlow = Color(0xFF00F0FF).copy(alpha = 0.15f)

val NeonCyan = Color(0xFF00F0FF)         // Primary resonance / Cavity mode
val NeonEmerald = Color(0xFF00FF9D)      // Solid substrate mode / Success
val NeonAmber = Color(0xFFFFB800)        // Warnings / Peak indicators
val PlasmaPink = Color(0xFFFF0055)       // High alert / Void delamination
val QuantumViolet = Color(0xFF9D4EDD)    // Sonar pulse / Pro badge

val TextPrimary = Color(0xFFE2F1FF)
val TextSecondary = Color(0xFF88A4C8)
val TextMuted = Color(0xFF4A6588)

// Spectrogram Heatmap Palette Table (ARGB)
val HeatmapColors = intArrayOf(
    0xFF070B12.toInt(), // Deep Void (Quiet)
    0xFF0D1B36.toInt(), // Navy Indigo
    0xFF123472.toInt(), // Royal Blue
    0xFF006699.toInt(), // Deep Cyan
    0xFF00F0FF.toInt(), // Cyber Cyan
    0xFF00FF9D.toInt(), // Neon Emerald
    0xFFFFCC00.toInt(), // Neon Amber
    0xFFFF0055.toInt(), // Plasma Magenta
    0xFFFFFFFF.toInt()  // White Hot
)
