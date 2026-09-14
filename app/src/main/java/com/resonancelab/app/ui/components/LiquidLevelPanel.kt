package com.resonancelab.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resonancelab.app.dsp.LiquidLevelResult
import com.resonancelab.app.ui.theme.CyberCardBorder
import com.resonancelab.app.ui.theme.CyberDeepSlate
import com.resonancelab.app.ui.theme.CyberSurfaceVariant
import com.resonancelab.app.ui.theme.CyberVoidBlack
import com.resonancelab.app.ui.theme.NeonAmber
import com.resonancelab.app.ui.theme.NeonCyan
import com.resonancelab.app.ui.theme.NeonEmerald
import com.resonancelab.app.ui.theme.TextMuted
import com.resonancelab.app.ui.theme.TextPrimary
import com.resonancelab.app.ui.theme.TextSecondary

/**
 * Acoustic Liquid Level Estimator Panel (Pro Tier).
 * Analyzes resonant acoustic air column cavity frequency shifts to estimate liquid level.
 */
@Composable
fun LiquidLevelPanel(
    liquidResult: LiquidLevelResult?,
    isPro: Boolean,
    onContainerHeightChange: (Float) -> Unit,
    onUnlockPro: () -> Unit,
    modifier: Modifier = Modifier
) {
    var containerHeight by remember { mutableFloatStateOf(30.0f) }
    val result = liquidResult ?: LiquidLevelResult(totalContainerHeightCm = containerHeight)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberDeepSlate),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = "Liquid Level",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Liquid level",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(NeonCyan.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "PRO",
                            color = NeonCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Tap the container wall to estimate fill height from air-column resonance.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Visual Tank & Metrics Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Visual Tank Tube
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberVoidBlack)
                            .border(1.dp, CyberCardBorder, RoundedCornerShape(8.dp))
                    ) {
                        // Liquid fill column from bottom
                        val fillRatio = (result.fillPercentage / 100.0f).coerceIn(0.0f, 1.0f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fillRatio)
                                .align(Alignment.BottomCenter)
                                .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(NeonCyan, NeonCyan.copy(alpha = 0.5f))
                                    )
                                )
                        )
                    }

                    // Numeric Metrics
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "FILL PERCENTAGE",
                                    color = TextMuted,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "${String.format("%.1f", result.fillPercentage)}%",
                                    color = NeonCyan,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "LIQUID HEIGHT",
                                    color = TextMuted,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "${String.format("%.1f", result.liquidHeightCm)} cm",
                                    color = NeonEmerald,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Container Height Setting Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "CONTAINER HEIGHT",
                                    color = TextSecondary,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "${containerHeight.toInt()} cm",
                                    color = NeonAmber,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Slider(
                                value = containerHeight,
                                onValueChange = {
                                    containerHeight = it
                                    onContainerHeightChange(it)
                                },
                                valueRange = 10.0f..100.0f,
                                steps = 18,
                                colors = SliderDefaults.colors(
                                    thumbColor = NeonAmber,
                                    activeTrackColor = NeonAmber,
                                    inactiveTrackColor = CyberSurfaceVariant
                                )
                            )
                        }
                    }
                }

                if (!isPro) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onUnlockPro,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberSurfaceVariant,
                            contentColor = NeonCyan
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = NeonAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "UNLOCK LIQUID LEVEL ESTIMATOR (PRO)",
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            if (!isPro) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberVoidBlack.copy(alpha = 0.65f))
                )
            }
        }
    }
}
