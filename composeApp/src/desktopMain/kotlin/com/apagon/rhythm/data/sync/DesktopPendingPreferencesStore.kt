package com.apagon.rhythm.data.sync

import com.apagon.rhythm.platform.AppHome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** PreferencesJoin's waiting settings, as a file beside sync_prefs.json. */
class DesktopPendingPreferencesStore : PendingPreferencesStore {
    private val file = File(AppHome.dir, "pending_sync_prefs.json")

    override suspend fun read(): String? = withContext(Dispatchers.IO) {
        if (file.exists()) runCatching { file.readText() }.getOrNull() else null
    }

    override suspend fun write(text: String?) = withContext(Dispatchers.IO) {
        if (text == null) { file.delete(); Unit } else file.writeText(text)
    }
}
