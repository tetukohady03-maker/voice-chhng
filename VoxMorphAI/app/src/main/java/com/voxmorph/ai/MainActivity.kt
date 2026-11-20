package com.voxmorph.ai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.voxmorph.ai.ui.screens.AiVoiceImportScreen
import com.voxmorph.ai.ui.screens.DashboardScreen
import com.voxmorph.ai.ui.screens.FloatingSettingsScreen
import com.voxmorph.ai.ui.screens.SettingsScreen
import com.voxmorph.ai.ui.screens.SplashScreen
import com.voxmorph.ai.ui.screens.VoiceLibraryScreen
import com.voxmorph.ai.ui.screens.VoiceStudioScreen
import com.voxmorph.ai.ui.theme.DeepSurface
import com.voxmorph.ai.ui.theme.MidnightBlack
import com.voxmorph.ai.ui.theme.VoxMorphTheme
import com.voxmorph.ai.ui.viewmodel.VoxMorphViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VoxMorphTheme {
                VoxMorphRoot()
            }
        }
    }
}

private enum class NavRoute(val label: String) {
    DASHBOARD("Home"),
    STUDIO("Studio"),
    IMPORT("AI Import"),
    LIBRARY("Library"),
    SETTINGS("Settings"),
    FLOATING_SETTINGS("Floating")
}

@Composable
private fun VoxMorphRoot(vm: VoxMorphViewModel = viewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showSplash by remember { mutableStateOf(true) }
    var currentRoute by remember { mutableStateOf(NavRoute.DASHBOARD) }
    val snackbarHostState = remember { SnackbarHostState() }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results[Manifest.permission.RECORD_AUDIO] == true) {
            vm.toggleEngine()
        }
    }

    LaunchedEffect(state.statusBanner) {
        state.statusBanner?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            vm.clearBanner()
        }
    }

    if (showSplash) {
        SplashScreen(onFinished = { showSplash = false })
        return
    }

    Scaffold(
        containerColor = MidnightBlack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(containerColor = DeepSurface) {
                val tabs = listOf(
                    NavRoute.DASHBOARD to Icons.Default.Dashboard,
                    NavRoute.STUDIO to Icons.Default.Tune,
                    NavRoute.IMPORT to Icons.Default.AudioFile,
                    NavRoute.LIBRARY to Icons.Default.LibraryMusic,
                    NavRoute.SETTINGS to Icons.Default.Settings
                )
                tabs.forEach { (route, icon) ->
                    NavigationBarItem(
                        selected = currentRoute == route,
                        onClick = { currentRoute = route },
                        icon = { Icon(icon, contentDescription = route.label) },
                        label = { Text(route.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentRoute) {
                NavRoute.DASHBOARD -> DashboardScreen(
                    state = state,
                    onToggleEngine = {
                        val hasMic = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasMic) {
                            vm.toggleEngine()
                        } else {
                            val perms = buildList {
                                add(Manifest.permission.RECORD_AUDIO)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    add(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }.toTypedArray()
                            permLauncher.launch(perms)
                        }
                    },
                    onToggleMute = vm::toggleMute,
                    onToggleFloating = vm::toggleFloatingWidget,
                    onSelectPreset = vm::selectPreset,
                    onOpenFloatingSettings = { currentRoute = NavRoute.FLOATING_SETTINGS }
                )

                NavRoute.STUDIO -> VoiceStudioScreen(
                    state = state,
                    onUpdateParam = vm::updateActivePresetParameters,
                    onToggleBypass = vm::toggleBypass,
                    onSaveCustomPreset = vm::saveCustomPreset
                )

                NavRoute.IMPORT -> AiVoiceImportScreen(
                    state = state,
                    onImportAudio = vm::importAudioSample,
                    onPlaySample = vm::playImportedSample,
                    onSaveProfile = vm::saveImportedVoiceProfile
                )

                NavRoute.LIBRARY -> VoiceLibraryScreen(
                    state = state,
                    onSelectPreset = vm::selectPreset,
                    onToggleFavorite = vm::toggleFavorite,
                    onDuplicate = vm::duplicatePreset,
                    onRename = vm::renamePreset,
                    onDelete = vm::deletePreset,
                    onResetDefault = vm::resetBuiltInPresetsToDefault
                )

                NavRoute.FLOATING_SETTINGS -> FloatingSettingsScreen(
                    state = state,
                    onToggleFloating = vm::toggleFloatingWidget,
                    onUpdateSettings = vm::updateSettings
                )

                NavRoute.SETTINGS -> SettingsScreen(
                    state = state,
                    onUpdateSettings = vm::updateSettings,
                    onDeleteAllCustomProfiles = vm::deleteAllCustomProfiles
                )
            }
        }
    }
}
