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
}
