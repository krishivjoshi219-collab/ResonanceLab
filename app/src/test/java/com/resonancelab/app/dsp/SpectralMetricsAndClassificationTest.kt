package com.resonancelab.app.dsp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpectralMetricsAndClassificationTest {

    @Test
    fun testMaterialClassificationHollowCavity() {
        val classifier = MaterialClassifier()
        val metrics = AcousticMetrics(
            peakFrequencyHz = 2400.0f,
            peakMagnitudeDb = -18.0f,
            spectralCentroidHz = 2600.0f,
            qFactor = 22.0f,
            energyDecayRateDbPerSec = 14.0f,
            rmsDbfs = -22.0f,
            snrDb = 28.0f
        )

        val classification = classifier.classify(metrics)
        assertEquals(MaterialType.HOLLOW_CAVITY, classification.type)
        assertTrue("Confidence should be high (> 70%)", classification.confidence >= 0.70f)
    }

    @Test
    fun testMaterialClassificationSolidSubstrate() {
        val classifier = MaterialClassifier()
        val metrics = AcousticMetrics(
            peakFrequencyHz = 450.0f,
            peakMagnitudeDb = -20.0f,
            spectralCentroidHz = 620.0f,
            qFactor = 3.2f,
            energyDecayRateDbPerSec = 65.0f,
            rmsDbfs = -24.0f,
            snrDb = 25.0f
        )

        val classification = classifier.classify(metrics)
        assertEquals(MaterialType.SOLID_SUBSTRATE, classification.type)
        assertTrue("Confidence should be high (> 70%)", classification.confidence >= 0.70f)
    }

    @Test
    fun testLiquidLevelEstimation() {
        val estimator = LiquidLevelEstimator()
        estimator.totalHeightCm = 30.0f

        // Empty container air resonance f = 34320 / (4 * 30) = 286 Hz
        // As liquid fills to 15cm (half full), air column is 15cm -> f = 34320 / (4 * 15) = 572 Hz
        val metrics = AcousticMetrics(
            peakFrequencyHz = 572.0f,
            qFactor = 12.0f,
            rmsDbfs = -20.0f,
            snrDb = 25.0f
        )

        val result = estimator.estimate(572.0f, metrics)
        // Fill percentage should be approximately 50%
        assertEquals(50.0f, result.fillPercentage, 5.0f)
        assertEquals(15.0f, result.liquidHeightCm, 1.5f)
    }

    @Test
    fun testLiquidLevelEstimationZeroOrNegativeContainerHeight() {
        val estimator = LiquidLevelEstimator()
        estimator.totalHeightCm = 0.0f

        val metrics = AcousticMetrics(
            peakFrequencyHz = 572.0f,
            qFactor = 12.0f,
            rmsDbfs = -20.0f,
            snrDb = 25.0f
        )

        val result = estimator.estimate(572.0f, metrics)
        // Should fallback safely to default 30cm container height without divide-by-zero or crash
        assertEquals(30.0f, result.totalContainerHeightCm, 0.01f)
        assertTrue("Fill percentage should be between 0 and 100%", result.fillPercentage in 0.0f..100.0f)
    }

    @Test
    fun testMaterialClassificationBoundaryVoid() {
        val classifier = MaterialClassifier()
        val metrics = AcousticMetrics(
            peakFrequencyHz = 1200.0f,
            peakMagnitudeDb = -15.0f,
            spectralCentroidHz = 1500.0f,
            qFactor = 8.5f,
            energyDecayRateDbPerSec = 25.0f,
            rmsDbfs = -20.0f,
            snrDb = 22.0f,
            secondaryPeaksHz = listOf(1450.0f, 1800.0f)
        )

        val classification = classifier.classify(metrics)
        assertEquals(MaterialType.BOUNDARY_VOID, classification.type)
        assertTrue("Confidence should be above 50%", classification.confidence >= 0.50f)
    }

    @Test
    fun testQFactorComputationAccuracy() {
        val calculator = SpectralMetricsCalculator(sampleRate = 48000, fftSize = 2048)
        val binRes = 48000.0f / 2048.0f // 23.4375 Hz
        val numBins = 1024
        val magnitudes = FloatArray(numBins)

        val peakBin = 50 // ~1171.875 Hz
        magnitudes[peakBin] = 1.0f
        magnitudes[peakBin - 1] = 0.8f
        magnitudes[peakBin + 1] = 0.8f
        magnitudes[peakBin - 2] = 0.5f
        magnitudes[peakBin + 2] = 0.5f

        val samples = FloatArray(2048) { 0.1f }
        val metrics = calculator.computeMetrics(magnitudes, samples)
        assertTrue("Q-factor should be computed cleanly", metrics.qFactor > 1.0f)
        assertTrue("Peak frequency should be near peakBin * binRes", metrics.peakFrequencyHz > 1000.0f)
    }
}
