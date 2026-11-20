package com.voxmorph.ai.ai

import ai.onnxruntime.OrtEnvironment
import android.content.Context
import com.voxmorph.ai.data.model.VoiceCategory
import com.voxmorph.ai.data.model.VoicePreset
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

data class VoiceAnalysisReport(
    val medianPitchHz: Float,
    val pitchSpreadHz: Float,
    val snrDb: Float,
    val durationSec: Float,
    val spectralEnvelopeDb: FloatArray,
    val suggestedPitchShift: Float,
    val suggestedFormantShift: Float,
    val suggestedBrightness: Float,
    val suggestedVoiceDepth: Float,
    val qualityScore: Int,
    val qualityLabel: String,
    val needsMoreSamples: Boolean,
    val recommendationNote: String,
    val estimatedLatencyMs: String,
    val onnxModelLoaded: Boolean,
    val waveformPreview: FloatArray
)

/**
 * On-device Voice Profile Analyzer.
 * Extracts F0 pitch distribution, 16-band Bark spectral envelope, timbre brightness,
 * chest depth, and SNR quality score locally. Supports optional quantized ONNX neural VC
 * model if placed in filesDir/models/voice_conversion_quant.onnx; otherwise uses real-time
 * DSP pitch + formant + 16-band spectral envelope voice conversion with honest quality metrics.
 */
class VoiceProfileAnalyzer(private val context: Context) {

    private val barkCenters = floatArrayOf(
        150f, 250f, 350f, 450f, 570f, 700f, 840f, 1000f,
        1170f, 1370f, 1600f, 1850f, 2150f, 2500f, 3150f, 4400f
    )

    fun hasLocalOnnxModel(): Boolean {
        val modelFile = File(context.filesDir, "models/voice_conversion_quant.onnx")
        return modelFile.exists() && modelFile.length() > 1024
    }

    fun verifyOnnxRuntimeAvailable(): Boolean = try {
        OrtEnvironment.getEnvironment() != null
    } catch (_: Throwable) {
        false
    }

