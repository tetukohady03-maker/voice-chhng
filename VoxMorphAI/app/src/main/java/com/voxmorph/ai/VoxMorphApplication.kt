package com.voxmorph.ai

import android.app.Application
import com.voxmorph.ai.ai.VoiceProfileAnalyzer
import com.voxmorph.ai.audio.NativeAudioBridge
import com.voxmorph.ai.data.db.DefaultPresets
import com.voxmorph.ai.data.db.VoxMorphDatabase
import com.voxmorph.ai.data.prefs.UserSettingsRepository
import com.voxmorph.ai.overlay.FloatingVoiceControlManager
import com.voxmorph.ai.service.VoiceChangerForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class VoxMorphApplication : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    lateinit var database: VoxMorphDatabase
    lateinit var settingsRepository: UserSettingsRepository
    lateinit var audioBridge: NativeAudioBridge
    lateinit var voiceAnalyzer: VoiceProfileAnalyzer
    lateinit var floatingManager: FloatingVoiceControlManager

    var floatingToggleCallback: (() -> Unit)? = null
    var floatingMuteCallback: (() -> Unit)? = null
    var floatingSelectPresetCallback: ((com.voxmorph.ai.data.model.VoicePreset) -> Unit)? = null
    var floatingVolumeCallback: ((Float) -> Unit)? = null

    override fun onCreate() {
        super.onCreate()
        VoiceChangerForegroundService.createChannel(this)
        database = VoxMorphDatabase.getInstance(this)
        settingsRepository = UserSettingsRepository(this)
        audioBridge = NativeAudioBridge(this)
        voiceAnalyzer = VoiceProfileAnalyzer(this)

        floatingManager = FloatingVoiceControlManager(
            context = this,
            onToggleEngine = { floatingToggleCallback?.invoke() },
            onToggleMute = { floatingMuteCallback?.invoke() },
            onSelectPreset = { floatingSelectPresetCallback?.invoke(it) },
            onVolumeChange = { floatingVolumeCallback?.invoke(it) }
        )

        appScope.launch {
            val dao = database.voicePresetDao()
            if (dao.count() == 0) {
                dao.insertAll(DefaultPresets.createAll())
            }
        }
    }
}
