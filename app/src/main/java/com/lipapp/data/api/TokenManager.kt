package com.lipapp.data.api

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor() {
    @Volatile var token: String? = null
    @Volatile var baseUrl: String = ""
}
