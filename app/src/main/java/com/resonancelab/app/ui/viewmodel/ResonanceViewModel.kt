package com.resonancelab.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.resonancelab.app.audio.AudioRecordManager
import com.resonancelab.app.billing.BillingManager
import com.resonancelab.app.dsp.AcousticAnalysisResult
import com.resonancelab.app.dsp.MaterialType
import com.resonancelab.app.export.ReportExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class MainTab(val title: String) {
    LIVE_SPECTRUM("LIVE TAP"),
    ACTIVE_SONAR("ACTIVE SONAR"),
    LIQUID_LEVEL("LIQUID LEVEL"),
    EXPORT_LOGS("EXPORT / LOGS")
}

/**
 * Clean Architecture MVVM ViewModel orchestrating real-time audio capture,
 * DSP analysis pipeline, RevenueCat entitlements, and export services.
 */
class ResonanceViewModel(application: Application) : AndroidViewModel(application) {

    val audioManager: AudioRecordManager = AudioRecordManager()
    val billingManager: BillingManager = BillingManager(application, viewModelScope)
    val reportExporter: ReportExporter = ReportExporter(application)

    // Streaming Analysis Telemetry
    val analysisState: StateFlow<AcousticAnalysisResult> = audioManager.analysisState
    val spectrogramFlow: SharedFlow<FloatArray> = audioManager.spectrogramFlow

    // Capture lifecycle
    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    // Navigation Tab
    private val _selectedTab = MutableStateFlow(MainTab.LIVE_SPECTRUM)
    val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

    // Paywall Dialog State
    private val _isPaywallOpen = MutableStateFlow(false)
    val isPaywallOpen: StateFlow<Boolean> = _isPaywallOpen.asStateFlow()

    // Calibration
    private val _sensitivity = MutableStateFlow(1.0f)
    val sensitivity: StateFlow<Float> = _sensitivity.asStateFlow()

    // Snapshot history for NDT inspection logs
    private val _snapshotHistory = MutableStateFlow<List<AcousticAnalysisResult>>(emptyList())
    val snapshotHistory: StateFlow<List<AcousticAnalysisResult>> = _snapshotHistory.asStateFlow()

    // Pro Entitlement State
    val isPro: StateFlow<Boolean> = billingManager.isPro
    val isDebugBypass: StateFlow<Boolean> = billingManager.debugBypassEnabled

    init {
        viewModelScope.launch {
            isPro.collect { pro ->
                audioManager.setProStatus(pro)
            }
        }
    }

    // User feedback toasts/alerts
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun startCapture() {
        if (!_isCapturing.value) {
            audioManager.startCapture(viewModelScope)
            _isCapturing.value = true
        }
    }

    fun stopCapture() {
        if (_isCapturing.value) {
            audioManager.stopCapture()
            _isCapturing.value = false
        }
    }

    fun toggleCapture() {
        if (_isCapturing.value) {
            stopCapture()
        } else {
            startCapture()
        }
    }

    fun selectTab(tab: MainTab) {
        if ((tab == MainTab.ACTIVE_SONAR || tab == MainTab.LIQUID_LEVEL || tab == MainTab.EXPORT_LOGS) && !isPro.value) {
            // Show paywall if accessing Pro features without entitlement
            _isPaywallOpen.value = true
        }
        _selectedTab.value = tab
    }

    fun openPaywall() {
        _isPaywallOpen.value = true
    }

    fun closePaywall() {
        _isPaywallOpen.value = false
    }

    // Next Gen student test path: judges enable Pro without Play Store.
    fun setTestProEnabled(enabled: Boolean) {
        billingManager.setDebugBypass(enabled)
        if (enabled) _isPaywallOpen.value = false
    }

    fun calibrateNoiseFloor(rmsDbfs: Float) {
        audioManager.calibrateNoiseFloor(rmsDbfs)
        _statusMessage.value = "Noise floor calibrated: ${String.format("%.1f", rmsDbfs)} dBFS"
    }

    fun setSensitivity(multiplier: Float) {
        _sensitivity.value = multiplier
        audioManager.setSensitivity(multiplier)
    }

    fun triggerActiveSonar() {
        if (!isPro.value) {
            _isPaywallOpen.value = true
            return
        }
        audioManager.triggerSonarChirp(viewModelScope)
        _statusMessage.value = "Active Sonar pulse emitted"
    }

    fun setLiquidContainerHeight(heightCm: Float) {
        audioManager.liquidEstimator.totalHeightCm = heightCm
    }

    fun captureSnapshot() {
        val current = analysisState.value
        if (current.classification.type != MaterialType.AMBIENT_NOISE || current.metrics.rmsDbfs > -70.0f) {
            val list = _snapshotHistory.value.toMutableList()
            list.add(0, current)
            if (list.size > 20) list.removeAt(list.size - 1)
            _snapshotHistory.value = list
            _statusMessage.value = "Acoustic snapshot saved (${current.classification.type.label})"
        } else {
            _statusMessage.value = "Cannot capture snapshot: Signal below threshold"
        }
    }

    fun exportCurrentPdf(result: AcousticAnalysisResult = analysisState.value) {
        if (!isPro.value) {
            _isPaywallOpen.value = true
            return
        }
        viewModelScope.launch {
            try {
                val pdfFile = reportExporter.generatePdfReport(result)
                reportExporter.shareFile(pdfFile, "application/pdf")
                _statusMessage.value = "PDF report generated: ${pdfFile.name}"
            } catch (e: Exception) {
                _statusMessage.value = "Export error: ${e.message}"
            }
        }
    }

    fun exportCurrentCsv(result: AcousticAnalysisResult = analysisState.value) {
        if (!isPro.value) {
            _isPaywallOpen.value = true
            return
        }
        viewModelScope.launch {
            try {
                val csvFile = reportExporter.generateCsvExport(result)
                reportExporter.shareFile(csvFile, "text/csv")
                _statusMessage.value = "CSV exported: ${csvFile.name}"
            } catch (e: Exception) {
                _statusMessage.value = "Export error: ${e.message}"
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        audioManager.stopCapture()
    }
}
