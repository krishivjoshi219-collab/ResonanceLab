package com.resonancelab.app.dsp

import com.resonancelab.app.audio.AudioConfig
import kotlin.math.abs
import kotlin.math.max

/**
 * Acoustic Liquid Level Estimator (Pro Feature).
 *
 * Utilizes acoustic resonance column shift (quarter-wave resonance mode in air chamber)
 * and impulse response to calculate liquid height and container fill percentage.
 *
 * Physics Model:
 * Fundamental air column resonance: f0 = v_air / (4 * L_air)
 * -> L_air = v_air / (4 * f0)
 * -> L_liquid = L_container - L_air
 * -> Fill % = (L_liquid / L_container) * 100%
 */
class LiquidLevelEstimator {

    /** Total container height in cm */
    var totalHeightCm: Float = 30.0f

    /** Speed of sound in air in cm/s (343.2 m/s = 34320 cm/s) */
    private val speedOfSoundAirCmS = AudioConfig.SPEED_OF_SOUND_AIR_M_S * 100.0f

    /**
     * Estimates liquid fill level from acoustic resonance peak.
     *
     * @param peakFreqHz Dominant detected resonant frequency in Hz.
     * @param metrics Full acoustic metrics.
     */
    fun estimate(peakFreqHz: Float, metrics: AcousticMetrics): LiquidLevelResult {
        if (peakFreqHz < 80.0f || metrics.rmsDbfs < -75.0f || metrics.snrDb < 6.0f) {
            return LiquidLevelResult(
                fillPercentage = 0.0f,
                liquidHeightCm = 0.0f,
                totalContainerHeightCm = totalHeightCm,
                fundamentalShiftHz = peakFreqHz,
                confidence = 0.0f
            )
        }

        // Theoretical minimum frequency for completely empty container (L_air = totalHeightCm)
        // f_empty = v / (4 * totalHeightCm)
        val fEmpty = speedOfSoundAirCmS / (4.0f * totalHeightCm)

        // If detected peak is lower than empty container fundamental, use base frequency
        val effectiveFreq = max(fEmpty * 0.8f, peakFreqHz)

        // Estimated air column length in cm: L_air = v / (4 * f)
        val estimatedAirHeightCm = speedOfSoundAirCmS / (4.0f * effectiveFreq)

        // Liquid height in cm
        val rawLiquidHeightCm = totalHeightCm - estimatedAirHeightCm
        val liquidHeightCm = rawLiquidHeightCm.coerceIn(0.0f, totalHeightCm)

        // Fill percentage
        val fillPercentage = (liquidHeightCm / totalHeightCm * 100.0f).coerceIn(0.0f, 100.0f)

        // Confidence evaluation based on Q-factor and SNR
        val qScore = (metrics.qFactor / 15.0f).coerceIn(0.4f, 1.0f)
        val snrScore = (metrics.snrDb / 25.0f).coerceIn(0.4f, 1.0f)
        val confidence = (qScore * snrScore).coerceIn(0.2f, 0.95f)

        return LiquidLevelResult(
            fillPercentage = fillPercentage,
            liquidHeightCm = liquidHeightCm,
            totalContainerHeightCm = totalHeightCm,
            fundamentalShiftHz = peakFreqHz,
            confidence = confidence
        )
    }
}
