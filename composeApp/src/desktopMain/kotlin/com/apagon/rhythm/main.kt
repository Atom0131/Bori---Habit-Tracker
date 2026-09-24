package com.apagon.rhythm

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.apagon.rhythm.data.sync.DEFAULT_SYNC_PORT
import com.apagon.rhythm.data.sync.SyncServer
import com.apagon.rhythm.di.desktopAppModule
import com.apagon.rhythm.ui.habit.DesktopHabitScreen
import org.koin.core.context.startKoin

// Stage 4b checkpoint: adds the local loopback sync engine (Ktor WebSocket
// server + client) on top of Stage 3's Habit list/add/complete screen. See
// the plan at ~/.claude/plans/looks-we-closied-trying-peaceful-ritchie.md.
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

    application {
        Window(onCloseRequest = ::exitApplication, title = "Rhythm") {
            MaterialTheme {
                DesktopHabitScreen()
            }
        }
    }
}
