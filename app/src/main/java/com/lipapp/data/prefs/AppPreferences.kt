package com.lipapp.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private val KEY_URL = stringPreferencesKey("url")
        private val KEY_USERNAME = stringPreferencesKey("username")
        private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
    }

    val url: Flow<String> =
        context.lipPreferences.data.map { it[KEY_URL] ?: "http://10.0.2.2:8080/api/" }

    val username: Flow<String> =
        context.lipPreferences.data.map { it[KEY_USERNAME] ?: "admin" }

    val darkMode: Flow<Boolean> =
        context.lipPreferences.data.map { it[KEY_DARK_MODE] ?: false }

    suspend fun saveLoginInfo(url: String, username: String) {
        context.lipPreferences.edit {
            it[KEY_URL] = url
            it[KEY_USERNAME] = username
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.lipPreferences.edit {
            it[KEY_DARK_MODE] = enabled
        }
    }
}
