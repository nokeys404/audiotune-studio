package com.audiotune.studio.core.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.audiotune.studio.core.ui.theme.CyanGlow
import com.audiotune.studio.core.ui.theme.ElectricViolet
import com.audiotune.studio.core.ui.theme.NeonCyan
import com.audiotune.studio.core.ui.theme.NeonPink

@Composable
fun StudioVisualizer(
    modifier: Modifier = Modifier,
    barCount: Int = 28
) {
    val infiniteTransition = rememberInfiniteTransition(label = "visualizer_wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        val totalWidth = size.width
        val totalHeight = size.height
        val barSpacing = 4.dp.toPx()
        val totalSpacing = barSpacing * (barCount - 1)
        val barWidth = (totalWidth - totalSpacing) / barCount

        val gradientBrush = Brush.linearGradient(
            colors = listOf(NeonCyan, CyanGlow, ElectricViolet, NeonPink),
            start = Offset(0f, 0f),
            end = Offset(totalWidth, 0f)
        )

        for (i in 0 until barCount) {
            val normalizedX = i.toFloat() / barCount
            // Harmonic wave calculation for natural equalizer animation
            val wave1 = kotlin.math.sin(normalizedX * 4f + phase) * 0.4f
            val wave2 = kotlin.math.cos(normalizedX * 8f - phase * 1.5f) * 0.3f
            val wave3 = kotlin.math.sin(normalizedX * 2f + phase * 0.5f) * 0.2f
            
            val amplitude = (0.25f + (wave1 + wave2 + wave3).coerceIn(-0.2f, 0.65f)).coerceIn(0.15f, 0.95f)
            val barHeight = totalHeight * amplitude
            val left = i * (barWidth + barSpacing)
            val top = totalHeight - barHeight

            drawRoundRect(
                brush = gradientBrush,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
        }
    }
}
