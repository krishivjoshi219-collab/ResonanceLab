package com.resonancelab.app.dsp

/**
 * Non-destructive acoustic material testing and structural void classification engine.
 *
 * Rules:
 * - "Hollow / Cavity": Sharp sustained resonance (peak frequency > 1.2 kHz, high Q-factor > 10.0,
 *   low damping decay rate < 30 dB/s).
 * - "Solid / Substrate": Rapid mechanical damping (decay rate > 40 dB/s, low Q-factor < 7.0,
 *   broad low-frequency smear < 1 kHz).
 * - "Delamination / Void": Split resonant frequencies, intermediate Q with modal secondary peaks.
 * - "Ambient Noise / Idle": Signal below active trigger threshold.
 */
class MaterialClassifier {

    // Thresholds
    var triggerSnrThresholdDb: Float = 8.0f // Minimum dB above noise floor to evaluate impact
    var hollowMinFreqHz: Float = 1100.0f
    var hollowMinQFactor: Float = 10.0f
    var hollowMaxDecayRate: Float = 32.0f

    var solidMaxFreqHz: Float = 950.0f
    var solidMaxQFactor: Float = 6.5f
    var solidMinDecayRate: Float = 38.0f

    /**
     * Evaluates acoustic metrics and returns a material classification.
     */
    fun classify(metrics: AcousticMetrics): MaterialClassification {
        // Check if signal is above detection threshold
        if (metrics.snrDb < triggerSnrThresholdDb || metrics.rmsDbfs < -75.0f) {
            return MaterialClassification(
                type = MaterialType.AMBIENT_NOISE,
                confidence = 0.0f,
                primaryReason = "Signal level (${String.format("%.1f", metrics.rmsDbfs)} dBFS) below tap trigger threshold",
                acousticSignature = "Noise floor (${String.format("%.1f", metrics.snrDb)} dB SNR)"
            )
        }

        val freq = metrics.peakFrequencyHz
        val q = metrics.qFactor
        val decay = metrics.energyDecayRateDbPerSec
        val centroid = metrics.spectralCentroidHz
        val hasSecondaryPeaks = metrics.secondaryPeaksHz.isNotEmpty()

        // Evaluate Hollow / Cavity
        var hollowScore = 0.0f
        if (freq >= hollowMinFreqHz) hollowScore += 0.35f
        if (q >= hollowMinQFactor) hollowScore += 0.35f
        if (decay <= hollowMaxDecayRate && decay > 0.1f) hollowScore += 0.20f
        if (centroid >= 1400.0f) hollowScore += 0.10f

        // Evaluate Solid / Substrate
        var solidScore = 0.0f
        if (freq <= solidMaxFreqHz) solidScore += 0.35f
        if (q <= solidMaxQFactor) solidScore += 0.30f
        if (decay >= solidMinDecayRate) solidScore += 0.25f
        if (centroid < 1100.0f) solidScore += 0.10f

        // Evaluate Void / Boundary Delamination
        var voidScore = 0.0f
        if (hasSecondaryPeaks) voidScore += 0.40f
        if (freq in 800.0f..1800.0f) voidScore += 0.30f
        if (q in 6.0f..14.0f) voidScore += 0.30f

        return when {
            hollowScore >= solidScore && hollowScore >= voidScore && hollowScore >= 0.50f -> {
                val conf = (hollowScore * (metrics.snrDb / 20.0f).coerceIn(0.7f, 1.0f)).coerceIn(0.55f, 0.99f)
                MaterialClassification(
                    type = MaterialType.HOLLOW_CAVITY,
                    confidence = conf,
                    primaryReason = "Sharp resonance at ${String.format("%.0f", freq)} Hz with high Q-factor (${String.format("%.1f", q)}) and slow decay (${String.format("%.1f", decay)} dB/s)",
                    acousticSignature = "Cavity mode: Q=${String.format("%.1f", q)}, Centroid=${String.format("%.0f", centroid)} Hz"
                )
            }
            solidScore >= hollowScore && solidScore >= voidScore && solidScore >= 0.50f -> {
                val conf = (solidScore * (metrics.snrDb / 20.0f).coerceIn(0.7f, 1.0f)).coerceIn(0.55f, 0.98f)
                MaterialClassification(
                    type = MaterialType.SOLID_SUBSTRATE,
                    confidence = conf,
                    primaryReason = "High mechanical damping (${String.format("%.1f", decay)} dB/s) with low-frequency concentration at ${String.format("%.0f", freq)} Hz",
                    acousticSignature = "Solid substrate: Damped Q=${String.format("%.1f", q)}, Low smear"
                )
            }
            voidScore >= 0.55f -> {
                val conf = (voidScore * 0.9f).coerceIn(0.50f, 0.92f)
                MaterialClassification(
                    type = MaterialType.BOUNDARY_VOID,
                    confidence = conf,
                    primaryReason = "Split modal resonance (${metrics.secondaryPeaksHz.map { String.format("%.0f", it) }.joinToString()} Hz) indicating acoustic boundary mismatch",
                    acousticSignature = "Delamination void: Split peaks detected"
                )
            }
            else -> {
                // Ambiguous / intermediate
                val primaryType = if (freq > 1000.0f) MaterialType.HOLLOW_CAVITY else MaterialType.SOLID_SUBSTRATE
                MaterialClassification(
                    type = primaryType,
                    confidence = 0.52f,
                    primaryReason = "Transient acoustic response detected at ${String.format("%.0f", freq)} Hz (Moderate resonance)",
                    acousticSignature = "Intermediate acoustic impedance"
                )
            }
        }
    }
}
