package com.apagon.rhythm

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.apagon.rhythm.di.desktopAppModule
import org.koin.core.context.startKoin

// Stage 2 checkpoint: the Habit database/repository/DI graph is wired up.
// The real Habit list/add/complete screen (Stage 3) replaces the
// placeholder Text below; see the plan at
// ~/.claude/plans/pasted-content-id-be9d-the-next-noble-tiger.md.
fun main() {
    startKoin {
        modules(desktopAppModule)
    }

    application {
        Window(onCloseRequest = ::exitApplication, title = "Rhythm") {
            MaterialTheme {
                Text("Rhythm")
            }
        }
    }
}
