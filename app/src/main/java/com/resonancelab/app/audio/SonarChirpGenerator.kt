package com.resonancelab.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import com.resonancelab.app.dsp.SonarEchoResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Active Sonar Chirp Synthesizer & Matched Filter Engine (Pro Tier).
 *
 * Emits a Linear Frequency Modulated (LFM) acoustic sweep pulse via [AudioTrack]
 * and computes cross-correlation against the captured microphone audio buffer
 * to calculate acoustic Time-of-Flight (ToF) and target echo distance.
 */
class SonarChirpGenerator(
    private val sampleRate: Int = AudioConfig.SAMPLE_RATE
) {
    private var isPlaying = false
    private var lastEmitTimestampMs: Long = 0L

    // Chirp reference signal buffer for matched filtering
    var chirpStartFreq: Float = AudioConfig.SONAR_DEFAULT_START_FREQ
    var chirpEndFreq: Float = AudioConfig.SONAR_DEFAULT_END_FREQ
    var chirpDurationMs: Int = AudioConfig.SONAR_CHIRP_DURATION_MS

    private var cachedChirpSamples: FloatArray = FloatArray(0)
    private var cachedPcm16Buffer: ShortArray = ShortArray(0)

    init {
        rebuildChirpSignal()
    }

    /**
     * Synthesizes the LFM chirp reference signal with Tukey/Hann edge tapering.
     */
    fun rebuildChirpSignal() {
        val numSamples = (sampleRate * (chirpDurationMs / 1000.0f)).toInt()
        val floatBuf = FloatArray(numSamples)
        val shortBuf = ShortArray(numSamples)

        val tTotal = chirpDurationMs / 1000.0f
        val f0 = chirpStartFreq
        val f1 = chirpEndFreq
        val k = (f1 - f0) / tTotal // Frequency sweep rate

        // Window edge taper length (10% on each side)
        val taperLen = (numSamples * 0.10f).toInt().coerceAtLeast(1)

        for (n in 0 until numSamples) {
            val t = n.toFloat() / sampleRate.toFloat()
            // Phase phi(t) = 2 * PI * (f0 * t + 0.5 * k * t^2)
            val phase = 2.0 * PI * (f0 * t + 0.5 * k * t * t)
            var sample = sin(phase).toFloat()

            // Apply taper window to edges
            if (n < taperLen) {
                val w = (0.5 * (1.0 - cos(PI * n / taperLen))).toFloat()
                sample *= w
            } else if (n > numSamples - taperLen) {
                val w = (0.5 * (1.0 - cos(PI * (numSamples - n) / taperLen))).toFloat()
                sample *= w
            }

            floatBuf[n] = sample
            shortBuf[n] = (sample * 32000.0f).toInt().coerceIn(-32768, 32767).toShort()
        }

        cachedChirpSamples = floatBuf
        cachedPcm16Buffer = shortBuf
    }

    /**
     * Plays the acoustic chirp pulse asynchronously via AudioTrack.
     */
    suspend fun emitChirpPulse(): Long = withContext(Dispatchers.IO) {
        if (isPlaying) return@withContext lastEmitTimestampMs

        if (cachedPcm16Buffer.isEmpty()) {
            rebuildChirpSignal()
        }

        isPlaying = true
        val pcm = cachedPcm16Buffer
        val bufferSize = pcm.size * 2

        var track: AudioTrack? = null
        try {
            track = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize,
                    AudioTrack.MODE_STATIC
                )
            }

            track.write(pcm, 0, pcm.size)
            track.play()
            lastEmitTimestampMs = System.currentTimeMillis()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isPlaying = false
            try {
                track?.release()
            } catch (_: Exception) {}
        }

        lastEmitTimestampMs
    }

    /**
     * Matched filter cross-correlation: correlates recorded input buffer against the reference chirp.
     * Computes acoustic Time of Flight and distance in centimeters.
     */
    fun processMatchedFilter(
        inputSamples: FloatArray,
        inputSize: Int
    ): SonarEchoResult {
        val chirp = cachedChirpSamples
        val chirpLen = chirp.size
        if (chirpLen == 0 || inputSize <= chirpLen) {
            return SonarEchoResult()
        }

        // Direct cross-correlation: R[d] = sum(input[d + n] * chirp[n])
        val maxDelay = inputSize - chirpLen
        var maxCorr = 0.0f
        var bestDelayIndex = 0

        // Calculate energy of chirp
        var chirpEnergy = 0.0f
        for (i in 0 until chirpLen) {
            chirpEnergy += chirp[i] * chirp[i]
        }
        val chirpNorm = sqrt(max(1e-9f, chirpEnergy))

        // Skip initial direct speaker-to-mic bleed (first 2-3 ms)
        val minSkipSamples = (sampleRate * 0.003f).toInt()

        for (d in minSkipSamples until maxDelay step 2) {
            var dot = 0.0f
            var sigEnergy = 0.0f
            for (n in 0 until chirpLen) {
                val s = inputSamples[d + n]
                dot += s * chirp[n]
                sigEnergy += s * s
            }

            val norm = sqrt(max(1e-9f, sigEnergy)) * chirpNorm
            val normalizedCorr = if (norm > 1e-6f) (dot / norm) else 0.0f

            if (normalizedCorr > maxCorr) {
                maxCorr = normalizedCorr
                bestDelayIndex = d
            }
        }

        // Time of Flight in milliseconds: ToF = delay_samples / sample_rate * 1000
        val tofMs = (bestDelayIndex.toFloat() / sampleRate.toFloat()) * 1000.0f

        // Round-trip distance d = (v_air * ToF) / 2
        val distanceMeters = (AudioConfig.SPEED_OF_SOUND_AIR_M_S * (tofMs / 1000.0f)) / 2.0f
        val distanceCm = (distanceMeters * 100.0f).coerceIn(0.0f, 500.0f)
        val confidence = maxCorr.coerceIn(0.0f, 1.0f)

        return SonarEchoResult(
            timeOfFlightMs = tofMs,
            estimatedDistanceCm = distanceCm,
            echoConfidence = confidence,
            rawCorrelationPeak = maxCorr,
            timestampMs = System.currentTimeMillis()
        )
    }

    private fun max(a: Float, b: Float): Float = kotlin.math.max(a, b)
}
