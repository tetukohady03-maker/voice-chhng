package com.voxmorph.ai.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voxmorph.ai.ui.components.WaveformVisualizer
import com.voxmorph.ai.ui.theme.AmberWarn
import com.voxmorph.ai.ui.theme.EmeraldLive
import com.voxmorph.ai.ui.theme.GlassCard
import com.voxmorph.ai.ui.theme.NeonBlue
import com.voxmorph.ai.ui.theme.TextPrimary
import com.voxmorph.ai.ui.theme.TextSecondary
import com.voxmorph.ai.ui.viewmodel.MainUiState

@Composable
fun AiVoiceImportScreen(
    state: MainUiState,
    onImportAudio: (Uri, String) -> Unit,
    onPlaySample: () -> Unit,
    onSaveProfile: (String, Boolean) -> Unit
) {
    var profileName by remember { mutableStateOf("My AI Voice Profile") }
    var consentChecked by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            onImportAudio(uri, uri.lastPathSegment ?: "audio_sample")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("AI Voice Import", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            "Impor contoh suara (WAV, MP3, M4A, AAC) secara lokal untuk mengekstrak pitch, formant, timbre, dan 16-band spectral envelope.",
            color = TextSecondary,
            fontSize = 12.sp
        )

        // Consent & Ethics Guardrail
        GlassCard(modifier = Modifier.fillMaxWidth(), borderColor = AmberWarn.copy(alpha = 0.45f)) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = consentChecked, onCheckedChange = { consentChecked = it })
                Text(
                    "Saya menyatakan sampel suara ini adalah suara saya sendiri atau telah mendapat persetujuan pemilik suara, dan tidak digunakan untuk penipuan atau penyamaran identitas tanpa izin.",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }

        Button(
            onClick = {
                picker.launch(arrayOf("audio/wav", "audio/x-wav", "audio/mpeg", "audio/mp4", "audio/aac", "audio/*"))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Pilih File Audio (WAV / MP3 / M4A / AAC)")
        }

        if (state.isAnalyzing) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(state.analysisStatusText, color = NeonBlue, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { state.analysisProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        state.analysisReport?.let { report ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Hasil Analisis Profil: ${state.importedFileName}", color = TextPrimary, fontWeight = FontWeight.Bold)
                    WaveformVisualizer(amplitudes = report.waveformPreview, height = 56.dp, isActive = true)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Kualitas Profil: ${report.qualityScore}/100", color = if (report.qualityScore >= 70) EmeraldLive else AmberWarn, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(report.qualityLabel, color = TextSecondary, fontSize = 12.sp)
                    }
                    Text("Durasi: %.1f dtk | F0 Median: %.0f Hz (±%.0f Hz) | SNR: %.1f dB".format(
                        report.durationSec, report.medianPitchHz, report.pitchSpreadHz, report.snrDb
                    ), color = TextPrimary, fontSize = 12.sp)
                    Text("Estimasi Performa: ${report.estimatedLatencyMs}", color = NeonBlue, fontSize = 12.sp)
                    Text(report.recommendationNote, color = TextSecondary, fontSize = 11.sp, lineHeight = 16.sp)

                    OutlinedButton(
                        onClick = onPlaySample,
                        enabled = !state.isPlayingSample,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (state.isPlayingSample) "Memutar Sampel..." else "Putar Sampel Audio")
                    }

                    OutlinedTextField(
                        value = profileName,
                        onValueChange = { profileName = it },
                        label = { Text("Nama Profil Suara") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = { onSaveProfile(profileName, consentChecked) },
                        enabled = consentChecked,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Simpan ke Voice Library & Aktifkan")
                    }
                }
            }
        }
    }
}
