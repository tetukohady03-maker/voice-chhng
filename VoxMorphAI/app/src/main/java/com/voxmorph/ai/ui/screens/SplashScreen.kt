package com.voxmorph.ai.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voxmorph.ai.ui.components.WaveformVisualizer
import com.voxmorph.ai.ui.theme.ElectricPurple
import com.voxmorph.ai.ui.theme.MidnightBlack
import com.voxmorph.ai.ui.theme.NeonBlue
import com.voxmorph.ai.ui.theme.TextPrimary
import com.voxmorph.ai.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "splash_wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "phase"
    )

    LaunchedEffect(Unit) {
        delay(1200)
        onFinished()
    }

    val bars = FloatArray(32) { i ->
        0.2f + 0.75f * abs(sin(phase + i * 0.35f))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBlack)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "VOXMORPH AI",
            color = TextPrimary,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 3.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Real-Time Native DSP & On-Device Voice Conversion",
            color = NeonBlue,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(28.dp))
        WaveformVisualizer(amplitudes = bars, height = 64.dp, isActive = true)
        Spacer(Modifier.height(28.dp))
        LinearProgressIndicator(color = ElectricPurple, trackColor = NeonBlue.copy(alpha = 0.2f))
        Spacer(Modifier.height(10.dp))
        Text("Memuat Oboe Audio Engine & Voice Library...", color = TextSecondary, fontSize = 12.sp)
    }
}
