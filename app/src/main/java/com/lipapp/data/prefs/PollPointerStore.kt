package com.lipapp.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PollPointerStore @Inject constructor(
    @ApplicationContext context: Context,
    private val json: Json,
) {
    private val dataStore = context.lipPreferences

    companion object {
        private val KEY_POINTERS = stringPreferencesKey("poll_pointers")
        private val KEY_BOOTSTRAPPED = booleanPreferencesKey("poll_bootstrapped")
    }

    suspend fun getPointers(): Map<String, String> {
        val raw = dataStore.data.first()[KEY_POINTERS] ?: return emptyMap()
        return try {
            json.decodeFromString<Map<String, String>>(raw)
        } catch (_: Exception) {
            emptyMap()
        }
    }

    suspend fun isBootstrapped(): Boolean =
        dataStore.data.first()[KEY_BOOTSTRAPPED] == true

    suspend fun bootstrap(pointers: Map<String, String>) {
        dataStore.edit { prefs ->
            if (prefs[KEY_BOOTSTRAPPED] != true) {
                prefs[KEY_POINTERS] = json.encodeToString(pointers)
                prefs[KEY_BOOTSTRAPPED] = true
            }
        }
    }

    suspend fun setPointer(key: String, messageId: String) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_POINTERS]?.let {
                try {
                    json.decodeFromString<Map<String, String>>(it).toMutableMap()
                } catch (_: Exception) {
                    mutableMapOf()
                }
            } ?: mutableMapOf()
            current[key] = messageId
            prefs[KEY_POINTERS] = json.encodeToString(current)
        }
    }

    suspend fun updatePointers(updates: Map<String, String>) {
        if (updates.isEmpty()) return
        dataStore.edit { prefs ->
            val current = prefs[KEY_POINTERS]?.let {
                try {
                    json.decodeFromString<Map<String, String>>(it).toMutableMap()
                } catch (_: Exception) {
                    mutableMapOf()
                }
            } ?: mutableMapOf()
            current.putAll(updates)
            prefs[KEY_POINTERS] = json.encodeToString(current)
        }
    }

    suspend fun clear() {
        dataStore.edit {
            it.remove(KEY_POINTERS)
            it.remove(KEY_BOOTSTRAPPED)
        }
    }
}
