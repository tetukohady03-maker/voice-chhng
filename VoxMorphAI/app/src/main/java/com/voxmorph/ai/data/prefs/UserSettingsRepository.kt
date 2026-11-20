package com.voxmorph.ai.data.prefs

import android.content.Context
import com.voxmorph.ai.data.model.ProcessingMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    val sampleRateHz: Int = 48000,
    val processingMode: ProcessingMode = ProcessingMode.BALANCED,
    val monitoringVolume: Float = 0.85f,
    val floatingOpacity: Float = 0.90f,
    val floatingScale: Float = 1.0f,
    val floatingDockRight: Boolean = true,
    val floatingAutoHide: Boolean = false,
    val localOnlyProcessing: Boolean = true
)

class UserSettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("voxmorph_settings", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun load(): AppSettings = AppSettings(
        sampleRateHz = prefs.getInt("sample_rate", 48000),
        processingMode = ProcessingMode.fromCode(prefs.getInt("processing_mode", ProcessingMode.BALANCED.code)),
        monitoringVolume = prefs.getFloat("monitoring_vol", 0.85f),
        floatingOpacity = prefs.getFloat("floating_opacity", 0.90f),
        floatingScale = prefs.getFloat("floating_scale", 1.0f),
        floatingDockRight = prefs.getBoolean("floating_dock_right", true),
        floatingAutoHide = prefs.getBoolean("floating_auto_hide", false),
        localOnlyProcessing = prefs.getBoolean("local_only", true)
    )

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
        _settings.value = next
        prefs.edit()
            .putInt("sample_rate", next.sampleRateHz)
            .putInt("processing_mode", next.processingMode.code)
            .putFloat("monitoring_vol", next.monitoringVolume)
            .putFloat("floating_opacity", next.floatingOpacity)
            .putFloat("floating_scale", next.floatingScale)
            .putBoolean("floating_dock_right", next.floatingDockRight)
            .putBoolean("floating_auto_hide", next.floatingAutoHide)
            .putBoolean("local_only", next.localOnlyProcessing)
            .apply()
    }
}
