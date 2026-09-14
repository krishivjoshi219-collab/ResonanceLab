package com.resonancelab.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resonancelab.app.dsp.AcousticAnalysisResult
import com.resonancelab.app.dsp.MaterialType
import com.resonancelab.app.ui.components.CalibrationBar
import com.resonancelab.app.ui.components.DemoTapBar
import com.resonancelab.app.ui.components.EmptyState
import com.resonancelab.app.ui.components.LiquidLevelPanel
import com.resonancelab.app.ui.components.MaterialStateCard
import com.resonancelab.app.ui.components.PaywallSheet
import com.resonancelab.app.ui.components.SectionHeader
import com.resonancelab.app.ui.components.SonarChirpPanel
import com.resonancelab.app.ui.components.SpectrogramCanvas
import com.resonancelab.app.ui.components.SpectrumBarChart
import com.resonancelab.app.ui.components.StatusBanner
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
import com.resonancelab.app.ui.viewmodel.MainTab
import com.resonancelab.app.ui.viewmodel.ResonanceViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Primary Cybernetic HUD Main Screen for ResonanceLab.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: ResonanceViewModel) {
    val analysisResult by viewModel.analysisState.collectAsState()
    val isCapturing by viewModel.isCapturing.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val isPaywallOpen by viewModel.isPaywallOpen.collectAsState()
    val isPro by viewModel.isPro.collectAsState()
    val isDebugBypass by viewModel.isDebugBypass.collectAsState()
    val sensitivity by viewModel.sensitivity.collectAsState()
    val snapshotHistory by viewModel.snapshotHistory.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        containerColor = CyberVoidBlack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopCyberneticAppBar(
                isCapturing = isCapturing,
                isPro = isPro,
                onToggleCapture = { viewModel.toggleCapture() },
                onSnapshot = { viewModel.captureSnapshot() },
                onOpenPaywall = { viewModel.openPaywall() }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ModeTabRow(
                selectedTab = selectedTab,
                isPro = isPro,
                onSelectTab = { viewModel.selectTab(it) }
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTab) {
                    MainTab.LIVE_SPECTRUM -> {
                        item {
                            DemoTapBar(
                                onDemoTap = { viewModel.playDemoTap(it) }
                            )
                        }
                        item {
                            StatusBanner(
                                isCapturing = isCapturing,
                                rmsDbfs = analysisResult.metrics.rmsDbfs
                            )
                        }
                        item {
                            SectionHeader(
                                title = "Live spectrum",
                                subtitle = "Tap a surface, then watch peak, Q and decay settle."
                            )
                        }
                        item {
                            SpectrogramCanvas(
                                spectrogramFlow = viewModel.spectrogramFlow,
                                peakFrequencyHz = analysisResult.metrics.peakFrequencyHz,
                                isHollow = analysisResult.classification.type == MaterialType.HOLLOW_CAVITY,
                                canvasHeightDp = 220
                            )
                        }

                        item {
                            MaterialStateCard(
                                classification = analysisResult.classification,
                                metrics = analysisResult.metrics
                            )
                        }

                        item {
                            SpectrumBarChart(
                                normalizedMagnitudes = analysisResult.normalizedMagnitudes,
                                peakFrequencyHz = analysisResult.metrics.peakFrequencyHz,
                                qFactor = analysisResult.metrics.qFactor,
                                heightDp = 150
                            )
                        }

                        item {
                            CalibrationBar(
                                currentRmsDbfs = analysisResult.metrics.rmsDbfs,
                                calibratedNoiseFloorDb = viewModel.audioManager.metricsCalculator.calibratedNoiseFloorDb,
                                sensitivity = sensitivity,
                                onCalibrateNoiseFloor = { viewModel.calibrateNoiseFloor(it) },
                                onSensitivityChange = { viewModel.setSensitivity(it) }
                            )
                        }
                    }

                    MainTab.ACTIVE_SONAR -> {
                        item {
                            SonarChirpPanel(
                                sonarEcho = analysisResult.sonarEcho,
                                isPro = isPro,
                                onTriggerChirp = { viewModel.triggerActiveSonar() },
                                onUnlockPro = { viewModel.openPaywall() }
                            )
                        }

                        item {
                            SpectrogramCanvas(
                                spectrogramFlow = viewModel.spectrogramFlow,
                                peakFrequencyHz = analysisResult.metrics.peakFrequencyHz,
                                isHollow = false,
                                canvasHeightDp = 200
                            )
                        }

                        item {
                            MaterialStateCard(
                                classification = analysisResult.classification,
                                metrics = analysisResult.metrics
                            )
                        }
                    }

                    MainTab.LIQUID_LEVEL -> {
                        item {
                            LiquidLevelPanel(
                                liquidResult = analysisResult.liquidLevel,
                                isPro = isPro,
                                onContainerHeightChange = { viewModel.setLiquidContainerHeight(it) },
                                onUnlockPro = { viewModel.openPaywall() }
                            )
                        }

                        item {
                            SpectrumBarChart(
                                normalizedMagnitudes = analysisResult.normalizedMagnitudes,
                                peakFrequencyHz = analysisResult.metrics.peakFrequencyHz,
                                qFactor = analysisResult.metrics.qFactor,
                                heightDp = 160
                            )
                        }

                        item {
                            MaterialStateCard(
                                classification = analysisResult.classification,
                                metrics = analysisResult.metrics
                            )
                        }
                    }

                    MainTab.EXPORT_LOGS -> {
                        item {
                            ExportActionsCard(
                                isPro = isPro,
                                onExportPdf = { viewModel.exportCurrentPdf() },
                                onExportCsv = { viewModel.exportCurrentCsv() },
                                onUnlockPro = { viewModel.openPaywall() }
                            )
                        }

                        item {
                            Text(
                                text = "History · ${snapshotHistory.size} saved",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        if (snapshotHistory.isEmpty()) {
                            item {
                                EmptyState(
                                    title = "No snapshots yet",
                                    body = "Tap the camera button in the top bar during a test to save a sample here."
                                )
                            }
                        } else {
                            items(snapshotHistory) { snapshot ->
                                SnapshotHistoryItem(
                                    result = snapshot,
                                    onExportPdf = { viewModel.exportCurrentPdf(snapshot) }
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Paywall Bottom Sheet
    PaywallSheet(
        isOpen = isPaywallOpen,
        isPro = isPro,
        isDebugBypass = isDebugBypass,
        billingManager = viewModel.billingManager,
        onDismiss = { viewModel.closePaywall() }
    )
}

@Composable
private fun TopCyberneticAppBar(
    isCapturing: Boolean,
    isPro: Boolean,
    onToggleCapture: () -> Unit,
    onSnapshot: () -> Unit,
    onOpenPaywall: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CyberVoidBlack)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "ResonanceLab",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(8.dp))
                AssistChip(
                    onClick = onOpenPaywall,
                    label = {
                        Text(
                            text = if (isPro) "Pro" else "Free",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (isPro) QuantumViolet.copy(alpha = 0.16f) else CyberSurfaceVariant,
                        labelColor = if (isPro) QuantumViolet else TextSecondary
                    ),
                    border = null,
                    modifier = Modifier.height(28.dp)
                )
            }
            Text(
                text = "Acoustic material analysis",
                color = TextMuted,
                fontSize = 12.sp
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onSnapshot,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CyberSurfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Capture snapshot",
                    tint = TextSecondary,
                    modifier = Modifier.size(19.dp)
                )
            }
            Button(
                onClick = onToggleCapture,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCapturing) PlasmaPink.copy(alpha = 0.16f) else NeonCyan,
                    contentColor = if (isCapturing) PlasmaPink else Color(0xFF06202B)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(40.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isCapturing) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isCapturing) "Stop" else "Record",
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeTabRow(
    selectedTab: MainTab,
    isPro: Boolean,
    onSelectTab: (MainTab) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = selectedTab.ordinal,
        containerColor = CyberVoidBlack,
        contentColor = TextPrimary,
        edgePadding = 16.dp,
        indicator = {},
        divider = {}
    ) {
        MainTab.values().forEach { tab ->
            val isSelected = selectedTab == tab
            val isTabProGated = (tab != MainTab.LIVE_SPECTRUM)
            Tab(
                selected = isSelected,
                onClick = { onSelectTab(tab) },
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) CyberSurfaceVariant else Color.Transparent)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = tab.title,
                            color = if (isSelected) TextPrimary else TextMuted,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                        if (isTabProGated && !isPro) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(QuantumViolet.copy(alpha = 0.18f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Pro",
                                    color = QuantumViolet,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(20.dp)
                            .height(2.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) NeonCyan else Color.Transparent)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExportActionsCard(
    isPro: Boolean,
    onExportPdf: () -> Unit,
    onExportCsv: () -> Unit,
    onUnlockPro: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberDeepSlate),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Export report",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Generate a PDF inspection report with spectrum plots, or download raw CSV data.",
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { if (isPro) onExportPdf() else onUnlockPro() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPro) TextPrimary else CyberSurfaceVariant,
                        contentColor = if (isPro) CyberVoidBlack else TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "PDF", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
                FilledTonalButton(
                    onClick = { if (isPro) onExportCsv() else onUnlockPro() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "CSV", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun SnapshotHistoryItem(
    result: AcousticAnalysisResult,
    onExportPdf: () -> Unit
) {
    val dateStr = remember(result.timestampMs) {
        SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(result.timestampMs))
    }

    val typeColor = when (result.classification.type) {
        MaterialType.HOLLOW_CAVITY -> NeonCyan
        MaterialType.SOLID_SUBSTRATE -> NeonEmerald
        MaterialType.BOUNDARY_VOID -> PlasmaPink
        MaterialType.AMBIENT_NOISE -> TextMuted
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CyberDeepSlate),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(typeColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = result.classification.type.label,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = dateStr, color = TextMuted, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${String.format("%.0f", result.metrics.peakFrequencyHz)} Hz · Q ${String.format("%.1f", result.metrics.qFactor)} · ${String.format("%.1f", result.metrics.energyDecayRateDbPerSec)} dB/s",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            IconButton(
                onClick = onExportPdf,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share PDF",
                    tint = NeonCyan,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
