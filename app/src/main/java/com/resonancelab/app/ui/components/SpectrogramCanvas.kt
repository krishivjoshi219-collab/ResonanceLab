package com.resonancelab.app.ui.components

import android.graphics.Bitmap
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resonancelab.app.audio.AudioConfig
import com.resonancelab.app.ui.theme.CyberCardBorder
import com.resonancelab.app.ui.theme.CyberVoidBlack
import com.resonancelab.app.ui.theme.HeatmapColors
import com.resonancelab.app.ui.theme.NeonAmber
import com.resonancelab.app.ui.theme.NeonCyan
import com.resonancelab.app.ui.theme.TextMuted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min

/**
 * High-performance scrolling waterfall spectrogram in Jetpack Compose.
 *
 * Renders real-time frequency distribution [0 Hz - 24 kHz] over time
 * using an internal mutable Bitmap pixel buffer to achieve 60+ FPS
 * with zero Garbage Collection thrashing.
 */
@Composable
fun SpectrogramCanvas(
    spectrogramFlow: SharedFlow<FloatArray>,
    peakFrequencyHz: Float,
    isHollow: Boolean,
    modifier: Modifier = Modifier,
    canvasHeightDp: Int = 240
) {
    // Bitmap dimensions for spectrogram waterfall texture
    val textureWidth = 256  // Frequency bins displayed
    val textureHeight = 160 // Time history frames

    val bitmap = remember {
        Bitmap.createBitmap(textureWidth, textureHeight, Bitmap.Config.ARGB_8888)
    }
    val pixelBuffer = remember { IntArray(textureWidth * textureHeight) }

    // Color gradient lookup table (256 levels mapped to Cyberpunk palette)
    val colorLut = remember {
        IntArray(256) { level ->
            val factor = level / 255.0f
            interpolateCyberColor(factor)
        }
    }

    // Collect streaming FFT frames and update bitmap pixels
    LaunchedEffect(spectrogramFlow) {
        spectrogramFlow.collectLatest { normalizedMagnitudes ->
            // Shift history down by 1 row
            System.arraycopy(
                pixelBuffer, 0,
                pixelBuffer, textureWidth,
                textureWidth * (textureHeight - 1)
            )

            // Map current FFT slice (1024 bins) into top row (textureWidth = 256)
            val binStep = normalizedMagnitudes.size.toFloat() / textureWidth.toFloat()
            for (x in 0 until textureWidth) {
                val binIdx = (x * binStep).toInt().coerceIn(0, normalizedMagnitudes.size - 1)
                val magnitude = normalizedMagnitudes[binIdx].coerceIn(0.0f, 1.0f)
                val lutIdx = (magnitude * 255.0f).toInt().coerceIn(0, 255)
                pixelBuffer[x] = colorLut[lutIdx]
            }

            // Write into bitmap
            bitmap.setPixels(pixelBuffer, 0, textureWidth, 0, 0, textureWidth, textureHeight)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(canvasHeightDp.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(CyberVoidBlack)
            .border(1.dp, CyberCardBorder, RoundedCornerShape(8.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw scrolling spectrogram texture stretched across canvas
            drawImage(
                image = bitmap.asImageBitmap(),
                srcOffset = androidx.compose.ui.unit.IntOffset.Zero,
                srcSize = androidx.compose.ui.unit.IntSize(textureWidth, textureHeight),
                dstOffset = androidx.compose.ui.unit.IntOffset.Zero,
                dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt())
            )

            // Draw Frequency Grid & Markers
            drawFrequencyGrid()

            // Draw Peak Resonance Tracking Line
            if (peakFrequencyHz > 100.0f) {
                drawPeakTracker(peakFrequencyHz, isHollow)
            }
        }

        // Frequency Legend Labels
        Text(
            text = "24 kHz",
            color = TextMuted,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 4.dp, end = 6.dp)
        )
        Text(
            text = "12 kHz",
            color = TextMuted,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 4.dp)
        )
        Text(
            text = "0 Hz",
            color = TextMuted,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 4.dp, start = 6.dp)
        )

        // Waterfall Direction Indicator
        Text(
            text = "▼ TIME (LIVE)",
            color = NeonCyan.copy(alpha = 0.6f),
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 4.dp, start = 6.dp)
        )
    }
}

/**
 * Draws vertical frequency guide lines.
 */
private fun DrawScope.drawFrequencyGrid() {
    val gridColor = Color(0x2200F0FF)
    val numDivisions = 4
    for (i in 1 until numDivisions) {
        val x = size.width * (i.toFloat() / numDivisions)
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1f
        )
    }
}

/**
 * Draws a vertical line marking the current dominant resonant peak frequency.
 */
private fun DrawScope.drawPeakTracker(peakHz: Float, isHollow: Boolean) {
    val peakX = (peakHz / AudioConfig.NYQUIST_FREQ) * size.width
    val color = if (isHollow) NeonCyan else NeonAmber

    // Glowing vertical line
    drawLine(
        color = color.copy(alpha = 0.35f),
        start = Offset(peakX, 0f),
        end = Offset(peakX, size.height),
        strokeWidth = 4f
    )
    drawLine(
        color = color,
        start = Offset(peakX, 0f),
        end = Offset(peakX, size.height),
        strokeWidth = 1.5f
    )
}

/**
 * Smooth RGB interpolation across cyber heatmap palette.
 */
private fun interpolateCyberColor(t: Float): Int {
    val clamped = t.coerceIn(0.0f, 1.0f)
    val colors = HeatmapColors
    val numIntervals = colors.size - 1
    val scaled = clamped * numIntervals
    val index = scaled.toInt().coerceIn(0, numIntervals - 1)
    val frac = scaled - index

    val c1 = colors[index]
    val c2 = colors[index + 1]

    val a1 = (c1 shr 24) and 0xFF
    val r1 = (c1 shr 16) and 0xFF
    val g1 = (c1 shr 8) and 0xFF
    val b1 = c1 and 0xFF

    val a2 = (c2 shr 24) and 0xFF
    val r2 = (c2 shr 16) and 0xFF
    val g2 = (c2 shr 8) and 0xFF
    val b2 = c2 and 0xFF

    val a = (a1 + frac * (a2 - a1)).toInt()
    val r = (r1 + frac * (r2 - r1)).toInt()
    val g = (g1 + frac * (g2 - g1)).toInt()
    val b = (b1 + frac * (b2 - b1)).toInt()

    return (a shl 24) or (r shl 16) or (g shl 8) or b
}
