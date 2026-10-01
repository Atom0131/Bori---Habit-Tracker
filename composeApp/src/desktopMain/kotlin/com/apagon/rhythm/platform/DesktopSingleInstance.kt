package com.apagon.rhythm.platform

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileLock

/**
 * Lets a hidden (run-in-background) instance of Rhythm be "reopened" without a system tray icon
 * to click — `java.awt.SystemTray`/`TrayIcon` is unreliable on modern Linux (disabled outside
 * GNOME 45+ by recent JDKs, entirely unsupported under Wayland), so there's nothing reliable to
 * put a "show window" menu item on.
 *
 * Instead: an OS-level exclusive file lock on `~/.rhythm/instance.lock` (same `rhythm.home`
 * convention `DesktopHabitDatabase.kt`/`DesktopThemeDataStore.kt` already use) identifies the
 * primary instance. A second launch that can't acquire it just touches a marker file and exits
 * immediately — no second JVM, no second DB connection, no second poller racing the first. The
 * primary instance's [startWatching] notices the marker on its next poll (same cadence style
 * `DesktopAlarmClockService`'s own tick loop already uses) and raises its window.
 */
class DesktopSingleInstance {
    private val dir = (System.getProperty("rhythm.home")?.let { File(it) }
        ?: File(System.getProperty("user.home"), ".rhythm")).apply { mkdirs() }
    private val lockFile = File(dir, "instance.lock")
    private val showSignalFile = File(dir, "show-window")

    private var lock: FileLock? = null

    /** Call once, as early as possible in `main()` — before any DB/Koin setup. Returns true if
     * this process is the primary instance and should continue starting up normally; false means
     * a primary instance is already running (and has been signaled to show its window), so the
     * caller should exit immediately without doing any further work. */
    fun acquire(): Boolean {
        val acquired = runCatching {
            val channel = RandomAccessFile(lockFile, "rw").channel
            lock = channel.tryLock()
            lock != null
        }.getOrDefault(false)
        if (!acquired) {
            runCatching { showSignalFile.createNewFile() }
        }
        return acquired
    }

    /** Only meaningful on the primary instance. Polls for the marker file a second launch leaves
     * behind and calls [onShowRequested] (expected to make the main window visible again) when
     * found. */
    fun startWatching(scope: CoroutineScope, onShowRequested: () -> Unit) {
        scope.launch {
            while (true) {
                if (showSignalFile.exists()) {
                    runCatching { showSignalFile.delete() }
                    onShowRequested()
                }
                delay(2_000L)
            }
        }
    }
}
