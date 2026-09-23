package com.apagon.rhythm

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.apagon.rhythm.di.desktopAppModule
import com.apagon.rhythm.ui.habit.DesktopHabitScreen
import org.koin.core.context.startKoin

// Stage 3 checkpoint: the real (if minimal) Habit list/add/complete screen.
// See the plan at ~/.claude/plans/pasted-content-id-be9d-the-next-noble-tiger.md.
fun main() {
    startKoin {
        modules(desktopAppModule)
    }

    application {
        Window(onCloseRequest = ::exitApplication, title = "Rhythm") {
            MaterialTheme {
                DesktopHabitScreen()
            }
        }
    }
}
