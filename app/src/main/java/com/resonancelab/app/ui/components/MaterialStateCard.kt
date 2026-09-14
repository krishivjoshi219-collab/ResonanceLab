package com.resonancelab.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resonancelab.app.dsp.AcousticMetrics
import com.resonancelab.app.dsp.MaterialClassification
import com.resonancelab.app.dsp.MaterialType
import com.resonancelab.app.ui.theme.CyberCardBorder
import com.resonancelab.app.ui.theme.CyberDeepSlate
import com.resonancelab.app.ui.theme.CyberSurfaceVariant
import com.resonancelab.app.ui.theme.CyberVoidBlack
import com.resonancelab.app.ui.theme.NeonAmber
import com.resonancelab.app.ui.theme.NeonCyan
import com.resonancelab.app.ui.theme.NeonEmerald
import com.resonancelab.app.ui.theme.PlasmaPink
import com.resonancelab.app.ui.theme.TextMuted
import com.resonancelab.app.ui.theme.TextPrimary
import com.resonancelab.app.ui.theme.TextSecondary

/**
 * Cybernetic HUD Card displaying live Material Classification, confidence meter,
 * and key acoustic DSP telemetry metrics.
 */
@Composable
fun MaterialStateCard(
    classification: MaterialClassification,
    metrics: AcousticMetrics,
    modifier: Modifier = Modifier
) {
    val themeColor by animateColorAsState(
        targetValue = when (classification.type) {
            MaterialType.HOLLOW_CAVITY -> NeonCyan
            MaterialType.SOLID_SUBSTRATE -> NeonEmerald
            MaterialType.BOUNDARY_VOID -> PlasmaPink
            MaterialType.AMBIENT_NOISE -> TextMuted
        },
        label = "ThemeColorAnim"
    )

    val animatedConfidence by animateFloatAsState(
        targetValue = classification.confidence,
        label = "ConfidenceAnim"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberDeepSlate),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(8.dp).clip(CircleShape).background(themeColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = classification.type.label,
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        Text(
                            text = classification.acousticSignature,
                            color = TextMuted,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                }
                Text(
                    text = "${(animatedConfidence * 100).toInt()}%",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { animatedConfidence },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                color = themeColor,
                trackColor = CyberSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = classification.primaryReason,
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2x3 Metric Telemetry Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricPill(
                    label = "Peak",
                    value = "${String.format("%.0f", metrics.peakFrequencyHz)} Hz",
                    accentColor = NeonCyan,
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = "Q factor",
                    value = String.format("%.1f", metrics.qFactor),
                    accentColor = if (metrics.qFactor > 10) NeonCyan else NeonEmerald,
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = "Decay",
                    value = "${String.format("%.1f", metrics.energyDecayRateDbPerSec)} dB/s",
                    accentColor = if (metrics.energyDecayRateDbPerSec > 40) NeonEmerald else NeonAmber,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricPill(
                    label = "Centroid",
                    value = "${String.format("%.0f", metrics.spectralCentroidHz)} Hz",
                    accentColor = NeonAmber,
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = "Level",
                    value = "${String.format("%.1f", metrics.rmsDbfs)} dBFS",
                    accentColor = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = "SNR",
                    value = "+${String.format("%.1f", metrics.snrDb)} dB",
                    accentColor = if (metrics.snrDb > 10) NeonEmerald else TextMuted,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(CyberSurfaceVariant.copy(alpha = 0.55f))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.35f))
        )
    }
}
