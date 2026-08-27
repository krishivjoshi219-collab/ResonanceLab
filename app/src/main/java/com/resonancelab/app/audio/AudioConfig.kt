package com.resonancelab.app.audio

/**
 * Global audio and DSP pipeline configuration constants.
 */
object AudioConfig {
    /** Target sample rate for acoustic capture (48 kHz) */
    const val SAMPLE_RATE: Int = 48000

    /** FFT window size (Power of 2 for Radix-2 FFT) */
    const val FFT_SIZE: Int = 2048

    /** Hop size between consecutive FFT frames (~93.75 frames/sec at 48kHz for fluid 60+ FPS visualization) */
    const val HOP_SIZE: Int = 512

    /** Maximum Nyquist frequency in Hz */
    const val NYQUIST_FREQ: Float = SAMPLE_RATE / 2.0f // 24000 Hz

    /** Frequency bin resolution in Hz per bin */
    const val BIN_RESOLUTION: Float = SAMPLE_RATE.toFloat() / FFT_SIZE.toFloat() // 23.4375 Hz

    /** Total number of useful magnitude bins (0 Hz to 24 kHz) */
    const val NUM_MAGNITUDE_BINS: Int = FFT_SIZE / 2 // 1024 bins

    /** Dynamic range floor for decibel scaling (dBFS) */
    const val MIN_DB: Float = -90.0f
    const val MAX_DB: Float = 0.0f

    /** Default noise floor threshold (dBFS) */
    const val DEFAULT_NOISE_FLOOR_DB: Float = -65.0f

    /** Speed of sound in air at 20°C (m/s) */
    const val SPEED_OF_SOUND_AIR_M_S: Float = 343.2f

    /** Sonar chirp parameters */
    const val SONAR_DEFAULT_START_FREQ: Float = 4000.0f
    const val SONAR_DEFAULT_END_FREQ: Float = 16000.0f
    const val SONAR_CHIRP_DURATION_MS: Int = 25
}
