package com.resonancelab.app.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.resonancelab.app.dsp.AcousticAnalysisResult
import com.resonancelab.app.dsp.FastFourierTransform
import com.resonancelab.app.dsp.LiquidLevelEstimator
import com.resonancelab.app.dsp.MaterialClassification
import com.resonancelab.app.dsp.MaterialClassifier
import com.resonancelab.app.dsp.MaterialType
import com.resonancelab.app.dsp.SpectralMetricsCalculator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Native Audio Engine managing [AudioRecord] capture, background DSP thread execution,
 * sliding-window FFT processing, and real-time acoustic metric extraction.
 */
class AudioRecordManager(
    private val fftEngine: FastFourierTransform = FastFourierTransform(AudioConfig.FFT_SIZE),
    val metricsCalculator: SpectralMetricsCalculator = SpectralMetricsCalculator(),
    val classifier: MaterialClassifier = MaterialClassifier(),
    val liquidEstimator: LiquidLevelEstimator = LiquidLevelEstimator(),
    val sonarGenerator: SonarChirpGenerator = SonarChirpGenerator()
) {
    private var audioRecord: AudioRecord? = null
    private var captureJob: Job? = null
    private val isRecording = AtomicBoolean(false)

    // StateFlow for high-level UI telemetry
    private val _analysisState = MutableStateFlow(AcousticAnalysisResult())
    val analysisState: StateFlow<AcousticAnalysisResult> = _analysisState.asStateFlow()

    // SharedFlow emitting 1024 normalized FFT bins for high-framerate waterfall spectrogram rendering
    private val _spectrogramFlow = MutableSharedFlow<FloatArray>(
        extraBufferCapacity = 64,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    val spectrogramFlow: SharedFlow<FloatArray> = _spectrogramFlow.asSharedFlow()

    // Flag for active sonar trigger
    private val activeSonarPending = AtomicBoolean(false)

    // Dedicated circular pre-allocated audio sample buffers (Zero GC thrashing)
    private val windowSize = AudioConfig.FFT_SIZE // 2048
    private val hopSize = AudioConfig.HOP_SIZE     // 512
    private val numBins = AudioConfig.NUM_MAGNITUDE_BINS // 1024

    private val ringBufferSize = 16384
    private val ringBuffer = FloatArray(ringBufferSize)
    private var ringWriteHead = 0
    private var ringAvailableSamples = 0

    // Reusable arrays for FFT and metrics extraction
    private val fftInputWindow = FloatArray(windowSize)
    private val normalizedDbSpectrum = FloatArray(numBins)
    private val rawMagnitudeSpectrum = FloatArray(numBins)
    private val broadcastSpectrum = FloatArray(numBins)

    // Calibration and sensitivity
    private var sensitivityMultiplier: Float = 1.0f

    /**
     * Starts acoustic capture on a dedicated background Dispatcher.
     */
    @SuppressLint("MissingPermission")
    fun startCapture(scope: CoroutineScope) {
        if (isRecording.getAndSet(true)) return

        captureJob = scope.launch(Dispatchers.Default) {
            try {
                runAudioCaptureLoop()
            } catch (e: CancellationException) {
                // Expected when stopping
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                releaseAudioRecord()
                isRecording.set(false)
            }
        }
    }

    /**
     * Stops acoustic capture and cancels background processing.
     */
    fun stopCapture() {
        isRecording.set(false)
        captureJob?.cancel()
        captureJob = null
        releaseAudioRecord()
    }

    fun isCapturing(): Boolean = isRecording.get()

    /**
     * Calibrates the environmental noise floor from ambient levels.
     */
    fun calibrateNoiseFloor(currentRmsDbfs: Float) {
        metricsCalculator.calibratedNoiseFloorDb = currentRmsDbfs.coerceIn(-90.0f, -30.0f)
    }

    /**
     * Sets sensitivity multiplier (0.5x to 3.0x).
     */
    fun setSensitivity(multiplier: Float) {
        sensitivityMultiplier = multiplier.coerceIn(0.2f, 5.0f)
    }

    /**
     * Triggers active sonar chirp pulse execution.
     */
    fun triggerSonarChirp(scope: CoroutineScope) {
        activeSonarPending.set(true)
        scope.launch(Dispatchers.IO) {
            sonarGenerator.emitChirpPulse()
        }
    }

    /**
     * The core DSP loop executing on Dispatchers.Default.
     */
    @SuppressLint("MissingPermission")
    private suspend fun runAudioCaptureLoop() = withContext(Dispatchers.Default) {
        val sampleRate = AudioConfig.SAMPLE_RATE
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT

        val minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val readChunkSize = hopSize.coerceAtLeast(minBufSize / 2)
        val pcmReadBuffer = ShortArray(readChunkSize)

        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            minBufSize.coerceAtLeast(readChunkSize * 4)
        )

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            return@withContext
        }

        audioRecord = record
        record.startRecording()

        var lastSonarCheckTime = System.currentTimeMillis()

        while (isActive && isRecording.get()) {
            val readCount = record.read(pcmReadBuffer, 0, readChunkSize)
            if (readCount <= 0) continue

            // Write PCM16 samples normalized to [-1.0f, 1.0f] into circular ring buffer
            for (i in 0 until readCount) {
                ringBuffer[ringWriteHead] = (pcmReadBuffer[i] / 32768.0f) * sensitivityMultiplier
                ringWriteHead = (ringWriteHead + 1) % ringBufferSize
            }
            ringAvailableSamples += readCount

            // Process sliding FFT windows when enough new samples arrive (hop size)
            while (ringAvailableSamples >= windowSize) {
                // Extract contiguous window of 2048 samples from circular buffer
                val readStart = (ringWriteHead - ringAvailableSamples + ringBufferSize) % ringBufferSize
                for (i in 0 until windowSize) {
                    val idx = (readStart + i) % ringBufferSize
                    fftInputWindow[i] = ringBuffer[idx]
                }

                val now = System.currentTimeMillis()

                // Compute linear magnitude spectrum
                fftEngine.computeMagnitudeSpectrum(
                    inputSamples = fftInputWindow,
                    inputOffset = 0,
                    outMagnitudes = rawMagnitudeSpectrum
                )

                // Compute normalized 0.0..1.0 dB spectrum for visualizer
                fftEngine.computeNormalizedDbSpectrum(
                    inputSamples = fftInputWindow,
                    inputOffset = 0,
                    outNormalizedDb = normalizedDbSpectrum,
                    minDb = AudioConfig.MIN_DB,
                    maxDb = AudioConfig.MAX_DB
                )

                // Compute DSP metrics
                val metrics = metricsCalculator.computeMetrics(
                    magnitudes = rawMagnitudeSpectrum,
                    rawSamples = fftInputWindow,
                    timestampMs = now
                )

                // Evaluate Material Classification
                val classification = classifier.classify(metrics)

                // Evaluate Liquid Level
                val liquidLevel = liquidEstimator.estimate(metrics.peakFrequencyHz, metrics)

                // Handle Active Sonar Echo if triggered
                var sonarEcho = _analysisState.value.sonarEcho
                if (activeSonarPending.getAndSet(false) || (now - lastSonarCheckTime > 300 && sonarEcho != null)) {
                    val echo = sonarGenerator.processMatchedFilter(fftInputWindow, windowSize)
                    if (echo.echoConfidence > 0.35f) {
                        sonarEcho = echo
                        lastSonarCheckTime = now
                    }
                }

                val isImpact = classification.type != MaterialType.AMBIENT_NOISE && metrics.snrDb >= 10.0f

                // Copy spectrum values to immutable array for emission
                System.arraycopy(normalizedDbSpectrum, 0, broadcastSpectrum, 0, numBins)

                // Update UI state
                _analysisState.value = AcousticAnalysisResult(
                    timestampMs = now,
                    metrics = metrics,
                    classification = classification,
                    normalizedMagnitudes = broadcastSpectrum.copyOf(),
                    rawMagnitudes = rawMagnitudeSpectrum.copyOf(),
                    isImpactDetected = isImpact,
                    sonarEcho = sonarEcho,
                    liquidLevel = liquidLevel
                )

                // Stream spectrum to waterfall canvas
                _spectrogramFlow.tryEmit(broadcastSpectrum.copyOf())

                // Advance by hopSize (512 samples)
                ringAvailableSamples -= hopSize
            }
        }
    }

    private fun releaseAudioRecord() {
        try {
            audioRecord?.let {
                if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    it.stop()
                }
                it.release()
            }
        } catch (_: Exception) {
        } finally {
            audioRecord = null
        }
    }
}
