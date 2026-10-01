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
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.isTraySupported
import androidx.compose.ui.window.rememberTrayState
import androidx.compose.ui.window.rememberWindowState
import com.apagon.rhythm.data.preferences.ThemePreferences
import com.apagon.rhythm.data.repository.TimerRepository
import com.apagon.rhythm.data.sync.DEFAULT_SYNC_PORT
import com.apagon.rhythm.data.sync.SyncServer
import com.apagon.rhythm.data.sync.findTailscaleAddress
import com.apagon.rhythm.di.desktopAppModule
import com.apagon.rhythm.platform.AlertCenter
import com.apagon.rhythm.platform.DesktopAlarmClockService
import com.apagon.rhythm.platform.DesktopSingleInstance
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.core.context.startKoin
import rhythm.composeapp.generated.resources.Res
import rhythm.composeapp.generated.resources.app_icon

// Stage 4b checkpoint: adds the local loopback sync engine (Ktor WebSocket
// server + client) on top of Stage 3's Habit list/add/complete screen. See
// the plan at ~/.claude/plans/looks-we-closied-trying-peaceful-ritchie.md.
// Stage 5 checkpoint: wires the real RhythmTheme (materialkolor dynamic
// color, bundled fonts, dark/AMOLED/contrast) in place of a bare
// MaterialTheme, driven by the same ThemePreferences DataStore as Android.
/**
 * Stage 18b: Compose Multiplatform's `FontFamily(fontA, fontB, ...)` fallback list only picks the
 * single closest weight/style match for layout — it does NOT fall back to a sibling entry for a
 * glyph the chosen typeface doesn't cover, unlike Android's font-fallback chain. So bundling
 * NotoColorEmoji as another `FontFamily` entry (see `ui/theme/Type.kt`) alone still rendered every
 * emoji (streak flame, notebook, lock/unlock, pin, journal feelings, note templates) as a "MISSING
 * GLYPH" tofu box on any machine with no system color-emoji font — confirmed live on this one via
 * `fc-list | grep -i emoji` returning nothing. What actually resolves it is Skia's own *system*
 * font-fallback (via fontconfig on Linux), which only sees fonts fontconfig knows about. This
 * installs the same bundled resource into the user's fontconfig font dir on first run (idempotent
 * — skipped once present) so every target machine self-heals the same way this one was fixed
 * manually, without requiring the user to install a font themselves.
 */
@OptIn(ExperimentalResourceApi::class)
private fun ensureEmojiFontInstalled() {
    if (System.getProperty("os.name")?.lowercase()?.contains("linux") != true) return
    val fontDir = java.io.File(System.getProperty("user.home"), ".local/share/fonts")
    val fontFile = java.io.File(fontDir, "rhythm-noto-color-emoji.ttf")
    if (fontFile.exists()) return
    runCatching {
        fontDir.mkdirs()
        val bytes = kotlinx.coroutines.runBlocking { Res.readBytes("font/noto_color_emoji.ttf") }
        fontFile.writeBytes(bytes)
        ProcessBuilder("fc-cache", "-f", fontDir.absolutePath).start().waitFor()
    }
}