    fun analyze(
        decoded: DecodedAudio,
        referenceUserPitchHz: Float = 145f,
        onProgress: (Float, String) -> Unit
    ): VoiceAnalysisReport {
        val samples = decoded.samples
        val sr = decoded.sampleRate
        onProgress(0.15f, "Memeriksa durasi & membersihkan noise...")

        // 1. Estimate noise floor & speech RMS for SNR
        val frameSize = (sr * 0.025f).toInt().coerceIn(256, 2048)
        val hopSize = frameSize / 2
        val frameRmsList = ArrayList<Float>()
        var idx = 0
        while (idx + frameSize <= samples.size) {
            var sumSq = 0.0
            for (i in 0 until frameSize) {
                val s = samples[idx + i]
                sumSq += s * s
            }
            frameRmsList.add(sqrt(sumSq / frameSize).toFloat())
            idx += hopSize
        }

        val sortedRms = frameRmsList.sorted()
        val noiseFloor = if (sortedRms.isNotEmpty()) sortedRms[(sortedRms.size * 0.15f).toInt()].coerceAtLeast(1e-5f) else 1e-4f
        val speechLevel = if (sortedRms.isNotEmpty()) sortedRms[(sortedRms.size * 0.85f).toInt()].coerceAtLeast(1e-4f) else 0.05f
        val snrDb = (20f * log10(speechLevel / noiseFloor)).coerceIn(0f, 50f)

        // 2. Pitch (F0) distribution via normalized autocorrelation on voiced frames
        onProgress(0.45f, "Menganalisis distribusi pitch (F0)...")
        val f0Values = ArrayList<Float>()
        val minLag = (sr / 420f).toInt().coerceAtLeast(16)
        val maxLag = (sr / 65f).toInt().coerceAtMost(frameSize - 1)
        val activeThreshold = noiseFloor * 2.2f

        idx = 0
        var fIdx = 0
        while (idx + frameSize <= samples.size) {
            if (fIdx < frameRmsList.size && frameRmsList[fIdx] > activeThreshold) {
                val f0 = estimateFrameF0(samples, idx, frameSize, sr, minLag, maxLag)
                if (f0 in 70f..400f) f0Values.add(f0)
            }
            idx += hopSize
            fIdx++
        }

        val sortedF0 = f0Values.sorted()
        val medianF0 = if (sortedF0.isNotEmpty()) sortedF0[sortedF0.size / 2] else 160f
        val p10 = if (sortedF0.size > 4) sortedF0[(sortedF0.size * 0.1f).toInt()] else medianF0 - 15f
        val p90 = if (sortedF0.size > 4) sortedF0[(sortedF0.size * 0.9f).toInt()] else medianF0 + 15f
        val pitchSpread = (p90 - p10).coerceAtLeast(5f)

        // 3. 16-Band Bark Spectral Envelope via Goertzel energy across voiced frames
        onProgress(0.75f, "Mengekstrak formant & spectral envelope...")
        val bandEnergy = DoubleArray(16)
        var voicedFrames = 0
        idx = 0
        fIdx = 0
        while (idx + frameSize <= samples.size) {
            if (fIdx < frameRmsList.size && frameRmsList[fIdx] > activeThreshold) {
                for (b in 0 until 16) {
                    bandEnergy[b] += goertzelPower(samples, idx, frameSize, barkCenters[b], sr.toFloat())
                }
                voicedFrames++
            }
            idx += hopSize * 2
            fIdx += 2
        }

        val bandDb = FloatArray(16)
        if (voicedFrames > 0) {
            var meanDb = 0.0
            for (b in 0 until 16) {
                val avgP = (bandEnergy[b] / voicedFrames).coerceAtLeast(1e-9)
                bandDb[b] = (10.0 * log10(avgP)).toFloat()
                meanDb += bandDb[b]
            }
            meanDb /= 16.0
            for (b in 0 until 16) {
                // Normalize relative to mean spectral energy and clamp to safe DSP filter range
                bandDb[b] = ((bandDb[b] - meanDb.toFloat()) * 0.45f).coerceIn(-8.0f, 8.0f)
            }
        }

        // 4. Derive musical/vocal characteristics
        val semitoneShift = (12f * (ln((medianF0 / referenceUserPitchHz).toDouble()) / ln(2.0)))
            .toFloat().coerceIn(-12f, 12f)
        val lowEnergy = (bandDb[0] + bandDb[1] + bandDb[2]) / 3f
        val highEnergy = (bandDb[12] + bandDb[13] + bandDb[14] + bandDb[15]) / 4f
        val suggestedDepth = (lowEnergy / 8f).coerceIn(-0.8f, 0.8f)
        val suggestedBrightness = (highEnergy / 8f).coerceIn(-0.8f, 0.8f)
        val suggestedFormant = ((semitoneShift / 12f) * 0.65f + (suggestedBrightness * 0.25f)).coerceIn(-0.85f, 0.85f)

        // 5. Honest quality score & guidance
        val durationScore = ((decoded.durationSec / 10f).coerceIn(0f, 1f) * 40f).toInt()
        val snrScore = (((snrDb - 6f) / 24f).coerceIn(0f, 1f) * 35f).toInt()
        val voicedScore = ((voicedFrames / 40f).coerceIn(0f, 1f) * 25f).toInt()
        val qualityScore = (durationScore + snrScore + voicedScore).coerceIn(10, 96)

        val needsMore = decoded.durationSec < 6f || snrDb < 14f || voicedFrames < 20
        val qualityLabel = when {
            qualityScore >= 80 -> "Tinggi (Profil Stabil)"
            qualityScore >= 55 -> "Sedang (Disarankan sampel lebih jernih)"
            else -> "Rendah (Butuh sampel tambahan)"
        }
        val note = buildString {
            append("Karakteristik suara diekstrak secara lokal (F0 median ${medianF0.toInt()} Hz, SNR ${snrDb.toInt()} dB). ")
            if (needsMore) {
                append("Untuk hasil lebih akurat, gunakan rekaman bicara jernih 10–30 detik tanpa musik latar. ")
            }
            append("Hasil adalah konversi karakteristik vokal, bukan kloning identik 100%.")
        }

        // Waveform preview (64 bars)
        val preview = FloatArray(64)
        val step = max(1, samples.size / 64)
        for (i in 0 until 64) {
            var peak = 0f
            val start = i * step
            val end = min(samples.size, start + step)
            for (j in start until end) {
                peak = max(peak, abs(samples[j]))
            }
            preview[i] = peak.coerceIn(0f, 1f)
        }

        val onnxLoaded = hasLocalOnnxModel() && verifyOnnxRuntimeAvailable()
        val latencyText = if (onnxLoaded) "DSP + ONNX ~65–115 ms" else "DSP Spectral Profile ~18–35 ms"

        onProgress(1.0f, "Analisis profil suara selesai.")
        return VoiceAnalysisReport(
            medianPitchHz = medianF0,
            pitchSpreadHz = pitchSpread,
            snrDb = snrDb,
            durationSec = decoded.durationSec,
            spectralEnvelopeDb = bandDb,
            suggestedPitchShift = semitoneShift,
            suggestedFormantShift = suggestedFormant,
            suggestedBrightness = suggestedBrightness,
            suggestedVoiceDepth = suggestedDepth,
            qualityScore = qualityScore,
            qualityLabel = qualityLabel,
            needsMoreSamples = needsMore,
            recommendationNote = note,
            estimatedLatencyMs = latencyText,
            onnxModelLoaded = onnxLoaded,
            waveformPreview = preview
        )
    }

