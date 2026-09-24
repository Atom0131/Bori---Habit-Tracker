package com.apagon.rhythm.data.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first

// Android already has real, working DataStore plumbing (AppModule.kt's
// settingsDataStore/securityDataStore precedent) — a dedicated "sync_prefs"
// DataStore is wired the same way, unlike desktop, which has none today.
class AndroidSyncPreferences(
    private val dataStore: DataStore<Preferences>
) : SyncPreferences {
    private val lastSyncedAtKey = longPreferencesKey("lastSyncedAt")
    private val peerAddressKey = stringPreferencesKey("peerAddress")

    override suspend fun getLastSyncedAt(): Long =
        dataStore.data.first()[lastSyncedAtKey] ?: 0L

    override suspend fun setLastSyncedAt(timestamp: Long) {
        dataStore.edit { it[lastSyncedAtKey] = timestamp }
    }

    override suspend fun getPeerAddress(): String? =
        dataStore.data.first()[peerAddressKey]

    override suspend fun setPeerAddress(address: String?) {
        dataStore.edit {
            if (address == null) it.remove(peerAddressKey) else it[peerAddressKey] = address
        }
    }
}