fun main() {
    // Must run before any DB/Koin setup — a second launch while a "run in background" instance
    // is already alive should just signal it and exit, not open a second connection to the same
    // Room database. See DesktopSingleInstance's own doc comment for why there's no tray icon to
    // click instead.
    val singleInstance = DesktopSingleInstance()
    if (!singleInstance.acquire()) return

    ensureEmojiFontInstalled()

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

    // Holds the live AWT window once Window{}'s content composes, so the single-instance watcher
    // (a plain coroutine, outside composition) and the Tray's click handler can toggle it later.
    //
    // Two earlier approaches were tried and rejected live before landing on Tray + isVisible:
    //  - Decomposing Window{} (`if (windowVisible) Window(...)`) tears down its Skiko/Skia surface,
    //    and doing that from inside the native windowing callback onCloseRequest crashed the JVM
    //    outright (SIGSEGV in libX11's XVisualIDFromVisual, confirmed live).
    //  - Setting `extendedState = ICONIFIED` from onCloseRequest (the first shipped fix) *looked*
    //    safe in that session's manual testing, but is actually a race: Skiko runs a background
    //    frame-limiter coroutine that queries the AWT drawing surface's X11 visual on every frame,
    //    and iconifying while that's in flight hits the exact same XVisualIDFromVisual SIGSEGV —
    //    confirmed live on a real XFCE/X11 session, non-deterministically (crashed outright once,
    //    silently destroyed the window without crashing a second time). `isVisible = false` avoids
    //    touching AWT's iconify state machine at all, which is what that frame-limiter race needs.
    // This is also what cross-platform JVM apps actually ship for Linux minimize-to-tray (a status
    // icon that hides/shows the window), rather than hand-rolling an iconify-based hide — Compose
    // Desktop exposes this directly as the `Tray` composable used below, no new dependency.
    val mainWindowRef = mutableStateOf<androidx.compose.ui.awt.ComposeWindow?>(null)
    val showMainWindow: () -> Unit = {
        java.awt.EventQueue.invokeLater {
            mainWindowRef.value?.apply {
                isVisible = true
                extendedState = java.awt.Frame.NORMAL
                toFront()
                requestFocus()
            }
        }
    }
    singleInstance.startWatching(CoroutineScope(SupervisorJob() + Dispatchers.Default)) {
        showMainWindow()
    }

    application {
        val themePreferences = koinInject<ThemePreferences>()
        val runInBackground by themePreferences.runInBackground.collectAsState(initial = false)

        // Stage 15a: the window previously had no default/min size at all (bare OS default),
        // which is what let the old ScrollableTabRow shell get squeezed into the label-wrapping
        // widths the last layout round had to patch around. The sidebar shell replacing it needs
        // enough width for a ~220dp rail plus a genuinely usable content pane, so give the window
        // real sizing intent instead of leaving it to chance.
        Window(
            onCloseRequest = {
                // Opt-in only (Settings → Layout → "Run in background") — closing the window
                // keeps quitting the app exactly as it always has unless the user turned this
                // on. DesktopAlarmClockService/SyncServer were already started above, outside
                // application{}, so they keep running untouched either way; this only decides
                // whether exitApplication() also tears the whole JVM process down with them.
                // Deferred via invokeLater rather than touched synchronously from this callback
                // — see mainWindowRef's doc comment above: doing it synchronously races Skiko's
                // frame-limiter coroutine querying the same X11 drawing surface, confirmed live.
                // Running it on a later EDT tick, off the native WM_DELETE_WINDOW callback's own
                // call stack, is the fix actually being tested here.
                if (runInBackground) {
                    val w = mainWindowRef.value
                    java.awt.EventQueue.invokeLater { w?.extendedState = java.awt.Frame.ICONIFIED }
                } else {
                    exitApplication()
                }
            },
            title = "Rhythm",
            state = rememberWindowState(width = 1280.dp, height = 800.dp)
        ) {
            window.minimumSize = java.awt.Dimension(960, 600)
            mainWindowRef.value = window

            RhythmThemedRoot(themePreferences = themePreferences) {
                DesktopAppRoot()
            }
        }

        DesktopAlertWindowHost()

        // Only meaningful alongside runInBackground's isVisible=false close path above — gives
        // the user a way to bring the hidden window back without relaunching the app, matching
        // every other Linux tray-minimize app (Slack, Discord, Signal, syncthing-gtk, etc.).
        // isTraySupported() is false on some Wayland/GNOME setups (java.awt.SystemTray isn't
        // universally implemented there); runInBackground still works without it since a second
        // launch's DesktopSingleInstance signal (wired to the same showMainWindow above) is the
        // fallback recovery path in that case.
        if (isTraySupported) {
            val trayState = rememberTrayState()
            Tray(
                icon = painterResource(Res.drawable.app_icon),
                state = trayState,
                tooltip = "Rhythm",
                onAction = showMainWindow,
                menu = {
                    Item("Show Rhythm", onClick = showMainWindow)
                    Separator()
                    Item("Quit", onClick = { exitApplication() })
                }
            )
        }
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
