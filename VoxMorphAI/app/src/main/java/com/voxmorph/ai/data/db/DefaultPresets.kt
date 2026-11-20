package com.voxmorph.ai.data.db

import com.voxmorph.ai.data.model.VocalFilterMode
import com.voxmorph.ai.data.model.VoiceCategory
import com.voxmorph.ai.data.model.VoicePreset

object DefaultPresets {
    fun createAll(): List<VoicePreset> = listOf(
        // --- HUMAN VOICE (8) ---
        VoicePreset(
            name = "Feminine Voice",
            category = VoiceCategory.HUMAN.name,
            pitchSemitones = 4.5f,
            formantShift = 0.45f,
            bassDb = -2.5f,
            trebleDb = 3.0f,
            resonance = 0.22f,
            brightness = 0.35f,
            voiceDepth = -0.35f,
            noiseSuppression = 0.3f,
            isBuiltIn = true,
            isFavorite = true
        ),
        VoicePreset(
            name = "Masculine Voice",
            category = VoiceCategory.HUMAN.name,
            pitchSemitones = -3.5f,
            formantShift = -0.32f,
            bassDb = 3.5f,
            trebleDb = -1.0f,
            resonance = 0.2f,
            brightness = -0.1f,
            voiceDepth = 0.45f,
            noiseSuppression = 0.3f,
            isBuiltIn = true
        ),
        VoicePreset(
            name = "Deep Voice",
            category = VoiceCategory.HUMAN.name,
            pitchSemitones = -6.0f,
            formantShift = -0.55f,
            bassDb = 6.0f,
            trebleDb = -2.0f,
            resonance = 0.35f,
            brightness = -0.25f,
            voiceDepth = 0.8f,
            noiseSuppression = 0.3f,
            isBuiltIn = true,
            isFavorite = true
        ),
        VoicePreset(
            name = "Soft Voice",
            category = VoiceCategory.HUMAN.name,
            pitchSemitones = 1.5f,
            formantShift = 0.15f,
            bassDb = 1.0f,
            trebleDb = -2.5f,
            resonance = 0.1f,
            brightness = -0.2f,
            voiceDepth = -0.1f,
            reverbMix = 0.12f,
            noiseSuppression = 0.35f,
            isBuiltIn = true
        ),
        VoicePreset(
            name = "Cute Voice",
            category = VoiceCategory.HUMAN.name,
            pitchSemitones = 6.0f,
            formantShift = 0.62f,
            bassDb = -3.0f,
            trebleDb = 4.0f,
            resonance = 0.25f,
            brightness = 0.48f,
            voiceDepth = -0.5f,
            noiseSuppression = 0.3f,
            isBuiltIn = true
        ),
        VoicePreset(
            name = "Mature Voice",
            category = VoiceCategory.HUMAN.name,
            pitchSemitones = -2.0f,
            formantShift = -0.18f,
            bassDb = 2.5f,
            trebleDb = 0.5f,
            resonance = 0.25f,
            brightness = 0.1f,
            voiceDepth = 0.35f,
            noiseSuppression = 0.3f,
            isBuiltIn = true
        ),
        VoicePreset(
            name = "Bright Voice",
            category = VoiceCategory.HUMAN.name,
            pitchSemitones = 2.0f,
            formantShift = 0.25f,
            bassDb = -1.5f,
            trebleDb = 5.5f,
            resonance = 0.18f,
            brightness = 0.65f,
            voiceDepth = -0.2f,
            noiseSuppression = 0.3f,
            isBuiltIn = true
        ),
        VoicePreset(
            name = "Anime-inspired Voice",
            category = VoiceCategory.HUMAN.name,
            pitchSemitones = 5.5f,
            formantShift = 0.58f,
            bassDb = -3.5f,
            trebleDb = 4.5f,
            resonance = 0.32f,
            brightness = 0.55f,
            voiceDepth = -0.45f,
            noiseSuppression = 0.3f,
            isBuiltIn = true,
            isFavorite = true
        ),

        // --- SPECIAL EFFECTS (12) ---
        VoicePreset(
            name = "Robot",
            category = VoiceCategory.SPECIAL_FX.name,
            pitchSemitones = 0f,
            formantShift = 0.1f,
            resonance = 0.65f,
            modulationIntensity = 0.45f,
            robotModMix = 0.8f,
            noiseSuppression = 0.35f,
            isBuiltIn = true,
            isFavorite = true
        ),
        VoicePreset(
            name = "Cyborg",
            category = VoiceCategory.SPECIAL_FX.name,
            pitchSemitones = -2.5f,
            formantShift = -0.25f,
            bassDb = 3.0f,
            resonance = 0.55f,
            modulationIntensity = 0.35f,
            robotModMix = 0.55f,
            distortionDrive = 0.2f,
            reverbMix = 0.15f,
            isBuiltIn = true
        ),
        VoicePreset(
            name = "Alien",
            category = VoiceCategory.SPECIAL_FX.name,
            pitchSemitones = 3.5f,
            formantShift = 0.7f,
            resonance = 0.75f,
            modulationIntensity = 0.85f,
            robotModMix = 0.4f,
            reverbMix = 0.3f,
            isBuiltIn = true
        ),
        VoicePreset(
            name = "Monster",
            category = VoiceCategory.SPECIAL_FX.name,
            pitchSemitones = -8.0f,
            formantShift = -0.75f,
            bassDb = 7.5f,
            voiceDepth = 0.9f,
            distortionDrive = 0.45f,
            reverbMix = 0.35f,
            isBuiltIn = true
        ),
        VoicePreset(
            name = "Demon",
            category = VoiceCategory.SPECIAL_FX.name,
            pitchSemitones = -9.5f,
            formantShift = -0.85f,
            bassDb = 8.0f,
            resonance = 0.5f,
            voiceDepth = 1.0f,
            modulationIntensity = 0.3f,
            distortionDrive = 0.65f,
            reverbMix = 0.5f,
            isBuiltIn = true
        ),
        VoicePreset(
            name = "Ghost",
            category = VoiceCategory.SPECIAL_FX.name,
            pitchSemitones = 1.5f,
            formantShift = 0.3f,
            bassDb = -4.0f,
            trebleDb = 2.0f,
            modulationIntensity = 0.35f,
            reverbMix = 0.78f,
            isBuiltIn = true
        ),
        VoicePreset(
            name = "Radio",
            category = VoiceCategory.SPECIAL_FX.name,
            pitchSemitones = 0f,
            trebleDb = 3.0f,
            distortionDrive = 0.18f,
            vocalFilterMode = VocalFilterMode.RADIO.code,
            noiseSuppression = 0.2f,
            isBuiltIn = true
        ),
        VoicePreset(
            name = "Telephone",
            category = VoiceCategory.SPECIAL_FX.name,
            pitchSemitones = 0f,
            distortionDrive = 0.1f,
            vocalFilterMode = VocalFilterMode.TELEPHONE.code,
            noiseSuppression = 0.25f,
            isBuiltIn = true
        ),
        VoicePreset(
            name = "Walkie Talkie",
            category = VoiceCategory.SPECIAL_FX.name,
            pitchSemitones = 0.5f,
            distortionDrive = 0.32f,
            vocalFilterMode = VocalFilterMode.WALKIE_TALKIE.code,
            noiseSuppression = 0.3f,
            isBuiltIn = true
        ),
        VoicePreset(
            name = "Echo",
            category = VoiceCategory.SPECIAL_FX.name,
            pitchSemitones = 0f,
            reverbMix = 0.68f,
            brightness = 0.15f,
            isBuiltIn = true
        ),
        VoicePreset(
            name = "Megaphone",
            category = VoiceCategory.SPECIAL_FX.name,
            pitchSemitones = 1.0f,
            brightness = 0.5f,
            distortionDrive = 0.42f,
            vocalFilterMode = VocalFilterMode.MEGAPHONE.code,
            isBuiltIn = true
        ),
        VoicePreset(
            name = "Helium",
            category = VoiceCategory.SPECIAL_FX.name,
            pitchSemitones = 9.0f,
            formantShift = 0.85f,
            bassDb = -6.0f,
            trebleDb = 5.0f,
            brightness = 0.65f,
            voiceDepth = -0.8f,
            isBuiltIn = true
        )
    )
}
