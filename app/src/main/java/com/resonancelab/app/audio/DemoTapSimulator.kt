package com.resonancelab.app.audio

import com.resonancelab.app.dsp.AcousticAnalysisResult
import com.resonancelab.app.dsp.AcousticMetrics
import com.resonancelab.app.dsp.MaterialClassification
import com.resonancelab.app.dsp.MaterialType
import kotlin.math.exp
import kotlin.random.Random

/**
 * Judge-proof demo synthesizer — the standout hook.
 *
 * No mic? No problem. One tap plays a realistic hollow / solid / void
 * tap so judges on emulator, in a noisy room, or in a 60s video
 * always see magic in <2s. Pure Kotlin, no audio I/O, fully testable.
 *
 * Spectra are Gaussian peaks at characteristic physics frequencies:
 * hollow = sharp high-Q ring ~1840 Hz, solid = damped low thud ~420 Hz,
 * void = split modes ~980 + 1420 Hz.
 */
object DemoTapSimulator {

    enum class DemoTapType(val label: String, val emoji: String) {
        HOLLOW_DOOR("Hollow door", "\uD83D\uDEAA"),
        SOLID_WALL("Solid wall", "\uD83E\uDDF1"),
        VOID_TILE("Void tile", "\uD83D\uDD73\uFE0F")
    }

    fun synthesize(type: DemoTapType, seed: Long? = null): AcousticAnalysisResult {
        val rng = if (seed != null) Random(seed) else Random(System.currentTimeMillis())
        val now = System.currentTimeMillis()
        val numBins = AudioConfig.NUM_MAGNITUDE_BINS
        val binRes = AudioConfig.BIN_RESOLUTION

        val normalized = FloatArray(numBins)
        val raw = FloatArray(numBins)

        fun splat(peakHz: Float, height: Float, widthBins: Float) {
            val peakBin = (peakHz / binRes).toInt().coerceIn(0, numBins - 1)
            val w = widthBins.coerceAtLeast(1.5f)
            for (i in 0 until numBins) {
                val d = (i - peakBin) / w
                val g = exp(-0.5f * d * d) * height
                normalized[i] = (normalized[i] + g).coerceAtMost(1.0f)
                raw[i] += g * 1200f
            }
        }

        // Floor noise so waterfall looks alive
        for (i in 0 until numBins) {
            val floor = 0.03f + rng.nextFloat() * 0.03f
            normalized[i] = floor
            raw[i] = floor * 80f
        }

        val (metrics, classification) = when (type) {
            DemoTapType.HOLLOW_DOOR -> {
                val peak = 1840f + rng.nextFloat() * 120f - 60f
                splat(peak, 1.0f, 3.0f)
                splat(peak * 2.02f, 0.35f, 4.0f)
                splat(peak * 2.94f, 0.18f, 5.0f)
                val m = AcousticMetrics(
                    peakFrequencyHz = peak,
                    peakMagnitudeDb = -14f + rng.nextFloat() * 3f,
                    spectralCentroidHz = peak * 1.35f,
                    qFactor = 13.5f + rng.nextFloat() * 2.5f,
                    energyDecayRateDbPerSec = 21f + rng.nextFloat() * 5f,
                    rmsDbfs = -22f + rng.nextFloat() * 4f,
                    snrDb = 22f + rng.nextFloat() * 6f
                )
                val c = MaterialClassification(
                    type = MaterialType.HOLLOW_CAVITY,
                    confidence = 0.91f + rng.nextFloat() * 0.06f,
                    primaryReason = "Sharp ring at ${peak.toInt()} Hz, Q 14.2, slow 22 dB/s decay — classic cavity sustain",
                    acousticSignature = "Cavity mode demo"
                )
                m to c
            }
            DemoTapType.SOLID_WALL -> {
                val peak = 420f + rng.nextFloat() * 80f - 40f
                splat(peak, 1.0f, 9.0f)
                splat(peak * 1.8f, 0.22f, 10.0f)
                val m = AcousticMetrics(
                    peakFrequencyHz = peak,
                    peakMagnitudeDb = -18f + rng.nextFloat() * 3f,
                    spectralCentroidHz = 640f + rng.nextFloat() * 120f,
                    qFactor = 3.8f + rng.nextFloat() * 1.2f,
                    energyDecayRateDbPerSec = 52f + rng.nextFloat() * 10f,
                    rmsDbfs = -26f + rng.nextFloat() * 4f,
                    snrDb = 18f + rng.nextFloat() * 5f
                )
                val c = MaterialClassification(
                    type = MaterialType.SOLID_SUBSTRATE,
                    confidence = 0.89f + rng.nextFloat() * 0.06f,
                    primaryReason = "Damped thud at ${peak.toInt()} Hz, Q 4.1, fast 55 dB/s decay — solid substrate",
                    acousticSignature = "Solid substrate demo"
                )
                m to c
            }
            DemoTapType.VOID_TILE -> {
                val p1 = 980f + rng.nextFloat() * 60f - 30f
                val p2 = 1420f + rng.nextFloat() * 80f - 40f
                splat(p1, 0.9f, 4.5f)
                splat(p2, 0.75f, 4.5f)
                splat((p1 + p2) / 2f, 0.3f, 7.0f)
                val m = AcousticMetrics(
                    peakFrequencyHz = p1,
                    peakMagnitudeDb = -16f + rng.nextFloat() * 3f,
                    spectralCentroidHz = 1180f + rng.nextFloat() * 100f,
                    qFactor = 8.5f + rng.nextFloat() * 1.5f,
                    energyDecayRateDbPerSec = 34f + rng.nextFloat() * 5f,
                    rmsDbfs = -24f + rng.nextFloat() * 4f,
                    snrDb = 19f + rng.nextFloat() * 5f,
                    secondaryPeaksHz = listOf(p2)
                )
                val c = MaterialClassification(
                    type = MaterialType.BOUNDARY_VOID,
                    confidence = 0.86f + rng.nextFloat() * 0.06f,
                    primaryReason = "Split modes ${p1.toInt()} + ${p2.toInt()} Hz — boundary delamination",
                    acousticSignature = "Delamination void demo"
                )
                m to c
            }
        }

        return AcousticAnalysisResult(
            timestampMs = now,
            metrics = metrics,
            classification = classification,
            normalizedMagnitudes = normalized,
            rawMagnitudes = raw,
            isImpactDetected = true
        )
    }

    /** One-line viral share text for #Shipaton #BuildInPublic. */
    fun shareLine(result: AcousticAnalysisResult): String {
        val c = result.classification
        val m = result.metrics
        return "I tapped my wall with ResonanceLab: ${c.type.label} " +
            "${(c.confidence * 100).toInt()}% — " +
            "${m.peakFrequencyHz.toInt()}Hz Q${"%.1f".format(m.qFactor)} " +
            "#Shipaton #BuildInPublic"
    }
}
