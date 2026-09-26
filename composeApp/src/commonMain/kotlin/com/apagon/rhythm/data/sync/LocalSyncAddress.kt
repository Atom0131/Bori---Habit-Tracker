package com.apagon.rhythm.data.sync

/**
 * This device's own address for a peer to sync against, as a ready-to-show
 * string (e.g. "100.x.y.z:47890", or "Tailscale not detected (using
 * 127.0.0.1:47890)" as a fallback) — Stage 13. Desktop-only for now: nothing
 * else currently needs to display its own reachable address. commonMain so
 * DesktopHabitViewModel can hold one without reaching into desktopMain (which
 * commonMain can't see) — the real resolution logic lives in desktopMain's
 * findTailscaleAddress().
 */
data class LocalSyncAddress(
    /** Human-readable, e.g. "100.x.y.z:47890" or "Tailscale not detected (using 127.0.0.1:47890)". */
    val display: String,
    /** Always a plain "host:port", even on the loopback fallback — what actually gets encoded into the pairing QR code. */
    val addressForPairing: String
)
