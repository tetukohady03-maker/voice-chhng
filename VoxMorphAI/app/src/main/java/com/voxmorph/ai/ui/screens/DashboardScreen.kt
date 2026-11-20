package com.voxmorph.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voxmorph.ai.data.model.VoicePreset
import com.voxmorph.ai.ui.components.WaveformVisualizer
import com.voxmorph.ai.ui.theme.AmberWarn
import com.voxmorph.ai.ui.theme.CrimsonAlert
import com.voxmorph.ai.ui.theme.ElectricPurple
import com.voxmorph.ai.ui.theme.EmeraldLive
import com.voxmorph.ai.ui.theme.GlassCard
import com.voxmorph.ai.ui.theme.NeonBlue
import com.voxmorph.ai.ui.theme.PrimaryGradient
import com.voxmorph.ai.ui.theme.TextPrimary
import com.voxmorph.ai.ui.theme.TextSecondary
import com.voxmorph.ai.ui.viewmodel.MainUiState

@Composable
fun DashboardScreen(
    state: MainUiState,
    onToggleEngine: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleFloating: () -> Unit,
    onSelectPreset: (VoicePreset) -> Unit,
    onOpenFloatingSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header + Live Mic Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("VOXMORPH AI", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(state.audioRoute, color = TextSecondary, fontSize = 12.sp)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (state.isEngineRunning) EmeraldLive.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = if (state.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Mic status",
                    tint = when {
                        state.isMuted -> CrimsonAlert
                        state.isEngineRunning -> EmeraldLive
                        else -> TextSecondary
                    },
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = when {
                        state.isMuted -> "MUTED"
                        state.isEngineRunning -> "MIC LIVE"
                        else -> "STANDBY"
                    },
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Main Power & Live Waveform Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Profil Aktif", color = TextSecondary, fontSize = 12.sp)
                Text(
                    state.activePreset.name,
                    color = NeonBlue,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                WaveformVisualizer(
                    amplitudes = state.waveform,
                    isActive = state.isEngineRunning && !state.isMuted,
                    height = 68.dp
                )
                Spacer(Modifier.height(14.dp))

                // Big Start / Stop Power Button
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(if (state.isEngineRunning) PrimaryGradient else PrimaryGradient)
                        .clickable { onToggleEngine() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Start/Stop Voice Changer",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (state.isEngineRunning) "Ketuk untuk Hentikan Voice Changer" else "Mulai Real-Time Voice Changer",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MetricChip("Latensi", "%.1f ms".format(state.latencyMs))
                    MetricChip("Mode", state.settings.processingMode.label)
                    MetricChip("XRuns", "${state.underrunCount}")
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onToggleMute,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            if (state.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(if (state.isMuted) "Unmute" else "Mute")
                    }
                    Button(
                        onClick = onToggleFloating,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple)
                    ) {
                        Icon(Icons.Default.PictureInPictureAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(if (state.isFloatingVisible) "Tutup Widget" else "Floating Control")
                    }
                }
            }
        }

        // Quick Preset Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Quick Presets", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Atur Floating",
                color = NeonBlue,
                fontSize = 12.sp,
                modifier = Modifier.clickable { onOpenFloatingSettings() }
            )
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(state.presets.take(10), key = { it.id }) { preset ->
                FilterChip(
                    selected = preset.id == state.activePreset.id,
                    onClick = { onSelectPreset(preset) },
                    label = { Text(preset.name, fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
            }
        }

        // Honest Android Cross-App Compatibility Notice
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = AmberWarn.copy(alpha = 0.45f)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Catatan Routing Lintas Aplikasi (Non-Root)", color = AmberWarn, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Floating Control dan pemrosesan DSP lokal berjalan penuh tanpa root. Namun, Android standar membatasi aplikasi pihak ketiga mengganti mikrofon langsung ke Mobile Legends, WhatsApp, atau Discord. Gunakan USB audio loopback / headset splitter untuk routing non-root, atau lihat matriks kompatibilitas di Pengaturan.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

@Composable
private fun MetricChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = NeonBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TextSecondary, fontSize = 11.sp)
    }
}
