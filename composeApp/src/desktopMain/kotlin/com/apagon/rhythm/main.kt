package com.apagon.rhythm

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.apagon.rhythm.data.preferences.ThemePreferences
import com.apagon.rhythm.data.repository.TimerRepository
import com.apagon.rhythm.data.sync.DEFAULT_SYNC_PORT
import com.apagon.rhythm.data.sync.SyncServer
import com.apagon.rhythm.data.sync.findTailscaleAddress
import com.apagon.rhythm.di.desktopAppModule
import com.apagon.rhythm.platform.AlertCenter
import com.apagon.rhythm.platform.DesktopAlarmClockService
import com.apagon.rhythm.platform.FiredAlert
import com.apagon.rhythm.platform.FiredAlertKind
import com.apagon.rhythm.ui.alarms.DesktopAlertContent
import com.apagon.rhythm.ui.habit.DesktopTodayScreen
import com.apagon.rhythm.ui.journal.DesktopJournalScreen
import com.apagon.rhythm.ui.notes.DesktopNotesTab
import com.apagon.rhythm.ui.reminders.DesktopClockScreen
import com.apagon.rhythm.ui.components.crystalChromeSurface
import com.apagon.rhythm.ui.components.crystalTileSurface
import com.apagon.rhythm.ui.settings.DesktopSettingsScreen
import com.apagon.rhythm.ui.theme.RhythmThemedRoot
import kotlinx.coroutines.launch
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
    // DesktopHabitDatabase.kt) gives each its own database. Stage 13: binds
    // to the real Tailscale interface IP when one is found, falling back to
    // 127.0.0.1 (never 0.0.0.0) so Stage 4b's local-loopback two-instance
    // dev workflow keeps working when Tailscale isn't installed/running.
    val syncPort = System.getProperty("rhythm.syncPort")?.toIntOrNull() ?: DEFAULT_SYNC_PORT
    val bindHost = findTailscaleAddress() ?: "127.0.0.1"
    koinApp.koin.get<SyncServer>().start(bindHost = bindHost, port = syncPort)

    // Stage 6 billing shim: there's no desktop payment rail and no plan to
    // build one, so desktop is unconditionally Pro rather than wiring up a
    // fake BillingRepository that mimics an API nothing here calls. Cheap to
    // re-assert on every launch since it's a single DataStore write.
    kotlinx.coroutines.runBlocking {
        koinApp.koin.get<ThemePreferences>().setIsPro(true)
    }

    // Stage 12: the in-process scheduler must run session-wide, independent of
    // whether the Clock tab is ever opened — same reasoning SyncServer.start()
    // above already established for this app's "start once at launch" services.
    koinApp.koin.get<DesktopAlarmClockService>().start()

    application {
        // Stage 15a: the window previously had no default/min size at all (bare OS default),
        // which is what let the old ScrollableTabRow shell get squeezed into the label-wrapping
        // widths the last layout round had to patch around. The sidebar shell replacing it needs
        // enough width for a ~220dp rail plus a genuinely usable content pane, so give the window
        // real sizing intent instead of leaving it to chance.
        Window(
            onCloseRequest = ::exitApplication,
            title = "Rhythm",
            state = rememberWindowState(width = 1280.dp, height = 800.dp)
        ) {
            window.minimumSize = java.awt.Dimension(960, 600)
            val themePreferences = koinInject<ThemePreferences>()

            RhythmThemedRoot(themePreferences = themePreferences) {
                DesktopAppRoot()
            }
        }

        DesktopAlertWindowHost()
    }
}

// Stage 12's stand-in for Android's full-screen alert Activities: a single
// always-on-top Window, opened only while an alert is active, driven by
// AlertCenter's SharedFlow. Lives at the application{} level (a sibling of
// the main Window) rather than inside DesktopAppRoot so it fires regardless
// of which tab is showing.
@Composable
private fun ApplicationScope.DesktopAlertWindowHost() {
    val alertCenter = koinInject<AlertCenter>()
    val timerRepository = koinInject<TimerRepository>()
    val scope = rememberCoroutineScope()
    var activeAlert by remember { mutableStateOf<FiredAlert?>(null) }

    LaunchedEffect(Unit) {
        alertCenter.alerts.collect { alert -> activeAlert = alert }
    }

    activeAlert?.let { alert ->
        Window(
            onCloseRequest = { activeAlert = null },
            title = "Rhythm Alert",
            alwaysOnTop = true,
            state = rememberWindowState(position = WindowPosition.Aligned(Alignment.Center), width = 380.dp, height = 220.dp)
        ) {
            val themePreferences = koinInject<ThemePreferences>()
            RhythmThemedRoot(themePreferences = themePreferences) {
                DesktopAlertContent(
                    alert = alert,
                    onDismiss = { activeAlert = null },
                    onStartNextPhase = if (alert.kind == FiredAlertKind.TIMER && alert.isPomo) {
                        {
                            scope.launch { timerRepository.advancePomoPhase(alert.sourceId) }
                            activeAlert = null
                        }
                    } else null
                )
            }
        }
    }
}

// Stage 15a: replaces Stage 7's ScrollableTabRow with a persistent left sidebar — the pattern
// every comparison app (Notion/Asana/Todoist/ClickUp) uses for desktop top-level nav, and one
// that doesn't have the tab row's label-wrapping-at-narrow-widths failure mode in the first
// place, since the rail's width is fixed rather than shared out across N tabs. "Recently
// Deleted" is deliberately dropped from this list — every comparison app treats trash as
// secondary, not primary, nav — and moves into Settings as a sub-section in Stage 15f.
// Stage 17e: "Habits"/"To-dos"/"Calendar" merged into one "Today" section — the real Android app
// has no separate tabs for these either (MainActivity.kt's bottom nav is Today/Journal/Clock/Notes
// only), so this now mirrors mobile's actual grouping instead of Stage 15a's general
// desktop-app-research guess.
private val SIDEBAR_SECTIONS = listOf("Today", "Journal", "Notes", "Clock", "Settings")
private val SIDEBAR_WIDTH = 220.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DesktopAppRoot() {
    var selectedSection by remember { mutableIntStateOf(0) }

    Row(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .width(SIDEBAR_WIDTH)
                .fillMaxHeight()
                .crystalChromeSurface()
                .padding(vertical = 12.dp, horizontal = 8.dp)
        ) {
            Text(
                text = "Rhythm",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 16.dp)
            )
            SIDEBAR_SECTIONS.forEachIndexed { index, label ->
                SidebarItem(
                    label = label,
                    selected = selectedSection == index,
                    onClick = { selectedSection = index }
                )
            }
        }
        Column(modifier = Modifier.fillMaxSize()) {
            when (selectedSection) {
                0 -> DesktopTodayScreen()
                1 -> DesktopJournalScreen()
                2 -> DesktopNotesTab()
                3 -> DesktopClockScreen()
                4 -> DesktopSettingsScreen()
            }
        }
    }
}

@Composable
private fun SidebarItem(label: String, selected: Boolean, onClick: () -> Unit) {
    // Stage 17a: no pre-clip here — crystalTileSurface() draws its own background/rim/shadow
    // using its own internal shape (MaterialTheme.shapes.medium), which doesn't match an outer
    // RoundedCornerShape(10.dp) clip. The mismatch let the tile's own shadow escape the outer
    // clip on one edge (a visible dark sliver on the left of the selected pill). Matches the
    // already-correct pattern in DesktopNotesScreen.kt's NotebookRail, which never had this bug.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (selected) Modifier.crystalTileSurface(fill = MaterialTheme.colorScheme.secondaryContainer)
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
