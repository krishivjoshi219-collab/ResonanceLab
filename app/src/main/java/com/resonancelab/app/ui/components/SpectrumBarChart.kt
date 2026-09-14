package com.resonancelab.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resonancelab.app.audio.AudioConfig
import com.resonancelab.app.ui.theme.CyberCardBorder
import com.resonancelab.app.ui.theme.CyberDeepSlate
import com.resonancelab.app.ui.theme.CyberVoidBlack
import com.resonancelab.app.ui.theme.NeonAmber
import com.resonancelab.app.ui.theme.NeonCyan
import com.resonancelab.app.ui.theme.NeonEmerald
import com.resonancelab.app.ui.theme.TextMuted
import com.resonancelab.app.ui.theme.TextSecondary

/**
 * Real-time FFT magnitude spectrum.
 */
@Composable
fun SpectrumBarChart(
    normalizedMagnitudes: FloatArray,
    peakFrequencyHz: Float,
    qFactor: Float,
    modifier: Modifier = Modifier,
    heightDp: Int = 160
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CyberDeepSlate)
            .border(1.dp, CyberCardBorder, RoundedCornerShape(16.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp)) {
            val width = size.width
            val height = size.height

            val gridColor = Color(0xFF232E44)
            val dbSteps = listOf(0.25f, 0.50f, 0.75f)
            dbSteps.forEach { ratio ->
                val y = height * (1.0f - ratio)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
            }

            if (normalizedMagnitudes.isEmpty()) return@Canvas

            // Build filled path & stroke line across spectrum
            val fillPath = Path()
            val strokePath = Path()
            fillPath.moveTo(0f, height)
            strokePath.moveTo(0f, height * (1.0f - normalizedMagnitudes[0]))

            val stepX = width / normalizedMagnitudes.size.toFloat()
            for (i in normalizedMagnitudes.indices) {
                val x = i * stepX
                val mag = normalizedMagnitudes[i].coerceIn(0.0f, 1.0f)
                val y = height * (1.0f - mag)

                fillPath.lineTo(x, y)
                strokePath.lineTo(x, y)
            }

            fillPath.lineTo(width, height)
            fillPath.close()

            // Draw Neon Gradient Fill
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        NeonCyan.copy(alpha = 0.45f),
                        NeonCyan.copy(alpha = 0.15f),
                        Color.Transparent
                    )
                )
            )

            // Draw Spectrum Line
            drawPath(
                path = strokePath,
                color = NeonCyan,
                style = Stroke(width = 2.0f, cap = StrokeCap.Round)
            )

            // Draw Peak Marker Crosshair
            if (peakFrequencyHz > 80.0f) {
                val peakX = (peakFrequencyHz / AudioConfig.NYQUIST_FREQ) * width
                val peakBin = (peakFrequencyHz / AudioConfig.BIN_RESOLUTION).toInt().coerceIn(0, normalizedMagnitudes.size - 1)
                val peakMag = normalizedMagnitudes[peakBin].coerceIn(0.0f, 1.0f)
                val peakY = height * (1.0f - peakMag)

                // Peak circle marker
                drawCircle(
                    color = NeonAmber,
                    radius = 5.dp.toPx(),
                    center = Offset(peakX, peakY)
                )
                drawCircle(
                    color = CyberVoidBlack,
                    radius = 2.5.dp.toPx(),
                    center = Offset(peakX, peakY)
                )

                // Vertical beacon line
                drawLine(
                    color = NeonAmber.copy(alpha = 0.5f),
                    start = Offset(peakX, peakY),
                    end = Offset(peakX, height),
                    strokeWidth = 1.5f
                )
            }
        }

        Text(
            text = "Spectrum · 0–24 kHz",
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 12.dp, top = 8.dp)
        )
        Text(
            text = "${String.format("%.0f", peakFrequencyHz)} Hz · Q ${String.format("%.1f", qFactor)}",
            color = TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.TopEnd).padding(end = 12.dp, top = 8.dp)
        )
    }
}
