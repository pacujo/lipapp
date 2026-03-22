package com.lipapp.data.api

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)

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
