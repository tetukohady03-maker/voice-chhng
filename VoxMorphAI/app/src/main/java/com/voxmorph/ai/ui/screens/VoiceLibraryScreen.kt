package com.voxmorph.ai.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.voxmorph.ai.data.model.VoiceCategory
import com.voxmorph.ai.data.model.VoicePreset
import com.voxmorph.ai.ui.theme.CrimsonAlert
import com.voxmorph.ai.ui.theme.ElectricPurple
import com.voxmorph.ai.ui.theme.GlassCard
import com.voxmorph.ai.ui.theme.NeonBlue
import com.voxmorph.ai.ui.theme.TextPrimary
import com.voxmorph.ai.ui.theme.TextSecondary
import com.voxmorph.ai.ui.viewmodel.MainUiState

@Composable
fun VoiceLibraryScreen(
    state: MainUiState,
    onSelectPreset: (VoicePreset) -> Unit,
    onToggleFavorite: (VoicePreset) -> Unit,
    onDuplicate: (VoicePreset) -> Unit,
    onRename: (VoicePreset, String) -> Unit,
    onDelete: (VoicePreset) -> Unit,
    onResetDefault: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var renameTarget by remember { mutableStateOf<VoicePreset?>(null) }
    var renameInput by remember { mutableStateOf("") }

    val filtered = state.presets.filter { preset ->
        val matchesSearch = preset.name.contains(search, ignoreCase = true)
        val matchesCat = when (selectedFilter) {
            "FAV" -> preset.isFavorite
            "ALL" -> true
            else -> preset.category == selectedFilter
        }
        matchesSearch && matchesCat
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Voice Library", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = onResetDefault) {
                Text("Reset Default", fontSize = 11.sp)
            }
        }

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            label = { Text("Cari preset atau profil suara...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val filters = listOf(
                "ALL" to "Semua",
                "FAV" to "Favorit",
                VoiceCategory.HUMAN.name to "Human Voice",
                VoiceCategory.SPECIAL_FX.name to "Special FX",
                VoiceCategory.CUSTOM_AI.name to "Custom AI"
            )
            items(filters) { (key, label) ->
                FilterChip(
                    selected = selectedFilter == key,
                    onClick = { selectedFilter = key },
                    label = { Text(label, fontSize = 12.sp) }
                )
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered, key = { it.id }) { preset ->
                val isSelected = preset.id == state.activePreset.id
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectPreset(preset) },
                    borderColor = if (isSelected) NeonBlue else ElectricPurple.copy(alpha = 0.25f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                preset.name,
                                color = if (isSelected) NeonBlue else TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                "Pitch: %.1f st | Formant: %.2f | %s".format(
                                    preset.pitchSemitones,
                                    preset.formantShift,
                                    if (preset.isBuiltIn) "Built-in" else "Custom"
                                ),
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Row {
                            IconButton(onClick = { onToggleFavorite(preset) }) {
                                Icon(
                                    if (preset.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (preset.isFavorite) CrimsonAlert else TextSecondary
                                )
                            }
                            IconButton(onClick = { onDuplicate(preset) }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = TextSecondary)
                            }
                            IconButton(onClick = {
                                renameTarget = preset
                                renameInput = preset.name
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Rename", tint = TextSecondary)
                            }
                            if (!preset.isBuiltIn) {
                                IconButton(onClick = { onDelete(preset) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CrimsonAlert)
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Ubah Nama Preset") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onRename(target, renameInput)
                    renameTarget = null
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Batal") }
            }
        )
    }
}
