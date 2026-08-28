package com.resonancelab.app.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * High-performance Radix-2 In-Place Fast Fourier Transform (FFT) engine.
 *
 * Implements the Cooley-Tukey algorithm with precomputed bit-reversal permutation
 * and trigonometric twiddle factor tables. Engineered for zero heap allocation
 * during 60+ FPS real-time audio DSP pipelines.
 *
 * @param size FFT length, must be a power of 2 (default 2048).
 */
class FastFourierTransform(val size: Int = 2048) {

    init {
        require(size > 0 && (size and (size - 1)) == 0) {
            "FFT size must be a power of 2, received: $size"
        }
    }

    val numMagnitudeBins: Int = size / 2

    // Precomputed bit-reversal table
    private val bitRevTable: IntArray = IntArray(size) { i ->
        var j = 0
        var temp = i
        var bits = 0
        var s = size
        while (s > 1) {
            bits++
            s = s shr 1
        }
        for (b in 0 until bits) {
            j = (j shl 1) or (temp and 1)
            temp = temp shr 1
        }
        j
    }

    // Precomputed twiddle factor tables
    // Cosine and Sine values for e^(-2*PI*i*k / L)
    private val cosTable: FloatArray = FloatArray(size / 2)
    private val sinTable: FloatArray = FloatArray(size / 2)

    // Dedicated reusable scratch buffers for zero-allocation streaming
    private val realScratch: FloatArray = FloatArray(size)
    private val imagScratch: FloatArray = FloatArray(size)
    private val windowedScratch: FloatArray = FloatArray(size)
    private val hannWindow: HannWindow = HannWindow(size)

    init {
        for (i in 0 until size / 2) {
            val angle = (-2.0 * PI * i / size).toFloat()
            cosTable[i] = cos(angle)
            sinTable[i] = sin(angle)
        }
    }

    /**
     * Executes the in-place Radix-2 FFT on real & imag arrays.
     * Arrays are permuted via bit-reversal and transformed through log2(N) butterfly stages.
     */
    fun transform(real: FloatArray, imag: FloatArray) {
        val n = size

        // 1. Bit-reversal permutation
        for (i in 0 until n) {
            val j = bitRevTable[i]
            if (i < j) {
                val tempR = real[i]
                real[i] = real[j]
                real[j] = tempR

                val tempI = imag[i]
                imag[i] = imag[j]
                imag[j] = tempI
            }
        }

        // 2. Butterfly stages (Cooley-Tukey)
        var len = 2
        while (len <= n) {
            val halfLen = len / 2
            val step = n / len

            var k = 0
            while (k < n) {
                var tableIdx = 0
                for (j in 0 until halfLen) {
                    val uR = cosTable[tableIdx]
                    val uI = sinTable[tableIdx]

                    val posA = k + j
                    val posB = k + j + halfLen

                    val tR = uR * real[posB] - uI * imag[posB]
                    val tI = uR * imag[posB] + uI * real[posB]

                    real[posB] = real[posA] - tR
                    imag[posB] = imag[posA] - tI
                    real[posA] += tR
                    imag[posA] += tI

                    tableIdx += step
                }
                k += len
            }
            len = len shl 1
        }
    }

    /**
     * Processes a windowed frame of audio samples and computes the linear magnitude spectrum.
     *
     * @param inputSamples Array of at least [size] float samples in range [-1.0, 1.0].
     * @param inputOffset Starting index in [inputSamples].
     * @param outMagnitudes Destination array of size at least [numMagnitudeBins].
     */
    fun computeMagnitudeSpectrum(
        inputSamples: FloatArray,
        inputOffset: Int = 0,
        outMagnitudes: FloatArray
    ) {
        // Apply Hann window into scratch buffer
        hannWindow.apply(inputSamples, inputOffset, windowedScratch)

        // Copy windowed samples into real scratch, clear imag scratch
        for (i in 0 until size) {
            realScratch[i] = windowedScratch[i]
            imagScratch[i] = 0.0f
        }

        // Perform FFT in place
        transform(realScratch, imagScratch)

        // Compute magnitude: 2.0 / N * sqrt(re^2 + im^2) for single-sided spectrum
        val normFactor = 2.0f / size.toFloat()
        val numBins = numMagnitudeBins

        // DC component
        outMagnitudes[0] = sqrt(realScratch[0] * realScratch[0] + imagScratch[0] * imagScratch[0]) / size.toFloat()

        for (k in 1 until numBins) {
            val r = realScratch[k]
            val im = imagScratch[k]
            outMagnitudes[k] = sqrt(r * r + im * im) * normFactor
        }
    }

    /**
     * Computes decibel magnitude spectrum scaled between 0.0f and 1.0f for visualization.
     *
     * @param inputSamples Audio samples.
     * @param inputOffset Sample offset.
     * @param outNormalizedDb Scaled output [0.0 = minDb, 1.0 = maxDb].
     * @param minDb Dynamic range floor (e.g. -90 dBFS).
     * @param maxDb Ceiling (e.g. 0 dBFS).
     */
    fun computeNormalizedDbSpectrum(
        inputSamples: FloatArray,
        inputOffset: Int = 0,
        outNormalizedDb: FloatArray,
        minDb: Float = -90.0f,
        maxDb: Float = 0.0f
    ) {
        computeMagnitudeSpectrum(inputSamples, inputOffset, outNormalizedDb)
        convertMagnitudesToNormalizedDb(outNormalizedDb, outNormalizedDb, minDb, maxDb)
    }

    /**
     * Converts a precomputed linear magnitude spectrum to a normalized [0.0f..1.0f] decibel spectrum.
     */
    fun convertMagnitudesToNormalizedDb(
        linearMagnitudes: FloatArray,
        outNormalizedDb: FloatArray,
        minDb: Float = -90.0f,
        maxDb: Float = 0.0f
    ) {
        val dbRange = maxDb - minDb
        val numBins = numMagnitudeBins.coerceAtMost(linearMagnitudes.size).coerceAtMost(outNormalizedDb.size)
        val eps = 1e-7f

        for (k in 0 until numBins) {
            val mag = linearMagnitudes[k]
            val db = 20.0f * log10(mag + eps)
            val normalized = (db - minDb) / dbRange
            outNormalizedDb[k] = normalized.coerceIn(0.0f, 1.0f)
        }
    }
}
