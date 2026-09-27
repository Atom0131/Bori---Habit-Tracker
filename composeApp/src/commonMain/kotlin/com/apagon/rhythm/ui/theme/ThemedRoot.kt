package com.apagon.rhythm.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.apagon.rhythm.data.preferences.CrystalBackground
import com.apagon.rhythm.data.preferences.CrystalMesh
import com.apagon.rhythm.data.preferences.CrystalStyle
import com.apagon.rhythm.data.preferences.DarkReadability
import com.apagon.rhythm.data.preferences.ThemeMode
import com.apagon.rhythm.data.preferences.ThemePreferences
import com.apagon.rhythm.data.preferences.ThemeStyle
import com.apagon.rhythm.ui.components.CrystalRoot
import com.apagon.rhythm.ui.components.crystalScaffoldColor
import com.apagon.rhythm.ui.components.crystalScaffoldContentColor

/**
 * The whole theme, for a window that is not the app's main one — an alert/lock-screen surface, or
 * any other secondary window. Ported from the Android original: each such window has its own
 * composition and so needs its own [CrystalRoot], since the ambient field a main window installs
 * lives in a different window and cannot show through here.
 *
 * @param seedColorOverride bypasses the accent preference (used by callers that resolve their own
 *   accent from elsewhere and must match what they actually render, e.g. Android's widget config
 *   screens reading `WidgetColors` — not applicable on desktop today, kept for parity).
 */
@Composable
fun RhythmThemedRoot(
    themePreferences: ThemePreferences,
    seedColorOverride: Color? = null,
    content: @Composable () -> Unit
) {
    val themeMode by themePreferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val amoledMode by themePreferences.amoledMode.collectAsState(initial = false)
    val accentColorIndex by themePreferences.accentColorIndex.collectAsState(initial = 0)
    val accentColorArgb by themePreferences.accentColorArgb.collectAsState(initial = null)
    val darkReadability by themePreferences.darkReadability.collectAsState(initial = DarkReadability.STANDARD)
    val themeStyle by themePreferences.themeStyle.collectAsState(initial = ThemeStyle.MATERIAL3)
    val crystalStyle by themePreferences.crystalStyle.collectAsState(initial = CrystalStyle.TINTED)
    val crystalIntensity by themePreferences.crystalIntensity.collectAsState(initial = ThemePreferences.DEFAULT_CRYSTAL_INTENSITY)
    val crystalBackground by themePreferences.crystalBackground.collectAsState(initial = CrystalBackground.MESH)
    val crystalMesh by themePreferences.crystalMesh.collectAsState(initial = CrystalMesh.AURORA)
    val accentIsDefault by themePreferences.accentColorIsDefault.collectAsState(initial = true)
    val crystalBgIndex by themePreferences.crystalBackgroundColorIndex.collectAsState(initial = ThemePreferences.CRYSTAL_BG_NEUTRAL)
    val crystalBgArgb by themePreferences.crystalBackgroundColorArgb.collectAsState(initial = null)
    val crystalMeshCustomArgb by themePreferences.crystalMeshCustomArgb.collectAsState(initial = null)
    val crystalMeshCustomChroma by themePreferences.crystalMeshCustomChroma
        .collectAsState(initial = ThemePreferences.DEFAULT_CRYSTAL_MESH_CUSTOM_CHROMA)

    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemDark
    }

    RhythmTheme(
        darkTheme = isDark,
        isAmoled = amoledMode,
        seedColor = seedColorOverride
            ?: accentSeed(themeStyle, crystalBackground, crystalMesh, accentColorIndex, accentColorArgb, accentIsDefault),
        themeStyle = themeStyle,
        crystalStyle = crystalStyle,
        crystalIntensity = crystalIntensity,
        crystalBackground = crystalBackground,
        crystalMesh = crystalMesh,
        crystalMeshCustomSeed = crystalMeshCustomArgb?.let { Color(it) },
        crystalMeshCustomChroma = crystalMeshCustomChroma,
        crystalRoom = crystalRoomColor(crystalBgIndex, crystalBgArgb, isDark, amoledMode),
        darkContrastLevel = when (darkReadability) {
            DarkReadability.STANDARD -> 0.0
            DarkReadability.COMFORTABLE -> 0.3
            DarkReadability.HIGH -> 0.65
        }
    ) {
        CrystalRoot {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = crystalScaffoldColor(),
                contentColor = crystalScaffoldContentColor(),
                content = content
            )
        }
    }
}
