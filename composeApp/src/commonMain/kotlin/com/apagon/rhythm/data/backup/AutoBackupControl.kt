package com.apagon.rhythm.data.backup

import kotlinx.coroutines.flow.StateFlow

/** How often the computer backs itself up. Same three choices as the phone. */
enum class AutoBackupFrequency(val label: String) { DAILY("Daily"), WEEKLY("Weekly"), OFF("Off") }

/**
 * Settings' handle on the computer's automatic backup (DesktopAutoBackup, desktopMain), so the
 * commonMain Settings screen never touches java.io.
 */
interface AutoBackupControl {
    val frequency: StateFlow<AutoBackupFrequency>
    fun setFrequency(frequency: AutoBackupFrequency)
    /** "bori_autobackup_2026-10-19.json, today 14:02", or null when none has been written yet. */
    val lastBackupLabel: StateFlow<String?>
    /** The folder, as shown to the person: "~/Downloads/Bori backup". */
    val folderLabel: String
    fun openFolder()
    /** Writes one now, whatever the schedule says. Null on success, else what went wrong. */
    suspend fun backUpNow(): String?
}
