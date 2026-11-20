package com.voxmorph.ai.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class VoiceCategory(val label: String) {
    HUMAN("Human Voice"),
    SPECIAL_FX("Special Effects"),
    CUSTOM_AI("AI Custom Voice")
}

enum class VocalFilterMode(val code: Int, val label: String) {
    NONE(0, "Off"),
    TELEPHONE(1, "Telephone"),
    RADIO(2, "Radio"),
    WALKIE_TALKIE(3, "Walkie Talkie"),
    MEGAPHONE(4, "Megaphone");

    companion object {
        fun fromCode(code: Int): VocalFilterMode =
            entries.firstOrNull { it.code == code } ?: NONE
    }
}

enum class ProcessingMode(val code: Int, val label: String, val description: String) {
    PERFORMANCE(0, "Performance", "Latensi terendah, CPU hemat (reverb dimatikan)"),
    BALANCED(1, "Balanced", "Keseimbangan latensi rendah dan kualitas penuh"),
    QUALITY(2, "Quality", "Presisi formant & spectral matching maksimal");

    companion object {
        fun fromCode(code: Int): ProcessingMode =
            entries.firstOrNull { it.code == code } ?: BALANCED
    }
}

@Entity(tableName = "voice_presets")
data class VoicePreset(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String = VoiceCategory.HUMAN.name,
    val pitchSemitones: Float = 0f,
    val formantShift: Float = 0f,
    val bassDb: Float = 0f,
    val trebleDb: Float = 0f,
    val resonance: Float = 0f,
    val brightness: Float = 0f,
    val voiceDepth: Float = 0f,
    val modulationIntensity: Float = 0f,
    val noiseSuppression: Float = 0.25f,
    val reverbMix: Float = 0f,
    val distortionDrive: Float = 0f,
    val robotModMix: Float = 0f,
    val vocalFilterMode: Int = VocalFilterMode.NONE.code,
    val spectralProfileEnabled: Boolean = false,
    val spectralEnvelopeCsv: String = "",
    val isBuiltIn: Boolean = false,
    val isFavorite: Boolean = false,
    val qualityScore: Int = 100,
    val sampleDurationSec: Float = 0f,
    val estimatedPitchHz: Float = 0f,
    val consentConfirmed: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun spectralArray(): FloatArray {
        if (spectralEnvelopeCsv.isBlank()) return FloatArray(16) { 0f }
        val parsed = spectralEnvelopeCsv.split(",").mapNotNull { it.trim().toFloatOrNull() }
        return FloatArray(16) { idx -> parsed.getOrElse(idx) { 0f } }
    }
}
