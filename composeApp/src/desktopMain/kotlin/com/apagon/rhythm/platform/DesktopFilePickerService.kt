package com.apagon.rhythm.platform

class DesktopFilePickerService : FilePicker {
    override fun pickImagePath(): String? =
        pickFileToOpen("Choose Photo", listOf("jpg", "jpeg", "png", "webp"))

    override fun pickBackupExportPath(suggestedName: String): String? =
        pickFileToSave("Export Backup", suggestedName)

    override fun pickBackupImportPath(): String? =
        pickFileToOpen("Import Backup", listOf("json"))
}
