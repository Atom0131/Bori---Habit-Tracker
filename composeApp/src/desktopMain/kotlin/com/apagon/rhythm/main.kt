package com.apagon.rhythm

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.apagon.rhythm.data.preferences.DarkReadability
import com.apagon.rhythm.data.preferences.ThemeMode
import com.apagon.rhythm.data.preferences.ThemePreferences
import com.apagon.rhythm.data.sync.DEFAULT_SYNC_PORT
import com.apagon.rhythm.data.sync.SyncServer
import com.apagon.rhythm.di.desktopAppModule
import com.apagon.rhythm.ui.habit.DesktopHabitScreen
import com.apagon.rhythm.ui.theme.RhythmTheme
import com.apagon.rhythm.ui.theme.resolveDisplayColor
import org.koin.compose.koinInject
import org.koin.core.context.startKoin

// Stage 4b checkpoint: adds the local loopback sync engine (Ktor WebSocket
// server + client) on top of Stage 3's Habit list/add/complete screen. See
// the plan at ~/.claude/plans/looks-we-closied-trying-peaceful-ritchie.md.
// Stage 5 checkpoint: wires the real RhythmTheme (materialkolor dynamic
// color, bundled fonts, dark/AMOLED/contrast) in place of a bare
// MaterialTheme, driven by the same ThemePreferences DataStore as Android.
fun main() {
    val koinApp = startKoin {
        modules(desktopAppModule)
    }

    // -Drhythm.syncPort=<port> lets two local instances (Stage 4b's test)
    // each bind to a distinct port on loopback; -Drhythm.home=<dir> (read in
    // DesktopHabitDatabase.kt) gives each its own database. Bound to
    // 127.0.0.1 explicitly, never 0.0.0.0 — Stage 4c rebinds this to the
    // device's real Tailscale interface IP instead, same principle.
    val syncPort = System.getProperty("rhythm.syncPort")?.toIntOrNull() ?: DEFAULT_SYNC_PORT
    koinApp.koin.get<SyncServer>().start(bindHost = "127.0.0.1", port = syncPort)

    // Stage 6 billing shim: there's no desktop payment rail and no plan to
    // build one, so desktop is unconditionally Pro rather than wiring up a
    // fake BillingRepository that mimics an API nothing here calls. Cheap to
    // re-assert on every launch since it's a single DataStore write.
    kotlinx.coroutines.runBlocking {
        koinApp.koin.get<ThemePreferences>().setIsPro(true)
    }

    application {
        Window(onCloseRequest = ::exitApplication, title = "Rhythm") {
            val themePreferences = koinInject<ThemePreferences>()
            val themeMode by themePreferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val amoledMode by themePreferences.amoledMode.collectAsState(initial = false)
            val accentColorIndex by themePreferences.accentColorIndex.collectAsState(initial = 0)
            val accentColorArgb by themePreferences.accentColorArgb.collectAsState(initial = null)
            val darkReadability by themePreferences.darkReadability.collectAsState(initial = DarkReadability.STANDARD)

            val isDark = when (themeMode) {
                ThemeMode.LIGHT  -> false
                ThemeMode.DARK   -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            RhythmTheme(
                darkTheme = isDark,
                isAmoled = amoledMode,
                seedColor = resolveDisplayColor(accentColorIndex.coerceAtLeast(0), accentColorArgb),
                darkContrastLevel = when (darkReadability) {
                    DarkReadability.STANDARD    -> 0.0
                    DarkReadability.COMFORTABLE -> 0.3
                    DarkReadability.HIGH        -> 0.65
                }
            ) {
                DesktopHabitScreen()
            }
        }
    }
}
