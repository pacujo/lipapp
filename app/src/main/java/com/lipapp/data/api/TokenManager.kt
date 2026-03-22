package com.lipapp.data.api

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val prefs = EncryptedSharedPreferences.create(
        "auth_encrypted",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    init {
        val oldPrefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
        if (oldPrefs.contains("token")) {
            prefs.edit()
                .putString("token", oldPrefs.getString("token", null))
                .putString("base_url", oldPrefs.getString("base_url", ""))
                .putString("username", oldPrefs.getString("username", ""))
                .putString("password", oldPrefs.getString("password", ""))
                .apply()
            oldPrefs.edit().clear().apply()
        }
    }

    var token: String?
        get() = prefs.getString("token", null)
        set(value) { prefs.edit().putString("token", value).apply() }

    var baseUrl: String
        get() = prefs.getString("base_url", "") ?: ""
        set(value) { prefs.edit().putString("base_url", value).apply() }

    var username: String
        get() = prefs.getString("username", "") ?: ""
        set(value) { prefs.edit().putString("username", value).apply() }

    var password: String
        get() = prefs.getString("password", "") ?: ""
        set(value) { prefs.edit().putString("password", value).apply() }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
