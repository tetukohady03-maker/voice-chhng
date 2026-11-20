package com.voxmorph.ai.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val MidnightBlack = Color(0xFF07080F)
val DeepSurface = Color(0xFF101324)
val GlassSurface = Color(0xCC151930)
val ElectricPurple = Color(0xFF8B5CF6)
val ElectricPurpleDark = Color(0xFF6D28D9)
val NeonBlue = Color(0xFF00D4FF)
val EmeraldLive = Color(0xFF10B981)
val AmberWarn = Color(0xFFF59E0B)
val CrimsonAlert = Color(0xFFEF4444)
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)

val PrimaryGradient = Brush.horizontalGradient(listOf(ElectricPurple, NeonBlue))

private val VoxDarkScheme = darkColorScheme(
    primary = ElectricPurple,
    secondary = NeonBlue,
    background = MidnightBlack,
    surface = DeepSurface,
    onPrimary = Color.White,
    onSecondary = MidnightBlack,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = CrimsonAlert
)

@Composable
fun VoxMorphTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VoxDarkScheme,
        content = content
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = ElectricPurple.copy(alpha = 0.28f),
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = GlassSurface),
        border = BorderStroke(1.dp, borderColor)
    ) {
        content()
    }
}
