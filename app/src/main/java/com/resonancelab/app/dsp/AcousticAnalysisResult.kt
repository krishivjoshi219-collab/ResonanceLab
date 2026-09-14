package com.resonancelab.app.dsp

/**
 * Classification category determined by acoustic resonance metrics.
 */
enum class MaterialType(val label: String, val description: String) {
    HOLLOW_CAVITY(
        label = "HOLLOW / CAVITY",
        description = "High Q-factor, sustained resonance modes > 1.2 kHz, minimal mechanical damping"
    ),
    SOLID_SUBSTRATE(
        label = "SOLID / SUBSTRATE",
        description = "Rapid energy dissipation, heavily damped broad low-frequency response"
    ),
    BOUNDARY_VOID(
        label = "DELAMINATION / VOID",
        description = "Split resonant frequencies, structural boundary impedance mismatch"
    ),
    AMBIENT_NOISE(
        label = "IDLE / AMBIENT",
        description = "Acoustic signal below detection threshold"
    )
}

/**
 * Material classification outcome with confidence score and reasoning.
 */
data class MaterialClassification(
    val type: MaterialType = MaterialType.AMBIENT_NOISE,
    val confidence: Float = 0.0f, // 0.0 to 1.0 (0% to 100%)
    val primaryReason: String = "Awaiting acoustic tap stimulus",
    val acousticSignature: String = "Baseline ambient"
)

/**
 * Extracted DSP metrics from single or multi-frame acoustic analysis.
 */
data class AcousticMetrics(
    /** Dominant resonant peak frequency in Hz */
    val peakFrequencyHz: Float = 0.0f,
    /** Magnitude of the primary resonant peak in dBFS */
    val peakMagnitudeDb: Float = -90.0f,
    /** Spectral Centroid (Center of spectral gravity) in Hz */
    val spectralCentroidHz: Float = 0.0f,
    /** Quality Factor (Q = f0 / delta_f_-3dB), indicating resonant sharpness */
    val qFactor: Float = 0.0f,
    /** Damping rate in dB per second */
    val energyDecayRateDbPerSec: Float = 0.0f,
    /** Root Mean Square acoustic energy level in dBFS */
    val rmsDbfs: Float = -90.0f,
    /** Signal to Noise Ratio in dB above noise floor */
    val snrDb: Float = 0.0f,
    /** Prominent secondary resonance frequencies in Hz */
    val secondaryPeaksHz: List<Float> = emptyList()
)

/**
 * Pro Tier Active Sonar echo reflection result.
 */
data class SonarEchoResult(
    val timeOfFlightMs: Float = 0.0f,
    val estimatedDistanceCm: Float = 0.0f,
    val echoConfidence: Float = 0.0f,
    val rawCorrelationPeak: Float = 0.0f,
    val timestampMs: Long = System.currentTimeMillis()
)

/**
 * Pro Tier acoustic liquid level estimation result.
 */
data class LiquidLevelResult(
    val fillPercentage: Float = 0.0f, // 0.0 to 100.0%
    val liquidHeightCm: Float = 0.0f,
    val totalContainerHeightCm: Float = 30.0f,
    val fundamentalShiftHz: Float = 0.0f,
    val confidence: Float = 0.0f,
    val timestampMs: Long = System.currentTimeMillis()
)

/**
 * Complete immutable snapshot of real-time acoustic analysis.
 */
data class AcousticAnalysisResult(
    val timestampMs: Long = System.currentTimeMillis(),
    val metrics: AcousticMetrics = AcousticMetrics(),
    val classification: MaterialClassification = MaterialClassification(),
    /** 1024 magnitude bins normalized [0.0f..1.0f] for visualization */
    val normalizedMagnitudes: FloatArray = FloatArray(1024),
    val rawMagnitudes: FloatArray = FloatArray(1024),
    val isImpactDetected: Boolean = false,
    val sonarEcho: SonarEchoResult? = null,
    val liquidLevel: LiquidLevelResult? = null
)
