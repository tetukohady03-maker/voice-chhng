package com.voxmorph.ai.ui.viewmodel

import android.app.Application
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voxmorph.ai.VoxMorphApplication
import com.voxmorph.ai.ai.AudioDecoder
import com.voxmorph.ai.ai.DecodedAudio
import com.voxmorph.ai.ai.VoiceAnalysisReport
import com.voxmorph.ai.data.db.DefaultPresets
import com.voxmorph.ai.data.model.ProcessingMode
import com.voxmorph.ai.data.model.VoicePreset
import com.voxmorph.ai.data.prefs.AppSettings
import com.voxmorph.ai.service.VoiceChangerForegroundService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MainUiState(
    val presets: List<VoicePreset> = emptyList(),
    val activePreset: VoicePreset = DefaultPresets.createAll().first(),
    val isEngineRunning: Boolean = false,
    val isMuted: Boolean = false,
    val isBypassed: Boolean = false,
    val isFloatingVisible: Boolean = false,
    val inputRms: Float = 0f,
    val outputRms: Float = 0f,
    val latencyMs: Float = 0f,
    val underrunCount: Int = 0,
    val audioRoute: String = "Menunggu koneksi audio...",
    val waveform: FloatArray = FloatArray(48) { 0.12f },
    val settings: AppSettings = AppSettings(),
    val statusBanner: String? = null,

    // AI Voice Import state
    val importedAudio: DecodedAudio? = null,
    val importedFileName: String = "",
    val analysisProgress: Float = 0f,
    val analysisStatusText: String = "",
    val isAnalyzing: Boolean = false,
    val analysisReport: VoiceAnalysisReport? = null,
    val isPlayingSample: Boolean = false
)

class VoxMorphViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as VoxMorphApplication
    private val dao = app.database.voicePresetDao()

    private val _uiState = MutableStateFlow(MainUiState(audioRoute = app.audioBridge.currentOutputRouteName()))
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        app.audioBridge.onRouteChanged = { route ->
            _uiState.update { it.copy(audioRoute = route) }
        }
        app.audioBridge.onFocusLost = {
            _uiState.update {
                it.copy(
                    isEngineRunning = false,
                    statusBanner = "Engine dijeda otomatis karena panggilan masuk / audio focus berpindah."
                )
            }
            syncFloatingOverlay()
        }

        app.floatingToggleCallback = { toggleEngine() }
        app.floatingMuteCallback = { toggleMute() }
        app.floatingSelectPresetCallback = { selectPreset(it) }
        app.floatingVolumeCallback = { vol ->
            updateSettings { it.copy(monitoringVolume = vol) }
        }

        viewModelScope.launch {
            dao.observeAll().collect { list ->
                if (list.isNotEmpty()) {
                    val currentId = _uiState.value.activePreset.id
                    val matched = list.firstOrNull { it.id == currentId } ?: list.first()
                    _uiState.update { it.copy(presets = list, activePreset = matched) }
                    pushParamsToNative()
                    syncFloatingOverlay()
                }
            }
        }

        viewModelScope.launch {
            app.settingsRepository.settings.collect { s ->
                _uiState.update { it.copy(settings = s) }
                app.floatingManager.applyVisualSettings(s)
                pushParamsToNative()
            }
        }

        // Real-time telemetry loop (20 Hz UI refresh)
        viewModelScope.launch {
            while (isActive) {
                if (_uiState.value.isEngineRunning) {
                    val inRms = app.audioBridge.getInputRms()
                    val outRms = app.audioBridge.getOutputRms()
                    val lat = app.audioBridge.getLatencyMs()
                    val xruns = app.audioBridge.getUnderrunCount()
                    val wf = app.audioBridge.getWaveform(48)
                    _uiState.update {
                        it.copy(
                            inputRms = inRms,
                            outputRms = outRms,
                            latencyMs = lat,
                            underrunCount = xruns,
                            waveform = wf
                        )
                    }
                    syncFloatingOverlay()
                }
                delay(50)
            }
        }
    }

    fun toggleEngine() {
        val currentlyRunning = _uiState.value.isEngineRunning
        if (currentlyRunning) {
            app.audioBridge.stop()
            VoiceChangerForegroundService.stopService(app)
            _uiState.update {
                it.copy(
                    isEngineRunning = false,
                    inputRms = 0f,
                    outputRms = 0f,
                    statusBanner = "Voice Changer dihentikan."
                )
            }
        } else {
            VoiceChangerForegroundService.startService(app)
            pushParamsToNative()
            val started = app.audioBridge.start(_uiState.value.settings.sampleRateHz)
            _uiState.update {
                it.copy(
                    isEngineRunning = started,
                    statusBanner = if (started) "Voice Changer aktif (Oboe Low-Latency)"
                    else "Gagal membuka stream audio. Pastikan izin mikrofon diberikan."
                )
            }
        }
        syncFloatingOverlay()
    }

    fun toggleMute() {
        _uiState.update { it.copy(isMuted = !it.isMuted) }
        pushParamsToNative()
        syncFloatingOverlay()
    }

    fun toggleBypass() {
        _uiState.update { it.copy(isBypassed = !it.isBypassed) }
        pushParamsToNative()
    }

    fun selectPreset(preset: VoicePreset) {
        _uiState.update { it.copy(activePreset = preset) }
        pushParamsToNative()
        syncFloatingOverlay()
    }

    fun updateActivePresetParameters(transform: (VoicePreset) -> VoicePreset) {
        val updated = transform(_uiState.value.activePreset)
        _uiState.update { it.copy(activePreset = updated) }
        pushParamsToNative()
    }

    fun saveCustomPreset(newName: String) {
        val base = _uiState.value.activePreset
        viewModelScope.launch(Dispatchers.IO) {
            val saved = base.copy(
                id = 0,
                name = newName.trim().ifEmpty { "${base.name} Custom" },
                isBuiltIn = false,
                createdAt = System.currentTimeMillis()
            )
            val newId = dao.insert(saved)
            dao.getById(newId)?.let { inserted ->
                _uiState.update {
                    it.copy(activePreset = inserted, statusBanner = "Preset '${inserted.name}' tersimpan.")
                }
            }
        }
    }

    fun renamePreset(preset: VoicePreset, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            dao.update(preset.copy(name = newName.trim()))
        }
    }

    fun duplicatePreset(preset: VoicePreset) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insert(
                preset.copy(
                    id = 0,
                    name = "${preset.name} (Copy)",
                    isBuiltIn = false,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun toggleFavorite(preset: VoicePreset) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.update(preset.copy(isFavorite = !preset.isFavorite))
        }
    }

    fun deletePreset(preset: VoicePreset) {
        if (preset.isBuiltIn) return
        viewModelScope.launch(Dispatchers.IO) {
            dao.delete(preset)
        }
    }

    fun resetBuiltInPresetsToDefault() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteAllBuiltIn()
            dao.insertAll(DefaultPresets.createAll())
            _uiState.update { it.copy(statusBanner = "Preset bawaan dikembalikan ke pengaturan awal.") }
        }
    }

    fun deleteAllCustomProfiles() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteAllCustomProfiles()
            _uiState.update { it.copy(statusBanner = "Semua profil suara kustom telah dihapus.") }
        }
    }

    fun toggleFloatingWidget() {
        if (!app.floatingManager.canDrawOverlays()) {
            _uiState.update { it.copy(statusBanner = "Aktifkan izin 'Tampilkan di atas aplikasi lain' terlebih dahulu.") }
            return
        }
        if (app.floatingManager.isShowing()) {
            app.floatingManager.hide()
            _uiState.update { it.copy(isFloatingVisible = false) }
        } else {
            app.floatingManager.show(_uiState.value.settings)
            _uiState.update { it.copy(isFloatingVisible = true) }
            syncFloatingOverlay()
        }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        app.settingsRepository.update(transform)
    }

    // --- AI Voice Import Flow ---
    fun importAudioSample(uri: Uri, fileName: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isAnalyzing = true,
                    importedFileName = fileName,
                    analysisProgress = 0.05f,
                    analysisStatusText = "Mendekode audio lokal...",
                    analysisReport = null
                )
            }
            try {
                val decoded = withContext(Dispatchers.IO) {
                    AudioDecoder.decodeUri(app, uri)
                }
                val report = withContext(Dispatchers.Default) {
                    app.voiceAnalyzer.analyze(decoded) { progress, label ->
                        _uiState.update {
                            it.copy(analysisProgress = progress, analysisStatusText = label)
                        }
                    }
                }
                _uiState.update {
                    it.copy(
                        importedAudio = decoded,
                        isAnalyzing = false,
                        analysisReport = report
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        analysisStatusText = "Gagal membaca audio: ${e.localizedMessage ?: "Format tidak didukung"}"
                    )
                }
            }
        }
    }

    fun playImportedSample() {
        val decoded = _uiState.value.importedAudio ?: return
        if (_uiState.value.isPlayingSample) return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isPlayingSample = true) }
            val minBuf = AudioTrack.getMinBufferSize(
                decoded.sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_FLOAT
            )
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setSampleRate(decoded.sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minBuf, 4096))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            try {
                track.play()
                val maxPlay = minOf(decoded.samples.size, decoded.sampleRate * 8)
                track.write(decoded.samples, 0, maxPlay, AudioTrack.WRITE_BLOCKING)
            } finally {
                track.stop()
                track.release()
                _uiState.update { it.copy(isPlayingSample = false) }
            }
        }
    }

    fun saveImportedVoiceProfile(profileName: String, consentConfirmed: Boolean) {
        val report = _uiState.value.analysisReport ?: return
        if (!consentConfirmed) {
            _uiState.update { it.copy(statusBanner = "Konfirmasi kepemilikan/izin suara wajib dicentang.") }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val preset = app.voiceAnalyzer.toVoicePreset(profileName, report, consentConfirmed)
            val newId = dao.insert(preset)
            dao.getById(newId)?.let { saved ->
                _uiState.update {
                    it.copy(
                        activePreset = saved,
                        statusBanner = "Profil AI '${saved.name}' berhasil disimpan ke Voice Library."
                    )
                }
                pushParamsToNative()
            }
        }
    }

    fun clearBanner() {
        _uiState.update { it.copy(statusBanner = null) }
    }

    private fun pushParamsToNative() {
        val s = _uiState.value
        app.audioBridge.applyPreset(
            preset = s.activePreset,
            monitoringVolume = s.settings.monitoringVolume,
            processingMode = s.settings.processingMode,
            muted = s.isMuted,
            bypassed = s.isBypassed
        )
    }

    private fun syncFloatingOverlay() {
        if (!app.floatingManager.isShowing()) return
        val s = _uiState.value
        app.floatingManager.updateState(
            isEngineRunning = s.isEngineRunning,
            isMuted = s.isMuted,
            activeName = s.activePreset.name,
            estLatencyMs = s.latencyMs,
            quickPresets = s.presets.filter { it.isFavorite }.ifEmpty { s.presets.take(5) }
        )
    }

    override fun onCleared() {
        super.onCleared()
        app.audioBridge.stop()
        app.floatingManager.hide()
    }
}
