package com.apagon.rhythm.ui.util

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.random.Random

@Composable
fun ConfettiAnimation(
    modifier: Modifier = Modifier,
    onAnimationEnd: () -> Unit = {}
) {
    val particles = remember { List(30) { createParticle() } }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1500, easing = LinearOutSlowInEasing)
        )
        onAnimationEnd()
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val p = progress.value
        particles.forEach { particle ->
            val angle = particle.angle
            val distance = particle.maxDistance * p
            val x = center.x + Math.cos(angle.toDouble()).toFloat() * distance
            val y = center.y + Math.sin(angle.toDouble()).toFloat() * distance - (p * 100f) // gravity feel
            
            rotate(degrees = particle.rotation * p * 360f, pivot = Offset(x, y)) {
                val particleSize = 8.dp.toPx()
                drawRect(
                    color = particle.color.copy(alpha = 1f - p),
                    topLeft = Offset(x, y),
                    size = Size(particleSize, particleSize)
                )
            }
        }
    }
}

private data class Particle(
    val color: Color,
    val angle: Float,
    val maxDistance: Float,
    val rotation: Float
)

private fun createParticle(): Particle {
    val colors = listOf(Color(0xFF6200EE), Color(0xFF03DAC6), Color(0xFFFF0266), Color(0xFFFDD835))
    return Particle(
        color = colors.random(),
        angle = Random.nextFloat() * 2 * Math.PI.toFloat(),
        maxDistance = 100f + Random.nextFloat() * 200f,
        rotation = Random.nextFloat()
    )
}
