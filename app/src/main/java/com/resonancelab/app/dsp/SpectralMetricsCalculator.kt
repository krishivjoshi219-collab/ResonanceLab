package com.resonancelab.app.dsp

import com.resonancelab.app.audio.AudioConfig
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

/**
 * High-precision DSP metrics calculator for acoustic resonance evaluation.
 * Computes Peak Frequency, Spectral Centroid, Q-Factor, and Energy Decay Rate.
 */
class SpectralMetricsCalculator(
    private val sampleRate: Int = AudioConfig.SAMPLE_RATE,
    private val fftSize: Int = AudioConfig.FFT_SIZE
) {
    private val binResolution: Float = sampleRate.toFloat() / fftSize.toFloat() // 23.4375 Hz
    private val numBins: Int = fftSize / 2

    // Circular ring buffer for RMS tracking & decay rate linear regression
    private val historySize = 20
    private val rmsHistory = FloatArray(historySize)
    private val timeHistory = LongArray(historySize)
    private var historyIndex = 0
    private var historyCount = 0

    // Noise floor tracking
    var calibratedNoiseFloorDb: Float = AudioConfig.DEFAULT_NOISE_FLOOR_DB

    /**
     * Computes all acoustic metrics for a single FFT frame.
     *
     * @param magnitudes Linear magnitude spectrum (size >= numBins).
     * @param rawSamples Raw time-domain audio samples for RMS calculation.
     * @param timestampMs Current sample timestamp.
     */
    fun computeMetrics(
        magnitudes: FloatArray,
        rawSamples: FloatArray,
        timestampMs: Long = System.currentTimeMillis()
    ): AcousticMetrics {
        // 1. RMS & Decibel Level
        var sumSquares = 0.0f
        val sampleCount = rawSamples.size
        for (i in 0 until sampleCount) {
            val s = rawSamples[i]
            sumSquares += s * s
        }
        val rms = sqrt(max(1e-12f, sumSquares / sampleCount.toFloat()))
        val rmsDbfs = (20.0f * log10(rms)).coerceIn(-100.0f, 0.0f)
        val snrDb = max(0.0f, rmsDbfs - calibratedNoiseFloorDb)

        // Store RMS history
        rmsHistory[historyIndex] = rmsDbfs
        timeHistory[historyIndex] = timestampMs
        historyIndex = (historyIndex + 1) % historySize
        if (historyCount < historySize) historyCount++

        // 2. Find Dominant Peak (ignoring DC / sub-bass rumble < 60 Hz)
        val minBin = max(2, (60.0f / binResolution).toInt())
        val maxBin = (22000.0f / binResolution).toInt().coerceAtMost(numBins - 2)

        var peakBin = minBin
        var maxMag = 0.0f

        for (k in minBin..maxBin) {
            if (magnitudes[k] > maxMag) {
                maxMag = magnitudes[k]
                peakBin = k
            }
        }

        // Sub-bin parabolic quadratic peak interpolation
        var interpolatedPeakFreq = peakBin * binResolution
        if (peakBin > minBin && peakBin < maxBin && maxMag > 1e-6f) {
            val alpha = 20.0f * log10(magnitudes[peakBin - 1] + 1e-9f)
            val beta = 20.0f * log10(magnitudes[peakBin] + 1e-9f)
            val gamma = 20.0f * log10(magnitudes[peakBin + 1] + 1e-9f)

            val denom = alpha - 2.0f * beta + gamma
            if (denom < 0.0f) {
                val delta = 0.5f * (alpha - gamma) / denom
                interpolatedPeakFreq = (peakBin + delta.coerceIn(-1.0f, 1.0f)) * binResolution
            }
        }

        val peakMagDb = (20.0f * log10(maxMag + 1e-9f)).coerceIn(-100.0f, 0.0f)

        // 3. Spectral Centroid
        var weightedSum = 0.0f
        var totalMag = 0.0f
        for (k in 1 until numBins) {
            val mag = magnitudes[k]
            val freq = k * binResolution
            weightedSum += freq * mag
            totalMag += mag
        }
        val spectralCentroid = if (totalMag > 1e-8f) weightedSum / totalMag else 0.0f

        // 4. Q-Factor (Quality Factor: Q = f0 / delta_f_-3dB)
        val qFactor = computeQFactor(magnitudes, peakBin, interpolatedPeakFreq, maxMag)

        // 5. Energy Decay Rate (dB / sec) via linear regression over recent window
        val decayRate = computeDecayRate()

        // 6. Secondary Resonance Peaks (harmonic modes / void splits)
        val secondaryPeaks = findSecondaryPeaks(magnitudes, peakBin, minBin, maxBin, maxMag)

        return AcousticMetrics(
            peakFrequencyHz = interpolatedPeakFreq,
            peakMagnitudeDb = peakMagDb,
            spectralCentroidHz = spectralCentroid,
            qFactor = qFactor,
            energyDecayRateDbPerSec = decayRate,
            rmsDbfs = rmsDbfs,
            snrDb = snrDb,
            secondaryPeaksHz = secondaryPeaks
        )
    }

    /**
     * Calculates the Q-Factor (resonant sharpness) by finding the -3dB bandwidth around the peak.
     */
    private fun computeQFactor(
        magnitudes: FloatArray,
        peakBin: Int,
        peakFreq: Float,
        peakMag: Float
    ): Float {
        if (peakMag < 1e-6f || peakFreq < 80.0f) return 0.5f

        val halfPowerMag = peakMag * 0.70710678f // -3 dB amplitude

        // Find lower -3dB frequency with linear interpolation
        var fLow = peakFreq - binResolution
        for (k in peakBin - 1 downTo 1) {
            if (magnitudes[k] <= halfPowerMag) {
                val frac = if (magnitudes[k + 1] != magnitudes[k]) {
                    (halfPowerMag - magnitudes[k]) / (magnitudes[k + 1] - magnitudes[k])
                } else 0.5f
                fLow = (k + frac) * binResolution
                break
            }
        }

        // Find upper -3dB frequency with linear interpolation
        var fHigh = peakFreq + binResolution
        for (k in peakBin + 1 until numBins - 1) {
            if (magnitudes[k] <= halfPowerMag) {
                val frac = if (magnitudes[k - 1] != magnitudes[k]) {
                    (magnitudes[k - 1] - halfPowerMag) / (magnitudes[k - 1] - magnitudes[k])
                } else 0.5f
                fHigh = (k - 1 + frac) * binResolution
                break
            }
        }

        val bandwidth = max(binResolution * 0.5f, fHigh - fLow)
        val q = peakFreq / bandwidth
        return q.coerceIn(0.1f, 150.0f)
    }

    /**
     * Computes linear regression energy decay slope over recent frames (dB/sec).
     */
    private fun computeDecayRate(): Float {
        if (historyCount < 5) return 0.0f

        var sumT = 0.0
        var sumY = 0.0
        var sumTY = 0.0
        var sumT2 = 0.0

        val n = historyCount
        val startIdx = (historyIndex - n + historySize) % historySize
        val t0 = timeHistory[startIdx]

        for (i in 0 until n) {
            val idx = (startIdx + i) % historySize
            val tSec = (timeHistory[idx] - t0) / 1000.0 // time in seconds
            val y = rmsHistory[idx].toDouble()

            sumT += tSec
            sumY += y
            sumTY += tSec * y
            sumT2 += tSec * tSec
        }

        val denom = (n * sumT2 - sumT * sumT)
        if (denom == 0.0) return 0.0f

        val slope = (n * sumTY - sumT * sumY) / denom // dB per sec

        // If slope is negative, decay rate is positive magnitude
        return if (slope < 0) (-slope).toFloat().coerceIn(0.0f, 250.0f) else 0.0f
    }

    /**
     * Discovers secondary resonant peaks with prominence above neighbor threshold.
     */
    private fun findSecondaryPeaks(
        magnitudes: FloatArray,
        primaryPeakBin: Int,
        minBin: Int,
        maxBin: Int,
        primaryMag: Float
    ): List<Float> {
        val list = mutableListOf<Float>()
        val minSecondaryMag = primaryMag * 0.25f // at least 25% of primary peak (-12 dB)
        val minBinDist = (150.0f / binResolution).toInt() // minimum 150 Hz separation

        for (k in (minBin + 1) until (maxBin - 1)) {
            if (kotlin.math.abs(k - primaryPeakBin) < minBinDist) continue

            val mag = magnitudes[k]
            if (mag > minSecondaryMag && mag > magnitudes[k - 1] && mag > magnitudes[k + 1]) {
                list.add(k * binResolution)
                if (list.size >= 3) break
            }
        }
        return list
    }
}
