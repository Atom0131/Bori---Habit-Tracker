package com.apagon.rhythm.ui.util

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun FutureGridAnimation(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gridFill")
    
    // Create a list of delays for each box to create a "filling up" effect
    val gridItems = remember { (0 until 35).toList().shuffled() }
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 7 columns (days of week)
        (0 until 7).forEach { col ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // 5 rows (weeks)
                (0 until 5).forEach { row ->
                    val index = col * 5 + row
                    val order = gridItems[index]
                    
                    val progress by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(
                                durationMillis = 2000,
                                delayMillis = order * 50,
                                easing = EaseOutExpo
                            ),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "boxFill"
                    )
                    
                    val boxColor by animateColorAsState(
                        targetValue = color.copy(alpha = progress.coerceIn(0.1f, 0.8f)),
                        label = "color"
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(boxColor)
                    )
                }
            }
        }
    }
}
