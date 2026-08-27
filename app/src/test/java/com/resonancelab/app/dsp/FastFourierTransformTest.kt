package com.resonancelab.app.dsp

import com.resonancelab.app.audio.AudioConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class FastFourierTransformTest {

    @Test
    fun testRadix2FftDetectsPureSineWave() {
        val fft = FastFourierTransform(2048)
        val sampleRate = AudioConfig.SAMPLE_RATE // 48000
        val targetFreq = 1200.0f // 1200 Hz
        val numSamples = 2048

        // Synthesize 1200 Hz pure sine wave
        val samples = FloatArray(numSamples) { i ->
            val t = i.toFloat() / sampleRate.toFloat()
            sin(2.0 * PI * targetFreq * t).toFloat()
        }

        val magnitudes = FloatArray(fft.numMagnitudeBins)
        fft.computeMagnitudeSpectrum(samples, 0, magnitudes)

        // Find peak bin
        var maxBin = 0
        var maxMag = 0.0f
        for (i in magnitudes.indices) {
            if (magnitudes[i] > maxMag) {
                maxMag = magnitudes[i]
                maxBin = i
            }
        }

        val detectedFreq = maxBin * AudioConfig.BIN_RESOLUTION
        // Expected bin for 1200 Hz: 1200 / 23.4375 ≈ 51.2
        assertEquals(1200.0f, detectedFreq, 30.0f)
        assertTrue("Magnitude should be significantly above zero", maxMag > 0.3f)
    }

    @Test
    fun testHannWindowEnergyConservation() {
        val window = HannWindow(2048)
        val input = FloatArray(2048) { 1.0f }
        val output = FloatArray(2048)
        window.apply(input, 0, output)

        // Hann window middle sample should be 1.0
        assertEquals(1.0f, output[1024], 0.01f)
        // Edges should be 0.0
        assertEquals(0.0f, output[0], 0.001f)
    }
}
