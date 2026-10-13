package com.apagon.rhythm.platform

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Posts a real OS desktop notification (the same `org.freedesktop.Notifications` D-Bus
 * service every browser/clock app on Linux uses) alongside the always-on-top alert
 * window `DesktopAlertWindowHost` already shows — so a fired alarm/reminder/timer is
 * visible even when the Rhythm window is minimized, behind other windows, or on another
 * virtual desktop. Shells out to `notify-send` rather than adding a D-Bus client
 * dependency: it's present by default on every mainstream desktop distro (part of
 * libnotify) and needs zero Gradle changes, matching the precedent `main.kt`'s
 * `ensureEmojiFontInstalled()` already set for best-effort OS integration via a plain
 * `ProcessBuilder` call.
 *
 * Fire-and-forget, not blocking: this runs on every fire from inside
 * `DesktopAlarmClockService`'s 10-second poll loop, unlike the font installer's one-time
 * startup call, so it must never stall the next tick. If `notify-send` isn't installed
 * (rare on a minimal/server-ish Linux install), this silently does nothing — the
 * always-on-top window remains the authoritative, always-present fallback.
 *
 * Off Linux (Windows) there's no `notify-send`. The alert goes out on [trayAlerts] instead, and
 * main.kt's AWT `Tray` turns it into a native toast with `TrayState.sendNotification`.
 */
class DesktopOsNotifier {
    private val isLinux = System.getProperty("os.name").orEmpty().lowercase().contains("linux")
    private val _trayAlerts = MutableSharedFlow<FiredAlert>(extraBufferCapacity = 8)
    val trayAlerts: SharedFlow<FiredAlert> = _trayAlerts.asSharedFlow()

    fun notify(alert: FiredAlert) {
        if (!isLinux) {
            _trayAlerts.tryEmit(alert)
            return
        }
        runCatching {
            val urgency = if (alert.kind == FiredAlertKind.REMINDER) "normal" else "critical"
            ProcessBuilder(
                "notify-send",
                "--app-name=Bori",
                "--urgency=$urgency",
                alert.title,
                alert.subtitle
            ).start()
        }
    }
}
