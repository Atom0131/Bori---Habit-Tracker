package com.apagon.rhythm.platform

import java.awt.FileDialog
import java.awt.Frame
import java.io.FilenameFilter

// Reusable native file-picker helpers for desktop, built in Stage 6 so the
// screens that need one later (Journal/Notes image attachments, Settings
// backup import/export) don't each build their own. Plain java.awt.FileDialog
// rather than Swing's JFileChooser — it's the native GTK/Motif dialog on
// Linux, matching what a desktop user actually expects, and Compose Desktop
// already hosts its event loop on the AWT thread (see kotlinx-coroutines-swing
// in build.gradle.kts), so blocking on it here is safe.

/** Opens a native "Open File" dialog. Returns the chosen absolute path, or null if cancelled. */
fun pickFileToOpen(title: String, extensions: List<String>? = null): String? {
    val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
    if (extensions != null) {
        dialog.filenameFilter = FilenameFilter { _, name ->
            extensions.any { name.endsWith(".$it", ignoreCase = true) }
        }
    }
    dialog.isVisible = true
    val file = dialog.file ?: return null
    return dialog.directory + file
}

/** Opens a native "Save File" dialog. Returns the chosen absolute path, or null if cancelled. */
fun pickFileToSave(title: String, suggestedName: String? = null): String? {
    val dialog = FileDialog(null as Frame?, title, FileDialog.SAVE)
    if (suggestedName != null) dialog.file = suggestedName
    dialog.isVisible = true
    val file = dialog.file ?: return null
    return dialog.directory + file
}
