package com.apagon.rhythm.platform

class DesktopFilePickerService : FilePicker {
    override fun pickImagePath(): String? =
        pickFileToOpen("Choose Photo", listOf("jpg", "jpeg", "png", "webp"))
}
