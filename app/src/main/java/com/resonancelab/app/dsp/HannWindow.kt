package com.resonancelab.app.dsp

import kotlin.math.PI
import kotlin.math.cos

/**
 * Precomputed Hann (Hanning) windowing function for spectral analysis.
 * Reduces spectral leakage in Radix-2 FFT operations.
 *
 * Formula: w[n] = 0.5 * (1 - cos(2 * PI * n / (N - 1)))
 */
class HannWindow(val size: Int = 2048) {

    private val coefficients: FloatArray = FloatArray(size) { i ->
        (0.5 * (1.0 - cos(2.0 * PI * i / (size - 1)))).toFloat()
    }

    /**
     * Applies the Hann window to [input] and writes the weighted samples into [output].
     * Avoids array allocations by mutating [output] in place.
     *
     * @param input Raw audio sample array (size >= [size])
     * @param inputOffset Offset inside [input]
     * @param output Destination array (size >= [size])
     */
    fun apply(
        input: FloatArray,
        inputOffset: Int = 0,
        output: FloatArray
    ) {
        val len = size
        val coeffs = coefficients
        for (i in 0 until len) {
            output[i] = input[inputOffset + i] * coeffs[i]
        }
    }

    /**
     * In-place windowing on a single buffer.
     */
    fun applyInPlace(buffer: FloatArray, offset: Int = 0) {
        val len = size
        val coeffs = coefficients
        for (i in 0 until len) {
            buffer[offset + i] *= coeffs[i]
        }
    }
}
