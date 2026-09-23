package com.apagon.rhythm.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.PaletteStyle

@Composable
fun RhythmTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isAmoled: Boolean = false,
    seedColor: Color = Color(0xFF6750A4),
    // Only applied in dark mode — lifts surface container tones so boxes and text
    // are more visible without changing the light-mode feel.
    darkContrastLevel: Double = 0.0,
    content: @Composable () -> Unit
) {
    DynamicMaterialTheme(
        seedColor = seedColor,
        useDarkTheme = darkTheme,
        withAmoled = isAmoled,
        contrastLevel = if (darkTheme) darkContrastLevel else 0.0,
        animate = false,
        style = PaletteStyle.TonalSpot,
        typography = Typography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small      = RoundedCornerShape(12.dp),
            medium     = RoundedCornerShape(16.dp),
            large      = RoundedCornerShape(32.dp),
            extraLarge = RoundedCornerShape(48.dp)
        ),
        content = content
    )
}
