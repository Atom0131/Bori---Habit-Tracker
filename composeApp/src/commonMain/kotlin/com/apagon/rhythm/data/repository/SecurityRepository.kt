package com.apagon.rhythm.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


enum class LockType { NONE, PIN, PASSWORD }
class SecurityRepository(
    private val dataStore: DataStore<Preferences>
) {
    private val PIN_KEY = stringPreferencesKey("journal_pin")
    private val PASSWORD_KEY = stringPreferencesKey("journal_password")
    private val LOCK_TYPE_KEY = stringPreferencesKey("lock_type")
    private val BIOMETRIC_ENABLED_KEY = booleanPreferencesKey("biometric_enabled")

    val lockType: Flow<LockType> = dataStore.data.map { prefs ->
        try {
            LockType.valueOf(prefs[LOCK_TYPE_KEY] ?: LockType.NONE.name)
        } catch (e: Exception) {
            LockType.NONE
        }
    }

    val journalPin: Flow<String?> = dataStore.data.map { prefs ->
        prefs[PIN_KEY]
    }

    val journalPassword: Flow<String?> = dataStore.data.map { prefs ->
        prefs[PASSWORD_KEY]
    }

    val biometricEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[BIOMETRIC_ENABLED_KEY] ?: false
    }

    suspend fun setJournalPin(pin: String?) {
        dataStore.edit { prefs ->
            if (pin == null) {
                prefs.remove(PIN_KEY)
                if (prefs[LOCK_TYPE_KEY] == LockType.PIN.name) {
                    prefs[LOCK_TYPE_KEY] = LockType.NONE.name
                    prefs.remove(BIOMETRIC_ENABLED_KEY)
                }
            } else {
                prefs[PIN_KEY] = pin
                prefs[LOCK_TYPE_KEY] = LockType.PIN.name
                prefs.remove(PASSWORD_KEY) // Mutually exclusive
            }
        }
    }

    suspend fun setJournalPassword(password: String?) {
        dataStore.edit { prefs ->
            if (password == null) {
                prefs.remove(PASSWORD_KEY)
                if (prefs[LOCK_TYPE_KEY] == LockType.PASSWORD.name) {
                    prefs[LOCK_TYPE_KEY] = LockType.NONE.name
                    prefs.remove(BIOMETRIC_ENABLED_KEY)
                }
            } else {
                prefs[PASSWORD_KEY] = password
                prefs[LOCK_TYPE_KEY] = LockType.PASSWORD.name
                prefs.remove(PIN_KEY) // Mutually exclusive
            }
        }
    }

    suspend fun clearAllLocks() {
        dataStore.edit { prefs ->
            prefs.remove(PIN_KEY)
            prefs.remove(PASSWORD_KEY)
            prefs[LOCK_TYPE_KEY] = LockType.NONE.name
            prefs.remove(BIOMETRIC_ENABLED_KEY)
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[BIOMETRIC_ENABLED_KEY] = enabled
        }
    }
}
