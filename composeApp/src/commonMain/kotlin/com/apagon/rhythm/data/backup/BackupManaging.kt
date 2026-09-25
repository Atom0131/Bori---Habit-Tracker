package com.apagon.rhythm.data.backup

/**
 * Backup export/import, addressed by absolute file path rather than a
 * platform Uri/ContentResolver. commonMain-visible so DesktopSettingsViewModel
 * (Stage 11) can depend on it — same interface + platform-impl shape as
 * FilePicker/PhotoStorage, needed because commonMain can't see desktopMain
 * declarations directly (only the reverse). DesktopBackupManager (desktopMain)
 * is the only implementation today.
 */
interface BackupManaging {
    suspend fun exportToPath(path: String)
    suspend fun importFromPath(path: String)
}
