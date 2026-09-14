package com.resonancelab.app.ui.components

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resonancelab.app.billing.BillingManager
import com.resonancelab.app.ui.theme.CyberCardBorder
import com.resonancelab.app.ui.theme.CyberDeepSlate
import com.resonancelab.app.ui.theme.CyberSurfaceVariant
import com.resonancelab.app.ui.theme.CyberVoidBlack
import com.resonancelab.app.ui.theme.NeonAmber
import com.resonancelab.app.ui.theme.NeonCyan
import com.resonancelab.app.ui.theme.NeonEmerald
import com.resonancelab.app.ui.theme.PlasmaPink
import com.resonancelab.app.ui.theme.QuantumViolet
import com.resonancelab.app.ui.theme.TextMuted
import com.resonancelab.app.ui.theme.TextPrimary
import com.resonancelab.app.ui.theme.TextSecondary
import com.revenuecat.purchases.Package

/**
 * Paywall Modal Bottom Sheet with RevenueCat Packages, Pro feature breakdown,
 * and built-in Judge/Demo Debug Bypass switch.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallSheet(
    isOpen: Boolean,
    isPro: Boolean,
    isDebugBypass: Boolean,
    billingManager: BillingManager,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    if (!isOpen) return

    val context = LocalContext.current
    val activity = context as? Activity
    var selectedPlanIndex by remember { mutableIntStateOf(0) }
    var actionMessage by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var isProcessing by remember { androidx.compose.runtime.mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CyberVoidBlack,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Close button top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(QuantumViolet.copy(alpha = 0.2f))
                        .border(0.5.dp, QuantumViolet, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "RESONANCE PRO",
                        color = QuantumViolet,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "ResonanceLab Pro",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Advanced sonar, liquid estimation, and certified reports.",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Feature List
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyberDeepSlate)
                    .border(1.dp, CyberCardBorder, RoundedCornerShape(10.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FeatureRow(
                    icon = Icons.Default.Radar,
                    title = "Active Sonar Chirp Radar",
                    description = "Frequency-modulated sweep pulses with matched filter acoustic echo ToF distance estimation.",
                    tint = QuantumViolet
                )
                FeatureRow(
                    icon = Icons.Default.WaterDrop,
                    title = "Acoustic Liquid Level Estimator",
                    description = "Quarter-wave air column resonance shift calculations for sealed & open container fill gauging.",
                    tint = NeonCyan
                )
                FeatureRow(
                    icon = Icons.Default.FileDownload,
                    title = "PDF & CSV Inspection Reports",
                    description = "Generate standardized non-destructive acoustic evaluation certificates with charts and raw FFT data.",
                    tint = NeonEmerald
                )
                FeatureRow(
                    icon = Icons.Default.Star,
                    title = "Continuous 48 kHz High-Res Streaming",
                    description = "Uncapped 60+ FPS waterfall spectrogram recording with instant cloud/disk export.",
                    tint = NeonAmber
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Subscription Package Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PlanCard(
                    title = "ANNUAL",
                    price = "$49.99 / yr",
                    tag = "SAVE 58%",
                    isSelected = selectedPlanIndex == 0,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedPlanIndex = 0 }
                )
                PlanCard(
                    title = "MONTHLY",
                    price = "$9.99 / mo",
                    tag = "FLEXIBLE",
                    isSelected = selectedPlanIndex == 1,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedPlanIndex = 1 }
                )
                PlanCard(
                    title = "LIFETIME",
                    price = "$119.99",
                    tag = "ONE-TIME",
                    isSelected = selectedPlanIndex == 2,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedPlanIndex = 2 }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (actionMessage != null) {
                Text(
                    text = actionMessage!!,
                    color = NeonAmber,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Primary Action Button
            Button(
                onClick = {
                    if (activity != null) {
                        isProcessing = true
                        val offerings = billingManager.offerings.value
                        val currentPackage: Package? = offerings?.current?.availablePackages?.firstOrNull()
                        if (currentPackage != null) {
                            billingManager.purchasePackage(
                                activity = activity,
                                rcPackage = currentPackage,
                                onSuccess = {
                                    isProcessing = false
                                    actionMessage = "Pro Access Activated!"
                                    onDismiss()
                                },
                                onError = { err ->
                                    isProcessing = false
                                    actionMessage = "Purchase note: $err"
                                }
                            )
                        } else {
                            // Demo simulation fallback
                            isProcessing = false
                            billingManager.setDebugBypass(true)
                            actionMessage = "Pro Demo Access Granted"
                            onDismiss()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = CyberVoidBlack
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        color = CyberVoidBlack,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (isPro) "PRO ACCESS ACTIVE" else "CONTINUE WITH PRO",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Restore Purchases button
            TextButton(
                onClick = {
                    isProcessing = true
                    billingManager.restorePurchases(
                        onSuccess = {
                            isProcessing = false
                            actionMessage = "Purchases restored successfully!"
                        },
                        onError = { err ->
                            isProcessing = false
                            actionMessage = err
                        }
                    )
                }
            ) {
                Text(
                    text = "Restore Purchases",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==========================================
            // JUDGE / DEMO DEBUG BYPASS BANNER & SWITCH
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberDeepSlate)
                    .border(1.dp, PlasmaPink.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = "Judge Bypass",
                            tint = PlasmaPink,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "JUDGE / VERIFICATION BYPASS",
                                color = PlasmaPink,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Instant Pro unlock for judges & review testing without purchase.",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                lineHeight = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Switch(
                        checked = isDebugBypass,
                        onCheckedChange = { billingManager.setDebugBypass(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PlasmaPink,
                            checkedTrackColor = PlasmaPink.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = CyberSurfaceVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Auto-renews until cancelled. Manage anytime in Google Play Store.",
                color = TextMuted,
                fontSize = 9.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    title: String,
    description: String,
    tint: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun PlanCard(
    title: String,
    price: String,
    tag: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) NeonCyan else CyberCardBorder
    val bgColor = if (isSelected) CyberSurfaceVariant else CyberDeepSlate

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(if (isSelected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isSelected) NeonCyan else CyberSurfaceVariant)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = tag,
                    color = if (isSelected) CyberVoidBlack else TextMuted,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = title,
                color = if (isSelected) TextPrimary else TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = price,
                color = if (isSelected) NeonCyan else TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
