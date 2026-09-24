package com.apagon.rhythm.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import java.io.File
import okio.Path.Companion.toPath

// Real DataStore<Preferences> for desktop (unlike DesktopSyncPreferences.kt's
// hand-rolled JSON file, ThemePreferences.kt is a substantial, already-built,
// reactive-Flow wrapper — rebuilding it would duplicate working logic and
// lose Compose's collectAsState() reactivity). Same ~/.rhythm/ convention as
// DesktopHabitDatabase.kt and DesktopSyncPreferences.kt.
fun buildDesktopThemeDataStore(): DataStore<Preferences> {
    val dir = System.getProperty("rhythm.home")?.let { File(it) }
        ?: File(System.getProperty("user.home"), ".rhythm")
    dir.mkdirs()
    val file = File(dir, "settings.preferences_pb")
    return PreferenceDataStoreFactory.createWithPath(
        produceFile = { file.absolutePath.toPath() }
    )
}
