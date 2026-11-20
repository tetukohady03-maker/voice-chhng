package com.voxmorph.ai.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voxmorph.ai.data.model.ProcessingMode
import com.voxmorph.ai.data.prefs.AppSettings
import com.voxmorph.ai.ui.theme.CrimsonAlert
import com.voxmorph.ai.ui.theme.GlassCard
import com.voxmorph.ai.ui.theme.NeonBlue
import com.voxmorph.ai.ui.theme.TextPrimary
import com.voxmorph.ai.ui.theme.TextSecondary
import com.voxmorph.ai.ui.viewmodel.MainUiState

@Composable
fun SettingsScreen(
    state: MainUiState,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
    onDeleteAllCustomProfiles: () -> Unit
) {
    val context = LocalContext.current
    val s = state.settings

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Settings", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)

        // Processing Mode & Audio Quality
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Audio Quality & Processing Mode", color = NeonBlue, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProcessingMode.entries.forEach { mode ->
                        FilterChip(
                            selected = s.processingMode == mode,
                            onClick = { onUpdateSettings { it.copy(processingMode = mode) } },
                            label = { Text(mode.label, fontSize = 11.sp) }
                        )
                    }
                }
                Text(s.processingMode.description, color = TextSecondary, fontSize = 11.sp)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(24000, 44100, 48000).forEach { sr ->
                        FilterChip(
                            selected = s.sampleRateHz == sr,
                            onClick = { onUpdateSettings { it.copy(sampleRateHz = sr) } },
                            label = { Text("${sr / 1000} kHz", fontSize = 11.sp) }
                        )
                    }
                }

                Text("Monitoring Volume: ${(s.monitoringVolume * 100).toInt()}%", color = TextPrimary, fontSize = 12.sp)
                Slider(
                    value = s.monitoringVolume,
                    onValueChange = { v -> onUpdateSettings { it.copy(monitoringVolume = v) } },
                    valueRange = 0f..1.5f
                )
            }
        }

        // Battery Optimization & Permissions Guidance
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Battery & Background Stability", color = NeonBlue, fontWeight = FontWeight.SemiBold)
                Text(
                    "Agar Foreground Service dan Floating Control tidak dihentikan oleh manajemen baterai OEM saat bermain game, atur penggunaan baterai VOXMORPH AI ke 'Unrestricted / Tidak Dibatasi'.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Buka Pengaturan Optimasi Baterai")
                }
            }
        }

        // Privacy & Model Management
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Privasi & Model Management", color = NeonBlue, fontWeight = FontWeight.SemiBold)
                Text(
                    "• Pemrosesan DSP & ekstraksi profil suara 100% lokal di perangkat.\n" +
                    "• Tidak ada perekaman diam-diam; notifikasi mikrofon selalu tampil saat aktif.\n" +
                    "• Slot ONNX Runtime Mobile: filesDir/models/voice_conversion_quant.onnx (opsional).",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onDeleteAllCustomProfiles,
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonAlert),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Hapus Semua Profil Suara Kustom")
                }
            }
        }
    }
}
