package com.apagon.rhythm.platform

/**
 * Native "pick a file" prompt. DesktopEntrySheet needs this (photo
 * attachments), and calling desktopMain's pickFileToOpen() directly from a
 * commonMain file doesn't compile — commonMain can't see desktopMain
 * declarations, only the reverse — so this is a DI interface instead, same
 * shape as ImageBitmapLoader/PhotoStorage.
 */
interface FilePicker {
    fun pickImagePath(): String?

    /** Native "save file" prompt for backup export. Returns the chosen absolute path, or null if cancelled. */
    fun pickBackupExportPath(suggestedName: String): String?

    /** Native "open file" prompt for backup import. Returns the chosen absolute path, or null if cancelled. */
    fun pickBackupImportPath(): String?
}
