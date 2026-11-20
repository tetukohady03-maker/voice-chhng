package com.voxmorph.ai.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.voxmorph.ai.ui.theme.ElectricPurple
import com.voxmorph.ai.ui.theme.NeonBlue

@Composable
fun WaveformVisualizer(
    amplitudes: FloatArray,
    modifier: Modifier = Modifier,
    height: Dp = 72.dp,
    isActive: Boolean = true
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val count = amplitudes.size.coerceAtLeast(1)
        val spacing = 4f
        val barWidth = ((size.width - spacing * (count - 1)) / count).coerceAtLeast(3f)
        val midY = size.height / 2f
        val brush = Brush.verticalGradient(
            colors = if (isActive) listOf(NeonBlue, ElectricPurple)
            else listOf(ElectricPurple.copy(alpha = 0.35f), NeonBlue.copy(alpha = 0.2f))
        )

        for (i in 0 until count) {
            val amp = amplitudes[i].coerceIn(0.06f, 1f)
            val barHeight = (amp * size.height * 0.9f).coerceAtLeast(6f)
            val x = i * (barWidth + spacing)
            drawRoundRect(
                brush = brush,
                topLeft = Offset(x, midY - barHeight / 2f),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
