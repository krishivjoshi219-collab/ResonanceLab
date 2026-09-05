package com.resonancelab.app.dsp

/**
 * Common materials and their approximate longitudinal speed of sound in m/s.
 */
enum class PresetMaterial(val label: String, val speedOfSoundMS: Float) {
    STEEL("Steel / Iron", 5900f),
    ALUMINUM("Aluminum", 6320f),
    CONCRETE("Concrete", 4000f),
    GLASS("Glass", 5640f),
    PINE_WOOD("Pine Wood (Along grain)", 3300f),
    OAK_WOOD("Oak Wood (Along grain)", 3800f),
    PVC_PLASTIC("PVC", 2395f),
    ICE("Ice", 3980f)
}

/**
 * Thickness estimation result.
 */
data class ThicknessResult(
    val material: PresetMaterial = PresetMaterial.STEEL,
    val estimatedThicknessCm: Float = 0.0f,
    val fundamentalFreqHz: Float = 0.0f,
    val confidence: Float = 0.0f,
    val timestampMs: Long = System.currentTimeMillis()
)

/**
 * Acoustic Material Thickness Estimator (Pro Feature).
 *
 * Estimates structural thickness by analyzing the dominant through-thickness
 * resonance (longitudinal wave) fundamental frequency.
 *
 * Physics Model:
 * f0 = v / (2 * d)
 * -> d = v / (2 * f0)
 * where v is speed of sound in the material, d is thickness, f0 is fundamental frequency.
 */
class MaterialThicknessEstimator {

    var selectedMaterial: PresetMaterial = PresetMaterial.STEEL

    fun estimate(peakFreqHz: Float, metrics: AcousticMetrics): ThicknessResult {
        if (peakFreqHz < 80.0f || metrics.rmsDbfs < -75.0f || metrics.snrDb < 6.0f) {
            return ThicknessResult(
                material = selectedMaterial,
                estimatedThicknessCm = 0.0f,
                fundamentalFreqHz = peakFreqHz,
                confidence = 0.0f
            )
        }

        // Speed of sound in cm/s
        val vCmS = selectedMaterial.speedOfSoundMS * 100f

        // Thickness in cm: d = v / (2 * f0)
        val thicknessCm = vCmS / (2.0f * peakFreqHz)

        // Calculate confidence
        val qScore = (metrics.qFactor / 20.0f).coerceIn(0.4f, 1.0f)
        val snrScore = (metrics.snrDb / 25.0f).coerceIn(0.4f, 1.0f)
        val confidence = (qScore * snrScore).coerceIn(0.1f, 0.95f)

        return ThicknessResult(
            material = selectedMaterial,
            estimatedThicknessCm = thicknessCm,
            fundamentalFreqHz = peakFreqHz,
            confidence = confidence
        )
    }
}
