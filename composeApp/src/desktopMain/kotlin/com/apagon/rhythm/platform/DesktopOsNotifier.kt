package com.apagon.rhythm.platform

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
 */
class DesktopOsNotifier {
    fun notify(alert: FiredAlert) {
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
