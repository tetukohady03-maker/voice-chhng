package com.voxmorph.ai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voxmorph.ai.data.model.VocalFilterMode
import com.voxmorph.ai.data.model.VoicePreset
import com.voxmorph.ai.ui.theme.GlassCard
import com.voxmorph.ai.ui.theme.NeonBlue
import com.voxmorph.ai.ui.theme.TextPrimary
import com.voxmorph.ai.ui.theme.TextSecondary
import com.voxmorph.ai.ui.viewmodel.MainUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceStudioScreen(
    state: MainUiState,
    onUpdateParam: ((VoicePreset) -> VoicePreset) -> Unit,
    onToggleBypass: () -> Unit,
    onSaveCustomPreset: (String) -> Unit
) {
    val p = state.activePreset
    var showSaveDialog by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("${p.name} Mod") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Voice Studio", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Perubahan parameter langsung terdengar real-time", color = TextSecondary, fontSize = 12.sp)
            }
            OutlinedButton(onClick = onToggleBypass) {
                Text(if (state.isBypassed) "Bypass ON" else "FX ON", fontSize = 12.sp)
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Pitch & Formant", color = NeonBlue, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                ParamSlider("Pitch", p.pitchSemitones, -12f..12f, "%.1f st") { v ->
                    onUpdateParam { it.copy(pitchSemitones = v) }
                }
                ParamSlider("Formant Shift", p.formantShift, -1f..1f, "%.2f") { v ->
                    onUpdateParam { it.copy(formantShift = v) }
                }
                ParamSlider("Voice Depth", p.voiceDepth, -1f..1f, "%.2f") { v ->
                    onUpdateParam { it.copy(voiceDepth = v) }
                }
                ParamSlider("Resonance", p.resonance, 0f..1f, "%.2f") { v ->
                    onUpdateParam { it.copy(resonance = v) }
                }
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Equalizer & Timbre", color = NeonBlue, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                ParamSlider("Bass", p.bassDb, -12f..12f, "%.1f dB") { v ->
                    onUpdateParam { it.copy(bassDb = v) }
                }
                ParamSlider("Treble", p.trebleDb, -12f..12f, "%.1f dB") { v ->
                    onUpdateParam { it.copy(trebleDb = v) }
                }
                ParamSlider("Brightness", p.brightness, -1f..1f, "%.2f") { v ->
                    onUpdateParam { it.copy(brightness = v) }
                }
                ParamSlider("Noise Suppression", p.noiseSuppression, 0f..1f, "%.0f%%", 100f) { v ->
                    onUpdateParam { it.copy(noiseSuppression = v) }
                }
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Modulation & Effects", color = NeonBlue, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                ParamSlider("Modulation Intensity", p.modulationIntensity, 0f..1f, "%.0f%%", 100f) { v ->
                    onUpdateParam { it.copy(modulationIntensity = v) }
                }
                ParamSlider("Robot Modulation", p.robotModMix, 0f..1f, "%.0f%%", 100f) { v ->
                    onUpdateParam { it.copy(robotModMix = v) }
                }
                ParamSlider("Reverb / Echo", p.reverbMix, 0f..1f, "%.0f%%", 100f) { v ->
                    onUpdateParam { it.copy(reverbMix = v) }
                }
                ParamSlider("Distortion", p.distortionDrive, 0f..1f, "%.0f%%", 100f) { v ->
                    onUpdateParam { it.copy(distortionDrive = v) }
                }

                Spacer(Modifier.height(4.dp))
                Text("Vocal Filter", color = TextSecondary, fontSize = 12.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    VocalFilterMode.entries.forEach { mode ->
                        FilterChip(
                            selected = p.vocalFilterMode == mode.code,
                            onClick = { onUpdateParam { it.copy(vocalFilterMode = mode.code) } },
                            label = { Text(mode.label, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        Button(
            onClick = {
                customName = "${p.name} Custom"
                showSaveDialog = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Custom Preset")
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Simpan Preset Kustom") },
            text = {
                OutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    label = { Text("Nama Preset") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onSaveCustomPreset(customName)
                    showSaveDialog = false
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun ParamSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    format: String,
    displayScale: Float = 1f,
    onChange: (Float) -> Unit
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = TextPrimary, fontSize = 12.sp)
            Text(format.format(value * displayScale), color = NeonBlue, fontSize = 12.sp)
        }
        Slider(value = value.coerceIn(range.start, range.endInclusive), onValueChange = onChange, valueRange = range)
    }
}
