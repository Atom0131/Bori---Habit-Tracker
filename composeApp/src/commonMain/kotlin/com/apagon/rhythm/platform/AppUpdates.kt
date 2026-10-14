package com.apagon.rhythm.platform

import kotlinx.coroutines.flow.StateFlow

/**
 * "Bori 1.0.6 is ready — Update now / Later". Checks GitHub Releases at most once a week, always
 * asks before installing, and cleans up the downloaded installer afterwards. Desktop only: the
 * phone updates through the Play Store. Implemented by `DesktopAppUpdater`.
 */
interface AppUpdates {
    val state: StateFlow<UpdateState>

    /** Epoch millis of the last finished check, or 0 if Bori has never checked. */
    val lastCheckedAt: StateFlow<Long>

    /** Settings → About's "Check now": checks straight away instead of waiting for the week. */
    fun checkNow()

    /** Downloads, verifies and installs [UpdateState.Available], then restarts Bori. */
    fun install()

    /** Hides the banner (until next week's check, or clears a failed install's message). */
    fun later()
}

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState

    /** This copy wasn't installed from a Bori package (a dev build), so it can't update itself. */
    data object NotInstalled : UpdateState
    data class Available(val version: String, val notes: String, val dismissed: Boolean = false) : UpdateState
    data class Downloading(val version: String, val progress: Float) : UpdateState
    data class Installing(val version: String) : UpdateState
    /** [inBanner]: an install that failed shows in the sidebar; a failed check only in About. */
    data class Failed(val message: String, val inBanner: Boolean = false) : UpdateState
}
