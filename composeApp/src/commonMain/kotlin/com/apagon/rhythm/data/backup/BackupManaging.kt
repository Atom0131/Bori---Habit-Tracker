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
    /** @param password when non-null, the backup is written as an encrypted envelope
     *  (see `DesktopBackupCrypto`). Null keeps the plain, readable JSON format. */
    suspend fun exportToPath(path: String, password: CharArray? = null)

    /**
     * @param password required when the file on disk is an encrypted envelope; ignored otherwise.
     * @return null on a clean restore, or a short warning/error message when part of it did not
     *   apply — including "wrong password" / "password required" for an encrypted file, matching
     *   Android's own pattern of surfacing partial-restore failures rather than swallowing them.
     */
    suspend fun importFromPath(path: String, password: CharArray? = null): String?
}
