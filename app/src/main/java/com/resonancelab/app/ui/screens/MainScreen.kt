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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.resonancelab.app.ui.components.LiquidLevelPanel
import com.resonancelab.app.ui.components.MaterialStateCard
import com.resonancelab.app.ui.components.PaywallSheet
import com.resonancelab.app.ui.components.SonarChirpPanel
import com.resonancelab.app.ui.components.SpectrogramCanvas
import com.resonancelab.app.ui.components.SpectrumBarChart
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
            // Mode Navigation Tabs
            ModeTabRow(
                selectedTab = selectedTab,
                isPro = isPro,
                onSelectTab = { viewModel.selectTab(it) }
            )

            // Dynamic Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (selectedTab) {
                    MainTab.LIVE_SPECTRUM -> {
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
                                text = "ACOUSTIC TEST AUDIT TRAIL (${snapshotHistory.size} SAVED)",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        if (snapshotHistory.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(CyberDeepSlate)
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No saved snapshots yet. Tap the snapshot button in top bar during live testing to log samples.",
                                        color = TextMuted,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
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
            .border(0.5.dp, CyberCardBorder)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Branding
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "RESONANCELAB",
                    color = NeonCyan,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isPro) QuantumViolet.copy(alpha = 0.25f) else CyberSurfaceVariant)
                        .border(0.5.dp, if (isPro) QuantumViolet else CyberCardBorder, RoundedCornerShape(4.dp))
                        .clickable(onClick = onOpenPaywall)
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isPro) "PRO ACCESS" else "FREE TIER",
                        color = if (isPro) QuantumViolet else TextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            Text(
                text = "NON-DESTRUCTIVE ACOUSTIC DSP",
                color = TextMuted,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Action Buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Snapshot Button
            IconButton(
                onClick = onSnapshot,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberSurfaceVariant)
                    .border(0.5.dp, CyberCardBorder, RoundedCornerShape(8.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Capture Snapshot",
                    tint = NeonAmber,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Capture Toggle Button (Start / Stop)
            Button(
                onClick = onToggleCapture,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCapturing) PlasmaPink else NeonCyan,
                    contentColor = CyberVoidBlack
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isCapturing) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isCapturing) "Stop" else "Record",
                        tint = CyberVoidBlack,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isCapturing) "HALT" else "CAPTURE",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
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
        containerColor = CyberDeepSlate,
        contentColor = NeonCyan,
        edgePadding = 8.dp,
        indicator = {},
        divider = {}
    ) {
        MainTab.values().forEach { tab ->
            val isSelected = selectedTab == tab
            val isTabProGated = (tab != MainTab.LIVE_SPECTRUM)

            Tab(
                selected = isSelected,
                onClick = { onSelectTab(tab) },
                modifier = Modifier
                    .padding(horizontal = 4.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) CyberSurfaceVariant else Color.Transparent)
                    .border(
                        0.5.dp,
                        if (isSelected) NeonCyan.copy(alpha = 0.6f) else Color.Transparent,
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tab.title,
                        color = if (isSelected) NeonCyan else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    if (isTabProGated && !isPro) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(QuantumViolet.copy(alpha = 0.3f))
                                .padding(horizontal = 3.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "PRO",
                                color = QuantumViolet,
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
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
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberCardBorder, RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CyberDeepSlate)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "EXPORT CERTIFICATION & RAW TELEMETRY",
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Generate comprehensive NDT acoustic evaluation PDF reports with FFT plots and raw CSV spectra.",
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp
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
                        containerColor = if (isPro) NeonCyan else CyberSurfaceVariant,
                        contentColor = if (isPro) CyberVoidBlack else NeonCyan
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PDF REPORT",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Button(
                    onClick = { if (isPro) onExportCsv() else onUnlockPro() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPro) NeonEmerald else CyberSurfaceVariant,
                        contentColor = if (isPro) CyberVoidBlack else NeonEmerald
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CSV DATA",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
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
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, CyberCardBorder, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = CyberDeepSlate)
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
                        color = typeColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "[$dateStr]",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Peak: ${String.format("%.0f", result.metrics.peakFrequencyHz)} Hz | Q: ${String.format("%.1f", result.metrics.qFactor)} | Decay: ${String.format("%.1f", result.metrics.energyDecayRateDbPerSec)} dB/s",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
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
