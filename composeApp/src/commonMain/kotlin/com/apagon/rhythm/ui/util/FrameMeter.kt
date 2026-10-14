package com.apagon.rhythm.ui.util

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.platform.RenderInfo
import com.apagon.rhythm.ui.components.crystalTileSurface

/**
 * A small frame-rate readout in the window's bottom-right corner while Settings → About → Show
 * frame rate is on: frames drawn in the last second and the slowest one. Nothing runs while it's
 * off. While on, it asks for a frame every vsync, so it measures what the window can do rather than
 * only when something changes; the "worst" number is the one that shows a stutter on a tab switch.
 */
@Composable
fun FrameMeterOverlay() {
    val on by RenderInfo.showFrameMeter.collectAsState()
    if (!on) return
    var text by remember { mutableStateOf("…") }
    LaunchedEffect(Unit) {
        var windowStart = 0L
        var last = 0L
        var frames = 0
        var worst = 0L
        while (true) {
            withFrameNanos { now ->
                if (windowStart == 0L) windowStart = now
                if (last != 0L) worst = maxOf(worst, now - last)
                last = now
                frames++
                if (now - windowStart >= 1_000_000_000L) {
                    text = "$frames fps · worst ${worst / 1_000_000} ms"
                    windowStart = now
                    frames = 0
                    worst = 0L
                }
            }
        }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 0.dp, end = 96.dp, bottom = 16.dp)
                .crystalTileSurface()
                .padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
