package com.voxmorph.ai.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voxmorph.ai.data.prefs.AppSettings
import com.voxmorph.ai.ui.theme.GlassCard
import com.voxmorph.ai.ui.theme.NeonBlue
import com.voxmorph.ai.ui.theme.TextPrimary
import com.voxmorph.ai.ui.theme.TextSecondary
import com.voxmorph.ai.ui.viewmodel.MainUiState

@Composable
fun FloatingSettingsScreen(
    state: MainUiState,
    onToggleFloating: () -> Unit,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit
) {
    val context = LocalContext.current
    val canOverlay = Settings.canDrawOverlays(context)
    val s = state.settings

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Floating Control Settings", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Atur widget mengambang agar nyaman saat membuka game atau aplikasi chat.", color = TextSecondary, fontSize = 12.sp)

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (canOverlay) "Izin Overlay: AKTIF" else "Izin Overlay: BELUM DIAKTIFKAN",
                    color = NeonBlue,
                    fontWeight = FontWeight.SemiBold
                )
                if (!canOverlay) {
                    Button(
                        onClick = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Berikan Izin SYSTEM_ALERT_WINDOW")
                    }
                } else {
                    Button(onClick = onToggleFloating, modifier = Modifier.fillMaxWidth()) {
                        Text(if (state.isFloatingVisible) "Sembunyikan Floating Widget" else "Tampilkan Floating Widget")
                    }
                }
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Transparansi: ${(s.floatingOpacity * 100).toInt()}%", color = TextPrimary, fontSize = 13.sp)
                Slider(
                    value = s.floatingOpacity,
                    onValueChange = { v -> onUpdateSettings { it.copy(floatingOpacity = v) } },
                    valueRange = 0.35f..1.0f
                )

                Text("Ukuran Widget: ${(s.floatingScale * 100).toInt()}%", color = TextPrimary, fontSize = 13.sp)
                Slider(
                    value = s.floatingScale,
                    onValueChange = { v -> onUpdateSettings { it.copy(floatingScale = v) } },
                    valueRange = 0.75f..1.25f
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Posisi Awal di Tepi Kanan", color = TextPrimary, fontSize = 13.sp)
                    Switch(
                        checked = s.floatingDockRight,
                        onCheckedChange = { checked -> onUpdateSettings { it.copy(floatingDockRight = checked) } }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto-Minimize saat Pindah App", color = TextPrimary, fontSize = 13.sp)
                    Switch(
                        checked = s.floatingAutoHide,
                        onCheckedChange = { checked -> onUpdateSettings { it.copy(floatingAutoHide = checked) } }
                    )
                }
            }
        }
    }
}