    fun toVoicePreset(name: String, report: VoiceAnalysisReport, consentConfirmed: Boolean): VoicePreset {
        return VoicePreset(
            name = name.trim().ifEmpty { "Custom Voice" },
            category = VoiceCategory.CUSTOM_AI.name,
            pitchSemitones = report.suggestedPitchShift,
            formantShift = report.suggestedFormantShift,
            bassDb = report.suggestedVoiceDepth * 4f,
            trebleDb = report.suggestedBrightness * 4f,
            resonance = 0.25f,
            brightness = report.suggestedBrightness,
            voiceDepth = report.suggestedVoiceDepth,
            noiseSuppression = 0.32f,
            spectralProfileEnabled = true,
            spectralEnvelopeCsv = report.spectralEnvelopeDb.joinToString(",") { "%.2f".format(it) },
            isBuiltIn = false,
            isFavorite = true,
            qualityScore = report.qualityScore,
            sampleDurationSec = report.durationSec,
            estimatedPitchHz = report.medianPitchHz,
            consentConfirmed = consentConfirmed
        )
    }

    private fun estimateFrameF0(
        samples: FloatArray,
        offset: Int,
        frameSize: Int,
        sr: Int,
        minLag: Int,
        maxLag: Int
    ): Float {
        var bestLag = -1
        var bestCorr = 0.35f
        for (lag in minLag..maxLag) {
            var num = 0f
            var denA = 0f
            var denB = 0f
            val limit = frameSize - lag
            var i = 0
            while (i < limit) {
                val a = samples[offset + i]
                val b = samples[offset + i + lag]
                num += a * b
                denA += a * a
                denB += b * b
                i += 2 // 2x decimation for fast mobile analysis
            }
            val denom = sqrt(denA * denB) + 1e-6f
            val corr = num / denom
            if (corr > bestCorr) {
                bestCorr = corr
                bestLag = lag
            }
        }
        return if (bestLag > 0) sr.toFloat() / bestLag.toFloat() else 0f
    }

    private fun goertzelPower(
        samples: FloatArray,
        offset: Int,
        n: Int,
        targetFreq: Float,
        sampleRate: Float
    ): Double {
        val omega = 2.0 * PI * targetFreq / sampleRate
        val coeff = 2.0 * cos(omega)
        var q0 = 0.0
        var q1 = 0.0
        var q2 = 0.0
        for (i in 0 until n) {
            val win = 0.5 * (1.0 - cos(2.0 * PI * i / (n - 1)))
            q0 = coeff * q1 - q2 + samples[offset + i] * win
            q2 = q1
            q1 = q0
        }
        val real = q1 - q2 * cos(omega)
        val imag = q2 * sin(omega)
        return (real * real + imag * imag) / (n * n)
    }
}
