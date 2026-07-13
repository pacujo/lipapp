package com.lipapp.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

internal val Context.lipPreferences: DataStore<Preferences> by preferencesDataStore(
    name = "lip_prefs",
)
